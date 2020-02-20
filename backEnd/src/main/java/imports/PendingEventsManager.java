package imports;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import utilities.ErrorDescriptor;
import utilities.IOStreamsHelper;
import utilities.JsonEncoders;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class PendingEventsManager extends DatabaseAccessManager {

  public static final String SCANNER_ID = "ScannerId";

  public static final String DELIM = ";";
  private static final String NUMBER_OF_PARTITIONS_ENV_KEY = "NUMBER_OF_PARTITIONS";

  private static final String STEP_FUNCTION_ARN = "arn:aws:states:us-east-2:871532548613:stateMachine:EventResolver";

  private final AWSStepFunctions client;

  public PendingEventsManager() {
    super("pending_events", SCANNER_ID, Regions.US_EAST_2);
    this.client = AWSStepFunctionsClientBuilder.standard()
        .withCredentials(new EnvironmentVariableCredentialsProvider())
        .withRegion(Regions.US_EAST_2)
        .build();
  }

  public PendingEventsManager(final DynamoDB dynamoDB, final AWSStepFunctions awsStepFunctions) {
    super("pending_events", SCANNER_ID, Regions.US_EAST_2, dynamoDB);
    this.client = awsStepFunctions;
  }

  private boolean startStepMachineExecution(String groupId, String eventId, String scannerId,
      Metrics metrics, LambdaLogger lambdaLogger) {
    final String classMethod = "PendingEventsManager.startStepMachineExecution";
    metrics.commonSetup(classMethod);

    boolean success = true;

    Map<String, Object> input = ImmutableMap
        .of(GroupsManager.GROUP_ID, groupId, RequestFields.EVENT_ID, eventId, SCANNER_ID,
            scannerId);

    try {
      StringBuilder escapedInput = new StringBuilder(JsonEncoders.convertMapToJson(input));
      IOStreamsHelper
          .removeAllInstancesOf(escapedInput,
              '\\'); // step function doesn't like these in the input

      this.client.startExecution(new StartExecutionRequest()
          .withStateMachineArn(STEP_FUNCTION_ARN)
          .withInput(escapedInput.toString()));
    } catch (Exception e) {
      success = false;
      lambdaLogger
          .log(new ErrorDescriptor<>(input, classMethod, metrics.getRequestId(), e).toString());
    }

    metrics.commonClose(success);
    return success;
  }

  public ResultStatus processPendingEvent(final Map<String, Object> jsonMap, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    final String classMethod = "PendingEventsManager.processPendingEvent";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(GroupsManager.GROUP_ID, RequestFields.EVENT_ID, SCANNER_ID);

    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GroupsManager.GROUP_ID);
        final String eventId = (String) jsonMap.get(RequestFields.EVENT_ID);
        final String scannerId = (String) jsonMap.get(SCANNER_ID);

        final Item groupData = DatabaseManagers.GROUPS_MANAGER.getItemByPrimaryKey(groupId);
        if (groupData != null) { // if null, assume the group was deleted
          //First thing to do is get the category data from the db.
          final Map<String, Object> groupDataMapped = groupData.asMap();
          Map<String, Object> groupEventDataMapped = (Map<String, Object>) groupDataMapped
              .get(GroupsManager.EVENTS);
          Map<String, Object> eventDataMapped = (Map<String, Object>) groupEventDataMapped
              .get(eventId);

          Map<String, Object> currentTentativeChoices = (Map<String, Object>) eventDataMapped
              .get(GroupsManager.TENTATIVE_CHOICES);

          if (currentTentativeChoices.isEmpty()) {
            //we need to:
            //  run the algorithm
            //  update the event object
            //  update the pending events table with the new data time for the end of voting
            Integer votingDuration = (Integer) eventDataMapped.get(GroupsManager.VOTING_DURATION);

            final Map<String, Object> tentativeChoices = this
                .getTentativeAlgorithmChoices(eventDataMapped, metrics, lambdaLogger);

            //update the event
            String updateExpression =
                "set " + GroupsManager.EVENTS + ".#eventId." + GroupsManager.TENTATIVE_CHOICES
                    + " = :tentativeChoices, " + GroupsManager.LAST_ACTIVITY + " = :currentDate";
            NameMap nameMap = new NameMap().with("#eventId", eventId);
            ValueMap valueMap = new ValueMap().withMap(":tentativeChoices", tentativeChoices)
                .withString(":currentDate",
                    LocalDateTime.now(ZoneId.of("UTC")).format(this.getDateTimeFormatter()));

            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(GroupsManager.GROUP_ID, groupId)
                .withUpdateExpression(updateExpression)
                .withNameMap(nameMap)
                .withValueMap(valueMap);

            DatabaseManagers.GROUPS_MANAGER.updateItem(updateItemSpec);

            //this overwrites the old mapping
            ResultStatus updatePendingEvent = this.addPendingEvent(groupId, eventId, votingDuration);
            if (updatePendingEvent.success) {
              resultStatus = new ResultStatus(true, "Event updated successfully");
            } else {
              resultStatus.resultMessage = "Error updating pending event mapping with voting duration";
            }
          } else {
            //we need to loop over the voting results and figure out the yes percentage of votes for the choices
            //set the selected choice as the one with the highest percent
            String result = this.getSelectedChoice(eventDataMapped, metrics, lambdaLogger);

            //update the event
            String updateExpression =
                "set " + GroupsManager.EVENTS + ".#eventId." + GroupsManager.SELECTED_CHOICE
                    + " = :selectedChoice, " + GroupsManager.LAST_ACTIVITY + " = :currentDate";
            NameMap nameMap = new NameMap().with("#eventId", eventId);
            ValueMap valueMap = new ValueMap().withString(":selectedChoice", result)
                .withString(":currentDate",
                    LocalDateTime.now(ZoneId.of("UTC")).format(this.getDateTimeFormatter()));

            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(GroupsManager.GROUP_ID, groupId)
                .withUpdateExpression(updateExpression)
                .withNameMap(nameMap)
                .withValueMap(valueMap);

            DatabaseManagers.GROUPS_MANAGER.updateItem(updateItemSpec);

            // now remove the entry from the pending events table since it has been fully processed now
            updateExpression = "remove #groupEventKey";
            nameMap = new NameMap().with("#groupEventKey", groupId + DELIM + eventId);

            updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(this.getPrimaryKeyIndex(), scannerId)
                .withUpdateExpression(updateExpression)
                .withNameMap(nameMap);

            this.updateItem(updateItemSpec);

            resultStatus = new ResultStatus(true, "Pending event finalized successfully");
          }
        } else {
          resultStatus.resultMessage = "Error: Group data not found.";
          lambdaLogger.log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
              "Group data not found.").toString());
        }
      } catch (Exception e) {
        resultStatus.resultMessage = "Error: Unable to parse request in manager.";
        lambdaLogger.log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
            "Unable to parse request in manager.").toString());
      }
    } else {
      resultStatus.resultMessage = "Error: Required request keys not found.";
      lambdaLogger.log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
          "Required request keys not found.").toString());
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public Map<String, Object> getTentativeAlgorithmChoices(final Map<String, Object> eventDataMapped,
      final Metrics metrics, final LambdaLogger lambdaLogger) {
    Map<String, Object> tentativeChoice;

    try {
      String categoryId = (String) eventDataMapped.get(GroupsManager.CATEGORY_ID);
      Item categoryData = DatabaseManagers.CATEGORIES_MANAGER.getItemByPrimaryKey(categoryId);

      Map<String, Object> categoryDataMapped = categoryData.asMap();

      //with the category, we can now get the first choice in the category which is our result
      tentativeChoice = (Map<String, Object>) categoryDataMapped.get(CategoriesManager.CHOICES);

      //limit to only three
      while (tentativeChoice.size() > 3) {
        tentativeChoice.remove(tentativeChoice.keySet().toArray(new String[0])[0]);
      }
    } catch (Exception e) {
      // we have an event pointing to a non existent category
      tentativeChoice = ImmutableMap.of("1", "Error");
    }

    return tentativeChoice;
  }

  public String getSelectedChoice(final Map<String, Object> eventDataMapped, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    String selectedChoice;

    try {
      Map<String, Object> tentativeChoice = (Map<String, Object>) eventDataMapped
          .get(GroupsManager.TENTATIVE_CHOICES);
      selectedChoice = (String) tentativeChoice.values().toArray()[0];
    } catch (Exception e) {
      selectedChoice = "Error";
    }

    return selectedChoice;
  }

  public ResultStatus addPendingEvent(final String groupId, final String eventId,
      final Integer pollDuration) {
    ResultStatus resultStatus = new ResultStatus();

    try {
      final String partitionKey = this.getPartitionKey();
      final LocalDateTime expirationDate = LocalDateTime.now(ZoneId.of("UTC"))
          .plus(pollDuration, ChronoUnit.MINUTES);

      final String updateExpression = "set #key = :date";
      final ValueMap valueMap = new ValueMap()
          .withString(":date",
              expirationDate.format(this.getDateTimeFormatter()));
      final NameMap nameMap = new NameMap()
          .with("#key", groupId + DELIM + eventId);

      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withPrimaryKey(this.getPrimaryKeyIndex(), partitionKey)
          .withUpdateExpression(updateExpression)
          .withNameMap(nameMap)
          .withValueMap(valueMap); // only modifying my row

      this.updateItem(updateItemSpec);

      resultStatus = new ResultStatus(true, "Pending event inserted successfully.");
    } catch (Exception e) {
      resultStatus.resultMessage = "Error adding pending event.";
    }

    return resultStatus;
  }

  public void scanPendingEvents(String scannerId, Metrics metrics, LambdaLogger lambdaLogger) {
    final String classMethod = "PendingEventsManager.scanPendingEvents";
    metrics.commonSetup(classMethod);

    boolean success = true; // if anything fails we'll set this to false so "everything" fails -> we investigate

    try {
      Item pendingEvents = this.getItemByPrimaryKey(scannerId);

      Map<String, Object> pendingEventsData = pendingEvents.asMap();
      LocalDateTime resolutionDate, currentDate = LocalDateTime.now(ZoneId.of("UTC"));

      String groupId, eventId;
      List<String> keyPair;
      for (String key : pendingEventsData.keySet()) {
        if (!key.equals(SCANNER_ID)) {
          try {
            resolutionDate = LocalDateTime
                .parse((String) pendingEventsData.get(key), this.getDateTimeFormatter());

            if (currentDate.isAfter(resolutionDate)) {
              keyPair = Arrays.asList(key.split(DELIM));

              if (keyPair.size() == 2) {
                groupId = keyPair.get(0);
                eventId = keyPair.get(1);

                success = (success && this
                    .startStepMachineExecution(groupId, eventId, scannerId, metrics, lambdaLogger));
              } else {
                lambdaLogger.log(
                    new ErrorDescriptor<>("scanner id: " + scannerId + ", key : " + key,
                        classMethod, metrics.getRequestId(),
                        "bad format for key in pending events table").toString());
                success = false;
              }
            }
          } catch (Exception e) {
            lambdaLogger.log(
                new ErrorDescriptor<>("scanner id: " + scannerId + ", key : " + key, classMethod,
                    metrics.getRequestId(), e).toString());
            success = false;
          }
        }
      }
    } catch (Exception e) {
      lambdaLogger.log(
          new ErrorDescriptor<>("scanner id: " + scannerId, classMethod, metrics.getRequestId(), e)
              .toString());
      success = false;
    }

    metrics.commonClose(success);
  }

  private String getPartitionKey() throws NullPointerException, NumberFormatException {
    //this gives a 'randomized' key based on the system's clock time.
    return Long.toString(
        (System.currentTimeMillis() % Integer.parseInt(System.getenv(NUMBER_OF_PARTITIONS_ENV_KEY)))
            + 1);
  }
}
