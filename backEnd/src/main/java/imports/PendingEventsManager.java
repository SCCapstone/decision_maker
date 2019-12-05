package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.util.ImmutableMapParameter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import utilities.IOStreamsHelper;
import utilities.JsonEncoders;
import utilities.RequestFields;
import utilities.ResultStatus;

public class PendingEventsManager extends DatabaseAccessManager {

  public static final String SCANNER_ID = "ScannerId";

  public static final String DELIM = ";";
  private static final Integer NUMBER_OF_PARTITIONS = 1;
  private static final Integer MINUTE = 60 * 1000; // milliseconds
  private static final PendingEventsManager PENDING_EVENTS_MANAGER = new PendingEventsManager();

  private static final String STEP_FUNCTION_ARN = "arn:aws:states:us-east-2:871532548613:stateMachine:EventResolver";

  private final AWSStepFunctions client = AWSStepFunctionsClientBuilder.defaultClient();

  public PendingEventsManager() {
    super("pending_events", SCANNER_ID, Regions.US_EAST_2);
  }

  private void startStepMachineExecution(String groupId, String eventId, String scannerId) {
    Map<String, Object> input = ImmutableMapParameter
        .of(GroupsManager.GROUP_ID, groupId, RequestFields.EVENT_ID, eventId, SCANNER_ID,
            scannerId);

    StringBuilder escapedInput = new StringBuilder(JsonEncoders.convertMapToJson(input));
    IOStreamsHelper
        .removeAllInstancesOf(escapedInput, '\\'); // step function doesn't like these in the input

    this.client.startExecution(new StartExecutionRequest()
        .withStateMachineArn(STEP_FUNCTION_ARN)
        .withInput(escapedInput.toString()));
  }

  public static ResultStatus processPendingEvent(final Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(GroupsManager.GROUP_ID, RequestFields.EVENT_ID, SCANNER_ID);

    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GroupsManager.GROUP_ID);
        final String eventId = (String) jsonMap.get(RequestFields.EVENT_ID);
        final String scannerId = (String) jsonMap.get(SCANNER_ID);

        final Item groupData = GroupsManager.GROUPS_MANAGER.getItemByPrimaryKey(groupId);
        if (groupData != null) { // if null, assume the group was deleted
          //Dumb algorithm simply gets first choice from category!

          //First thing to do is get the category data from the db.
          final Map<String, Object> groupDataMapped = groupData.asMap();
          Map<String, Object> groupEventDataMapped = (Map<String, Object>) groupDataMapped
              .get(GroupsManager.EVENTS);
          Map<String, Object> eventDataMapped = (Map<String, Object>) groupEventDataMapped
              .get(eventId);

          String categoryId = (String) eventDataMapped.get(GroupsManager.CATEGORY_ID);
          Item categoryData = CategoriesManager.CATEGORIES_MANAGER.getItemByPrimaryKey(categoryId);
          Map<String, Object> categoryDataMapped = categoryData.asMap();

          //with the category, we can now get the first choice in the category which is our result
          Map<String, Object> choices = (Map<String, Object>) categoryDataMapped
              .get(CategoriesManager.CHOICES);
          String result = (String) choices.values().toArray()[0];

          //update the event
          String updateExpression =
              "set " + GroupsManager.EVENTS + ".#eventId." + GroupsManager.SELECTED_CHOICE
                  + " = :result";
          NameMap nameMap = new NameMap().with("#eventId", eventId);
          ValueMap valueMap = new ValueMap().withString(":result", result);

          UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withPrimaryKey(GroupsManager.GROUP_ID, groupId)
              .withUpdateExpression(updateExpression)
              .withNameMap(nameMap)
              .withValueMap(valueMap);

          GroupsManager.GROUPS_MANAGER.updateItem(updateItemSpec);

          //remove the pending entry from the pending events table
          updateExpression = "remove #groupEventKey";
          nameMap = new NameMap().with("#groupEventKey", groupId + DELIM + eventId);

          updateItemSpec = new UpdateItemSpec()
              .withPrimaryKey(SCANNER_ID, scannerId)
              .withUpdateExpression(updateExpression)
              .withNameMap(nameMap);

          PENDING_EVENTS_MANAGER.updateItem(updateItemSpec);

          resultStatus = new ResultStatus(true, "Pending event processed successfully.");
        }
      } catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request in manager.";
      }
    } else {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    return resultStatus;
  }

  public static ResultStatus addPendingEvent(final String groupId, final String eventId,
      final Date creationDate,
      final Integer pollDuration) {
    ResultStatus resultStatus = new ResultStatus();

    try {
      final String partitionKey = PENDING_EVENTS_MANAGER.getPartitionKey(creationDate);
      final Date expirationDate = new Date(creationDate.getTime() + pollDuration * MINUTE);

      final String attributeValue = groupId + DELIM + eventId;

      final String updateExpression = "set #key = :date";
      final ValueMap valueMap = new ValueMap()
          .withString(":date", PENDING_EVENTS_MANAGER.getDbDateFormatter().format(expirationDate));
      final NameMap nameMap = new NameMap()
          .with("#key", attributeValue);

      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withPrimaryKey(PENDING_EVENTS_MANAGER.getPrimaryKeyIndex(), partitionKey)
          .withUpdateExpression(updateExpression)
          .withNameMap(nameMap)
          .withValueMap(valueMap); // only modifying my row

      PENDING_EVENTS_MANAGER.updateItem(updateItemSpec);

      resultStatus = new ResultStatus(true, "Pending event inserted successfully.");
    } catch (Exception e) {
      resultStatus.resultMessage = "Error adding pending event.";
    }

    return resultStatus;
  }

  public static void scanPendingEvents(String scannerId) {
    try {
      Item pendingEvents = PENDING_EVENTS_MANAGER.getItemByPrimaryKey(scannerId);

      if (pendingEvents != null) {
        Map<String, Object> pendingEventsData = pendingEvents.asMap();
        Date resolutionDate, currentDate = new Date();

        String groupId, eventId;
        List<String> keyPair;
        for (String key : pendingEventsData.keySet()) {
          if (!key.equals(SCANNER_ID)) {
            try {
              resolutionDate = PENDING_EVENTS_MANAGER.getDbDateFormatter()
                  .parse((String) pendingEventsData.get(key));

              if (currentDate.after(resolutionDate)) {
                keyPair = Arrays.asList(key.split(DELIM));

                if (keyPair.size() == 2) {
                  groupId = keyPair.get(0);
                  eventId = keyPair.get(1);

                  PENDING_EVENTS_MANAGER.startStepMachineExecution(groupId, eventId, scannerId);
                } else {
                  //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
                  //we inserted this wrong, fix this
                }
              }
            } catch (Exception e) {
              //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
              //probably and issue with parsing the date string
            }
          }
        }
      }
    } catch (Exception e) {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      //probably an issue with getting the item/getting data from it
    }
  }

  //This method is purely for testing purposes -> hardcoded strings
  public static ResultStatus addPendingEvent(final Map<String, Object> jsonMap) {
    return PendingEventsManager.addPendingEvent(
        (String) jsonMap.get("GroupId"),
        (String) jsonMap.get("EventId"),
        new Date(),
        (Integer) jsonMap.get("Duration")
    );
  }

  private String getPartitionKey(Date creationDate) {
    //this gives a randomized key based on the time of the input.
    return Long.toString(creationDate.getTime() % NUMBER_OF_PARTITIONS + 1);
  }
}
