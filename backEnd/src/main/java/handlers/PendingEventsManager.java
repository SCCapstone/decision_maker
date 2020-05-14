package handlers;

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
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import models.Category;
import models.Event;
import models.EventWithCategoryChoices;
import models.GroupWithCategoryChoices;
import models.User;
import utilities.ErrorDescriptor;
import utilities.IOStreamsHelper;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.NondeterministicOptimalChoiceSelector;
import utilities.RequestFields;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class PendingEventsManager extends DatabaseAccessManager {

  public static final String SCANNER_ID = "ScannerId";

  public static final String DELIM = ";";
  public static final String NUMBER_OF_PARTITIONS_ENV_KEY = "NUMBER_OF_PARTITIONS";

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
      StringBuilder escapedInput = new StringBuilder(JsonUtils.convertMapToJson(input));
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

  /**
   * This function handles tokaing a pending event and figuring out what needs to be done to it so
   * that it can process to the next level.
   *
   * @param jsonMap Standard json map containing the payload of an api request. This payload must
   *                have the group id and the event id of the pending event as well as the index of
   *                the pending events container that the event is in.
   * @param metrics Standard metrics object for profiling and logging.
   * @return Standard result status object giving insight on whether the request was successful.
   */
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

          //first, set whatever needs to be set on the pending event
          if (event.getTentativeAlgorithmChoices().isEmpty()) {
            //we need to set the tentative choices
            final Map<String, String> tentativeChoices;
            if (event.getVotingDuration() > 0) {
              tentativeChoices = this.getTentativeAlgorithmChoices(event, 3, metrics);
            } else {
              //skipping voting, we also need to set the selected choice
              tentativeChoices = this.getTentativeAlgorithmChoices(event, 1, metrics);
              updatedEvent.setSelectedChoice(tentativeChoices.values().toArray()[0].toString());
            }

            updatedEvent.setTentativeAlgorithmChoices(tentativeChoices);
          } else {
            //we need to set the selected choice as the one with the highest percent
            updatedEvent.setSelectedChoice(this.getSelectedChoice(event));
          }

          //update the event with whatever got added to it
          //TODO
//          DatabaseManagers.GROUPS_MANAGER
//              .updateEvent(group, eventId, updatedEvent, isNewEvent, metrics);

          if (updatedEvent.getSelectedChoice() == null) {
            //if this event is still pending, add it back into the pending events table
            final ResultStatus updatePendingEvent = this
                .addPendingEvent(groupId, eventId, event.getVotingDuration(), metrics);
            if (updatePendingEvent.success) {
              resultStatus = new ResultStatus(true, "Event updated successfully");
            } else {
              resultStatus.resultMessage = "Error updating pending event mapping with voting duration";
            }
          } else {
            //event finalized -> remove the entry from the pending events table
            final String updateExpression = "remove #groupEventKey";
            final NameMap nameMap = new NameMap().with("#groupEventKey", groupId + DELIM + eventId);

            final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression(updateExpression)
                .withNameMap(nameMap);

            this.updateItem(scannerId, updateItemSpec);

            resultStatus = new ResultStatus(true, "Pending event finalized successfully");
          }
        } else {
          resultStatus.resultMessage = "Error: Group data not found.";
          metrics.log(new WarningDescriptor<>(jsonMap, classMethod, "Group data not found."));
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

  /**
   * This method handles getting the top N choices for an event based on the considered users'
   * category choice ratings.
   *
   * @param event           This is the event that tentative choices are being gotten for.
   * @param numberOfChoices This is the number of tentative choices we want to set.
   * @param metrics         Standard metrics object for profiling and logging.
   * @return Standard result status object giving insight on whether the request was successful.
   */
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
        //TODO
//        final Category category = new Category(
//            DatabaseManagers.CATEGORIES_MANAGER.getItemByPrimaryKey(categoryId).asMap());
//        event.setCategoryChoices(category.getChoices());
      }

      final List<User> optedInUsers = new ArrayList<>();
      for (String username : event.getOptedIn().keySet()) {
        //TODO
        //optedInUsers.add(new User(DatabaseManagers.USERS_MANAGER.getMapByPrimaryKey(username)));
      }

      final NondeterministicOptimalChoiceSelector nondeterministicOptimalChoiceSelector =
          new NondeterministicOptimalChoiceSelector(event, optedInUsers, metrics);
      nondeterministicOptimalChoiceSelector.crunch(K);
      returnValue = nondeterministicOptimalChoiceSelector.getTopXChoices(numberOfChoices);
    } catch (Exception e) {
      returnValue.putIfAbsent("1", "Error");
      success = false;
      metrics.log(new ErrorDescriptor<>(event, classMethod, e));
    }

    metrics.commonClose(success);
    return returnValue;
  }

  private String getKeyWithMaxMapping(final Map<String, Integer> input) {
    String maxKey = null;
    for (final String key : input.keySet()) {
      if (maxKey == null || input.get(key) > input.get(maxKey)) {
        maxKey = key;
      }
    }

    return maxKey;
  }

  private String getSelectedChoice(final Event event) {
    final Map<String, Integer> votingSums = new HashMap<>();

    //reminder: the votingNumbers is a map from choice ids to maps of username to 0 or 1 (no or yes)
    final Map<String, Map<String, Integer>> votingNumbers = event.getVotingNumbers();
    for (final String choiceId : votingNumbers.keySet()) {
      Integer sum = 0;
      for (final Integer vote : votingNumbers.get(choiceId).values()) {
        //the vote values are restricted to 0 or 1 in GroupsManager.voteForChoice
        sum += vote;
      }

      votingSums.put(choiceId, sum);
    }

    //determine what the highest vote sum is
    final Integer maxVoteSum = votingSums.get(this.getKeyWithMaxMapping(votingSums));

    //we then get all choice ids that had that max vote sun
    final List<String> maxChoiceIds = votingSums.entrySet().stream()
        .filter(e -> e.getValue().equals(maxVoteSum))
        .map(Entry::getKey).collect(Collectors.toList());

    //pick one of the max keys randomly
    final String selectedMaxChoiceId = maxChoiceIds
        .get(new Random().nextInt(maxChoiceIds.size()));

    //return the appropriate choice label
    return event.getTentativeAlgorithmChoices().get(selectedMaxChoiceId);
  }

  /**
   * This method handles adding a pending event into one of the pending event table partitions.
   *
   * @param groupId      The group that the pending event belongs to.
   * @param eventId      The event that is pending.
   * @param pollDuration The duration that the event will pend for.
   * @param metrics      Standard metrics object for profiling and logging.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus addPendingEvent(final String groupId, final String eventId,
      final Integer pollDuration, final Metrics metrics) {
    if (pollDuration <= 0) { // there no 'pending' needed for zero minutes of duration
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

  /**
   * This method is run every minute by a cron job. It looks at one of the partitions of the pending
   * events table and if there are any pending events that are ready to be processed, then it kicks
   * off a step function process to handle that event's resolution.
   *
   * @param scannerId The partition of the table that this function should scan for ready events.
   * @param metrics   Standard metrics object for profiling and logging.
   */
  public void scanPendingEvents(final String scannerId, final Metrics metrics) {
    final String classMethod = "PendingEventsManager.scanPendingEvents";
    metrics.commonSetup(classMethod);

    //if anything fails we'll set this to false so "everything" fails -> we can then investigate
    boolean success = true;

    try {
      final Map<String, Object> pendingEventsData = this.getItemByPrimaryKey(scannerId).asMap();
      final LocalDateTime currentDate = LocalDateTime.now(ZoneId.of("UTC"));
      LocalDateTime resolutionDate;

      String groupId, eventId;
      List<String> keyPair;
      for (String key : pendingEventsData.keySet()) {
        //skip the scanner id key as it isn't real pending event data
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
                new ErrorDescriptor<>(String.format("scanner id: %s, key: %s", scannerId, key),
                    classMethod, e));
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

  public String getPartitionKey() throws NullPointerException, NumberFormatException {
    //this gives a 'randomized' key based on the system's clock time.
    return Long.toString(
        (System.currentTimeMillis() % Integer.parseInt(System.getenv(NUMBER_OF_PARTITIONS_ENV_KEY)))
            + 1);
  }
}
