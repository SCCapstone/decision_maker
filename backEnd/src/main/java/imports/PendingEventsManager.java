package imports;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import models.Category;
import models.Event;
import models.EventWithCategoryChoices;
import models.GroupWithCategoryChoices;
import models.User;
import utilities.DataCruncher;
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

  private static final Float K = 0.2f;

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

  private boolean startStepMachineExecution(final String groupId, final String eventId,
      final String scannerId, final Metrics metrics) {
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
      metrics.log(new ErrorDescriptor<>(input, classMethod, e));
    }

    metrics.commonClose(success);
    return success;
  }

  public ResultStatus processPendingEvent(final Map<String, Object> jsonMap,
      final Metrics metrics) {
    final String classMethod = "PendingEventsManager.processPendingEvent";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(GroupsManager.GROUP_ID, RequestFields.EVENT_ID, SCANNER_ID);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GroupsManager.GROUP_ID);
        final String eventId = (String) jsonMap.get(RequestFields.EVENT_ID);
        final String scannerId = (String) jsonMap.get(SCANNER_ID);
        final Boolean isNewEvent = (jsonMap.containsKey(RequestFields.NEW_EVENT)
            && (Boolean) jsonMap.get(RequestFields.NEW_EVENT));

        final Item groupData = DatabaseManagers.GROUPS_MANAGER.getItemByPrimaryKey(groupId);
        if (groupData != null) { // if null, assume the group was deleted
          final GroupWithCategoryChoices group = new GroupWithCategoryChoices(groupData.asMap());
          final EventWithCategoryChoices event = group.getEventsWithCategoryChoices().get(eventId);
          final Event updatedEvent = event.clone();

          if (event.getTentativeAlgorithmChoices().isEmpty()) {
            //we need to:
            //  run the algorithm
            //  update the event object
            //  update the pending events table with the new data time for the end of voting
            final Map<String, String> tentativeChoices;
            if (event.getVotingDuration() > 0) {
              tentativeChoices = this.getTentativeAlgorithmChoices(event, 3, metrics);
            } else {
              tentativeChoices = this.getTentativeAlgorithmChoices(event, 1, metrics);
              updatedEvent.setSelectedChoice(tentativeChoices.keySet().toArray()[0].toString());
            }

            updatedEvent.setTentativeAlgorithmChoices(tentativeChoices);
            DatabaseManagers.GROUPS_MANAGER
                .updateEvent(group, eventId, updatedEvent, isNewEvent, metrics);

            //this overwrites the old mapping
            ResultStatus updatePendingEvent = this
                .addPendingEvent(groupId, eventId, event.getVotingDuration(), metrics);
            if (updatePendingEvent.success) {
              resultStatus = new ResultStatus(true, "Event updated successfully");
            } else {
              resultStatus.resultMessage = "Error updating pending event mapping with voting duration";
            }
          } else {
            //we need to loop over the voting results and figure out the yes percentage of votes for the choices
            //set the selected choice as the one with the highest percent
            updatedEvent.setSelectedChoice(this.getSelectedChoice(event, metrics));

            DatabaseManagers.GROUPS_MANAGER
                .updateEvent(group, eventId, updatedEvent, false, metrics);

            // now remove the entry from the pending events table since it has been fully processed now
            String updateExpression = "remove #groupEventKey";
            NameMap nameMap = new NameMap().with("#groupEventKey", groupId + DELIM + eventId);

            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(this.getPrimaryKeyIndex(), scannerId)
                .withUpdateExpression(updateExpression)
                .withNameMap(nameMap);

            this.updateItem(updateItemSpec);

            resultStatus = new ResultStatus(true, "Pending event finalized successfully");
          }
        } else {
          resultStatus.resultMessage = "Error: Group data not found.";
          metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Group data not found."));
        }
      } catch (Exception e) {
        resultStatus.resultMessage = "Error: Unable to parse request in manager.";
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
      }
    } else {
      resultStatus.resultMessage = "Error: Required request keys not found.";
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found."));
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private Map<String, String> getTentativeAlgorithmChoices(final EventWithCategoryChoices event,
      final Integer numberOfChoices, final Metrics metrics) {
    final String classMethod = "PendingEventsManager.getTentativeAlgorithmChoices";
    metrics.commonSetup(classMethod);

    boolean success = true;
    Map<String, String> returnValue = new HashMap<>();

    try {
      final String categoryId = event.getCategoryId();

      //we need to make sure the event's category choices are setup
      if (event.getCategoryChoices() == null) {
        final Category category = new Category(
            DatabaseManagers.CATEGORIES_MANAGER.getItemByPrimaryKey(categoryId).asMap());
        event.setCategoryChoices(category.getChoices());
      }

      try {
        final List<User> optedInUsers = new ArrayList<>();
        for (String username : event.getOptedIn().keySet()) {
          optedInUsers.add(new User(DatabaseManagers.USERS_MANAGER.getMapByPrimaryKey(username)));
        }

        final DataCruncher dataCruncher = new DataCruncher(event, optedInUsers, metrics);
        dataCruncher.crunch(K);
        returnValue = dataCruncher.getTopXAllChoices(numberOfChoices);
      } catch (final Exception e) {
        metrics.log(new ErrorDescriptor<>(event.asMap(), classMethod, e));
      }
    } catch (Exception e) {
      // we have an event pointing to a non existent category
      returnValue.putIfAbsent("1", "Error");
      success = false;
      metrics.log(new ErrorDescriptor<>(event, classMethod, e));
    }

    metrics.commonClose(success);
    return returnValue;
  }

  private String getKeyWithMaxMapping(final Map<String, Integer> input) {
    if (input == null || input.isEmpty()) {
      return ""; // maybe throw an exception idk
    }

    String maxKey = null;
    for (final String key : input.keySet()) {
      if (maxKey == null || input.get(key) > input.get(maxKey)) {
        maxKey = key;
      }
    }

    return maxKey;
  }

  public String getSelectedChoice(final Event event, final Metrics metrics) {
    final String classMethod = "PendingEventsManager.getSelectedChoice";
    String selectedChoice;

    try {
      final Map<String, Integer> votingSums = new HashMap<>();
      final Map<String, Map<String, Integer>> votingNumbers = event.getVotingNumbers();
      for (final String choiceId : votingNumbers.keySet()) {
        Integer sum = 0;
        for (final Integer vote : votingNumbers.get(choiceId).values()) {
          //I think this could technically just be sum += vote, but this is more safe
          if (vote == 1) {
            sum++;
          }
        }

        votingSums.put(choiceId, sum);
      }

      String maxChoiceId = this.getKeyWithMaxMapping(votingSums);
      selectedChoice = event.getTentativeAlgorithmChoices().get(maxChoiceId);
    } catch (Exception e) {
      selectedChoice = "Error";
      metrics.log(new ErrorDescriptor<>(event, classMethod, e));
    }

    return selectedChoice;
  }

  public ResultStatus addPendingEvent(final String groupId, final String eventId,
      final Integer pollDuration, final Metrics metrics) {
    if (pollDuration <= 0) {
      return new ResultStatus(true, "No insert needed");
    }

    final String classMethod = "PendingEventsManager.addPendingEvent";
    metrics.commonSetup(classMethod);

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
      metrics.log(new ErrorDescriptor<>(String
          .format("Group: %s, Event: %s, duration: %s", groupId, eventId, pollDuration.toString()),
          classMethod, e));
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public void scanPendingEvents(final String scannerId, final Metrics metrics) {
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
                    .startStepMachineExecution(groupId, eventId, scannerId, metrics));
              } else {
                metrics.log(new ErrorDescriptor<>("scanner id: " + scannerId + ", key : " + key,
                    classMethod, "bad format for key in pending events table"));
                success = false;
              }
            }
          } catch (Exception e) {
            metrics.log(
                new ErrorDescriptor<>("scanner id: " + scannerId + ", key : " + key, classMethod,
                    e));
            success = false;
          }
        }
      }
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>("scanner id: " + scannerId, classMethod, e));
      success = false;
    }

    metrics.commonClose(success);
  }

  public ResultStatus deleteAllPendingGroupEvents(final String groupId, final Set<String> eventIds,
      final Metrics metrics) {
    if (eventIds.isEmpty()) {
      return new ResultStatus(true, "No events to delete");
    }

    final String classMethod = "PendingEventsManager.deleteAllPendingGroupEvents";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    StringBuilder updateExpression = new StringBuilder();
    final NameMap nameMap = new NameMap();

    int i = 0;
    String groupEventKey;
    for (final String eventId : eventIds) {
      //this will create distinct name maps for every key we need to remove
      groupEventKey = "#groupEventKey" + i;
      if (i == 0) {
        updateExpression.append("remove ").append(groupEventKey);
        nameMap.with(groupEventKey, groupId + DELIM + eventId);
      } else {
        updateExpression.append(", ").append(groupEventKey);
        nameMap.with(groupEventKey, groupId + DELIM + eventId);
      }
      i++;
    }

    final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
        .withUpdateExpression(updateExpression.toString())
        .withNameMap(nameMap);

    final int numberOfPartitions = Integer.parseInt(System.getenv(NUMBER_OF_PARTITIONS_ENV_KEY));
    for (i = 1; i <= numberOfPartitions; i++) {
      try {
        updateItemSpec.withPrimaryKey(this.getPrimaryKeyIndex(), Integer.valueOf(i).toString());
        this.updateItem(updateItemSpec);
      } catch (final Exception e) {
        metrics.log(new ErrorDescriptor<>(i, classMethod, e));
      }
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public String getPartitionKey() throws NullPointerException, NumberFormatException {
    //this gives a 'randomized' key based on the system's clock time.
    return Long.toString(
        (System.currentTimeMillis() % Integer.parseInt(System.getenv(NUMBER_OF_PARTITIONS_ENV_KEY)))
            + 1);
  }
}
