package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.util.ImmutableMapParameter;
import utilities.ExceptionHelper;
import utilities.JsonEncoders;
import utilities.RequestFields;
import utilities.ResultStatus;

public class PendingEventsManager extends DatabaseAccessManager {

  private static final Integer NUMBER_OF_PARTITIONS = 1;
  private static final Integer MINUTE = 60 * 1000; // milliseconds
  private static final PendingEventsManager PENDING_EVENTS_MANAGER = new PendingEventsManager();

  private static final String STEP_FUNCTION_ARN = "aws:states:us-east-2:871532548613:stateMachine:EventResolver";

  private final AWSStepFunctions client = AWSStepFunctionsClientBuilder.defaultClient();

  public PendingEventsManager() {
    super("pending_events", "ScannerId", Regions.US_EAST_2);
  }

  private void startStepMachineExecution(String groupId, String eventId) {
    this.client.startExecution(new StartExecutionRequest()
            .withStateMachineArn(STEP_FUNCTION_ARN)
            .withInput(JsonEncoders.convertObjectToJson(ImmutableMapParameter.of(GroupsManager.GROUP_ID, groupId, RequestFields.EVENT_ID, eventId))));
  }

  public static ResultStatus addPendingEvent(final String groupId, final String eventId,
      final Date creationDate,
      final Integer pollDuration) {
    ResultStatus resultStatus = new ResultStatus();

    try {
      final String partitionKey = PENDING_EVENTS_MANAGER.getPartitionKey(creationDate);
      final Date expirationDate = new Date(creationDate.getTime() + pollDuration * MINUTE);

      final String attributeValue = groupId + ";" + eventId;

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
      resultStatus.resultMessage = "Error adding pending event. Exception: " + e + "\n" + ExceptionHelper.getStackTrace(e);
    }

    return resultStatus;
  }

  public static void scanPendingEvents(String scannerId) {
    try {
      Item pendingEvents = PENDING_EVENTS_MANAGER.getItemByPrimaryKey(scannerId);

      if (pendingEvents != null) {
        Map<String, Object> pendingEventsData = pendingEvents.asMap();
        Date currentDate = new Date(), resolutionDate;

        String groupId, eventId;
        List<String> keyPair;
        for (String key : pendingEventsData.keySet()) {
          resolutionDate = PENDING_EVENTS_MANAGER.getDbDateFormatter().parse((String) pendingEventsData.get(key));

          if (currentDate.after(resolutionDate)) {
            keyPair = Arrays.asList(key.split(";"));

            if (keyPair.size() == 2) {
              groupId = keyPair.get(0);
              eventId = keyPair.get(1);

              PENDING_EVENTS_MANAGER.startStepMachineExecution(groupId, eventId);
            } else {
              //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
              //we inserted this wrong, fix this
            }
          }
        }
      }
    } catch (Exception e) {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
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
