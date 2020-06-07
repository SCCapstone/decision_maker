package handlers;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;
import javax.inject.Inject;
import managers.DbAccessManager;
import managers.SnsAccessManager;
import models.Category;
import models.Event;
import models.EventWithCategoryChoices;
import models.Group;
import models.GroupWithCategoryChoices;
import models.Metadata;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.NondeterministicOptimalChoiceSelector;
import utilities.RequestFields;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class ProcessPendingEventHandler {

  private static final Float K = 0.2f;
  private static final String DELIM = ";";

  private DbAccessManager dbAccessManager;
  private SnsAccessManager snsAccessManager;
  private AddPendingEventHandler addPendingEventHandler;
  private Metrics metrics;

  @Inject
  public ProcessPendingEventHandler(final DbAccessManager dbAccessManager,
      final SnsAccessManager snsAccessManager, final AddPendingEventHandler addPendingEventHandler,
      final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.snsAccessManager = snsAccessManager;
    this.addPendingEventHandler = addPendingEventHandler;
    this.metrics = metrics;
  }

  /**
   * This function handles toking a pending event and figuring out what needs to be done to it so
   * that it can process to the next level.
   *
   * @param groupId   The group id of the event that needs to be processed.
   * @param eventId   The id of the event that needs to be processed.
   * @param scannerId The partition that the pending event exists within. If this is null then this
   *                  processing is being done for a brand new event.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String groupId, final String eventId, String scannerId) {
    final String classMethod = "ProcessPendingEventHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Boolean isNewEvent = (scannerId == null);

      final Item groupData = this.dbAccessManager.getGroupItem(groupId);
      if (groupData != null) { // if null, assume the group was deleted
        final GroupWithCategoryChoices group = new GroupWithCategoryChoices(groupData.asMap());
        final EventWithCategoryChoices event = group.getEventsWithCategoryChoices().get(eventId);
        final Event updatedEvent = event.clone();

        //first, set whatever needs to be set on the pending event
        if (event.getTentativeAlgorithmChoices().isEmpty()) {
          //we need to set the tentative choices
          final Map<String, String> tentativeChoices;
          if (event.getVotingDuration() > 0) {
            tentativeChoices = this.getTentativeAlgorithmChoices(event, 3);
          } else {
            //skipping voting, we also need to set the selected choice
            tentativeChoices = this.getTentativeAlgorithmChoices(event, 1);
            updatedEvent.setSelectedChoice(tentativeChoices.values().toArray()[0].toString());
          }

          updatedEvent.setTentativeAlgorithmChoices(tentativeChoices);
        } else {
          //we need to set the selected choice as the one with the highest percent
          updatedEvent.setSelectedChoice(this.getSelectedChoice(event));
        }

        //update the event with whatever got added to it
        this.updateEvent(group, eventId, updatedEvent, isNewEvent);

        if (updatedEvent.getSelectedChoice() == null) {
          //if this event is still pending, add it back into the pending events table
          final ResultStatus updatePendingEvent = this.addPendingEventHandler
              .handle(groupId, eventId, event.getVotingDuration());
          if (updatePendingEvent.success) {
            resultStatus = ResultStatus.successful("Event updated successfully");
          } else {
            resultStatus = ResultStatus
                .failure("Error updating pending event mapping with voting duration");
          }
        } else {
          //event finalized -> remove entry from the pending events table if it was ever put there
          if (!isNewEvent) {
            final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression("remove #groupEventKey")
                .withNameMap(new NameMap().with("#groupEventKey", groupId + DELIM + eventId));

            this.dbAccessManager.updatePendingEvent(scannerId, updateItemSpec);
          }

          resultStatus = ResultStatus.successful("Pending event finalized successfully");
        }
      } else {
        resultStatus = ResultStatus.failure("Error: Group data not found.");
        this.metrics.logWithBody(new WarningDescriptor<>(classMethod, "Group data not found."));
      }
    } catch (Exception e) {
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This method handles getting the top N choices for an event based on the considered users'
   * category choice ratings.
   *
   * @param event           This is the event that tentative choices are being gotten for.
   * @param numberOfChoices This is the number of tentative choices we want to set.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  private Map<String, String> getTentativeAlgorithmChoices(final EventWithCategoryChoices event,
      final Integer numberOfChoices) {
    final String classMethod = "ProcessPendingEventHandler.getTentativeAlgorithmChoices";
    this.metrics.commonSetup(classMethod);

    boolean success = true;
    Map<String, String> returnValue = new HashMap<>();

    try {
      final String categoryId = event.getCategoryId();

      //we need to make sure the event's category choices are setup
      if (event.getCategoryChoices() == null) {
        final Category category = this.dbAccessManager.getCategory(categoryId);
        event.setCategoryChoices(category.getChoices());
      }

      final List<User> optedInUsers = new ArrayList<>();
      for (String username : event.getOptedIn().keySet()) {
        optedInUsers.add(this.dbAccessManager.getUser(username));
      }

      final NondeterministicOptimalChoiceSelector nondeterministicOptimalChoiceSelector =
          new NondeterministicOptimalChoiceSelector(event, optedInUsers, metrics);
      nondeterministicOptimalChoiceSelector.crunch(K);
      returnValue = nondeterministicOptimalChoiceSelector.getTopXChoices(numberOfChoices);
    } catch (Exception e) {
      returnValue.putIfAbsent("1", "Error");
      success = false;
      this.metrics.log(new ErrorDescriptor<>(event, classMethod, e));
    }

    this.metrics.commonClose(success);
    return returnValue;
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

  //finds the key that maps to the largest integer
  private String getKeyWithMaxMapping(final Map<String, Integer> input) {
    String maxKey = null;
    for (final String key : input.keySet()) {
      if (maxKey == null || input.get(key) > input.get(maxKey)) {
        maxKey = key;
      }
    }

    return maxKey;
  }

  /**
   * This method handles updating an event in a group. It does this by taking in the old group
   * definition and the updated event and it compares what was on the group to what is being put on
   * the group. In this way, it can know what was updated and it can know what data needs to be
   * denormalized to the users table.
   *
   * @param oldGroup     The group object before the event was updated.
   * @param eventId      The id of the event that was update.
   * @param updatedEvent The event object with the updates registered.
   * @param isNewEvent   Is this update for a new event or for one that has been pending for some
   *                     duration?
   * @return Standard result status object giving insight on whether the request was successful.
   */
  private ResultStatus updateEvent(final Group oldGroup, final String eventId,
      final Event updatedEvent, final Boolean isNewEvent) {
    final String classMethod = "ProcessPendingEventHandler.updateEvent";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      final Event oldEvent = oldGroup.getEvents().get(eventId);

      final String lastActivity = this.dbAccessManager.now();

      String updateExpression = "set " + Group.LAST_ACTIVITY + " = :currentDate";
      ValueMap valueMap = new ValueMap().withString(":currentDate", lastActivity);
      NameMap nameMap = new NameMap();

      //set all of the update statements
      if (oldEvent.getTentativeAlgorithmChoices().isEmpty()) {
        //if the old event did not have tentative choices set, it must have be getting them
        if (updatedEvent.getTentativeAlgorithmChoices().isEmpty()) {
          throw new Exception("Empty tentative choices must be filled!");
        }

        updateExpression +=
            ", " + Group.EVENTS + ".#eventId." + Event.TENTATIVE_CHOICES + " = :tentativeChoices, "
                + Group.EVENTS + ".#eventId." + Event.VOTING_NUMBERS + " = :votingNumbers";

        nameMap.with("#eventId", eventId);
        valueMap.withMap(":tentativeChoices", updatedEvent.getTentativeAlgorithmChoices())
            .withMap(":votingNumbers",
                this.getVotingNumbersSetup(updatedEvent.getTentativeAlgorithmChoices()));
      }

      if (oldEvent.getSelectedChoice() == null && updatedEvent.getSelectedChoice() != null) {
        updateExpression +=
            ", " + Group.EVENTS + ".#eventId." + Event.SELECTED_CHOICE + " = :selectedChoice";
        nameMap.with("#eventId", eventId);
        valueMap.withString(":selectedChoice", updatedEvent.getSelectedChoice());
      }
      //end setting update statements

      if (nameMap.containsKey("#eventId")) {
        //after any update, the event is not longer in consider and the choices are not needed!
        updateExpression += " remove " + Group.EVENTS + ".#eventId." + Category.CHOICES;

        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap)
            .withValueMap(valueMap);

        this.dbAccessManager.updateGroup(oldGroup.getGroupId(), updateItemSpec);

        final Group newGroup = oldGroup.clone();
        newGroup.setLastActivity(lastActivity);
        newGroup.getEvents().put(eventId, updatedEvent);

        //the event has been updated on the group, update the user-groups now
        this.updateUsersTable(newGroup, eventId, isNewEvent);

        resultStatus = new ResultStatus(true, "Event updated successfully.");
      } else {
        // this method shouldn't get called if there is nothing to update
        throw new Exception("Nothing to update");
      }
    } catch (final Exception e) {
      this.metrics.log(new ErrorDescriptor<>(oldGroup.asMap(), classMethod, e));
      resultStatus.resultMessage = "Exception in " + classMethod;
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This method updates user items based on the changed definition of a group
   *
   * @param newGroup       The new group definition after the update.
   * @param updatedEventId This is the event id of an event that just changed states. Null means
   *                       this isn't being called from an event update.
   * @param isNewEvent     Boolean on whether or not this is a brand new event being processed.
   */
  private void updateUsersTable(final Group newGroup, final String updatedEventId,
      final Boolean isNewEvent) {
    final String classMethod = "ProcessPendingEventHandler.updateUsersTable";
    this.metrics.commonSetup(classMethod);

    boolean success = true;

    final String newEventCreator = newGroup.getEvents().get(updatedEventId)
        .getEventCreatorUsername();

    final String updateExpression =
        "set " + User.GROUPS + ".#groupId." + Group.LAST_ACTIVITY + " = :lastActivity, "
            + User.GROUPS + ".#groupId." + User.EVENTS_UNSEEN + ".#eventId = :true";
    final NameMap nameMap = new NameMap().with("#groupId", newGroup.getGroupId())
        .with("#eventId", updatedEventId);
    final ValueMap valueMap = new ValueMap()
        .withString(":lastActivity", newGroup.getLastActivity())
        .withBoolean(":true", true);
    final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
        .withUpdateExpression(updateExpression)
        .withValueMap(valueMap)
        .withNameMap(nameMap);

    for (final String username : newGroup.getMembers().keySet()) {
      try {
        if (!isNewEvent || !username.equals(newEventCreator)) {
          this.dbAccessManager.updateUser(username, updateItemSpec);
        } else { // This must be a new event because boolean logic
          // this means the username is the event creator, we should only update the last activity
          final String updateExpressionEventCreator =
              "set " + User.GROUPS + ".#groupId." + Group.LAST_ACTIVITY + " = :lastActivity";
          final ValueMap valueMapEventCreator = new ValueMap()
              .withString(":lastActivity", newGroup.getLastActivity());
          final NameMap nameMapEventCreator = new NameMap()
              .with("#groupId", newGroup.getGroupId());
          final UpdateItemSpec updateItemSpecEventCreator = new UpdateItemSpec()
              .withUpdateExpression(updateExpressionEventCreator)
              .withValueMap(valueMapEventCreator)
              .withNameMap(nameMapEventCreator);

          this.dbAccessManager.updateUser(username, updateItemSpecEventCreator);
        }
      } catch (final Exception e) {
        success = false;
        metrics.log(new ErrorDescriptor<>(username, classMethod, e));
      }
    }

    //blind send push notifications to the user's that just had their items updated
    this.sendEventUpdatedNotification(newGroup, updatedEventId, isNewEvent);

    this.metrics.commonClose(success);
  }

  private void sendEventUpdatedNotification(final Group group, final String eventId,
      final Boolean isNewEvent) {
    final String classMethod = "ProcessPendingEventHandler.sendEventUpdatedNotification";
    this.metrics.commonSetup(classMethod);

    boolean success = true;

    final Event updatedEvent = group.getEvents().get(eventId);
    final String updatedEventCreator = updatedEvent.getEventCreatorUsername();

    final Map<String, Object> payload = updatedEvent.asMap();
    payload.putIfAbsent(Group.GROUP_ID, group.getGroupId());
    payload.putIfAbsent(Group.GROUP_NAME, group.getGroupName());
    payload.putIfAbsent(Group.LAST_ACTIVITY, group.getLastActivity());

    String action = "eventCreated";

    String eventChangeTitle = "Event in " + group.getGroupName();

    //assume the event just got created - this can't be true otherwise we wouldn't hit this flow
    String eventChangeBody =
        "'" + updatedEvent.getEventName() + "' created by: " + updatedEvent
            .getEventCreatorDisplayName();

    if (updatedEvent.getSelectedChoice() != null) {
      //we just transitioned to a having a selected choice -> stage: occurring
      action = "eventChosen";
      eventChangeBody =
          updatedEvent.getEventName() + " - " + updatedEvent.getSelectedChoice() + " Won!";
    } else if (!updatedEvent.getTentativeAlgorithmChoices().isEmpty()) {
      //we just transitioned to getting tentative choices -> stage: voting
      action = "eventVoting";
      eventChangeBody = "Vote for " + updatedEvent.getEventName();
    } // else the event was indeed just created

    final Metadata metadata = new Metadata(action, payload);

    for (String username : group.getMembers().keySet()) {
      //if it's a new event, the event creator doesn't need a notification
      if (!(isNewEvent && username.equals(updatedEventCreator))) {
        try {
          //we aren't using cache here since the old user might have been cached before updating the
          // users table if this processing required getting the tentative algorithm choices
          final User user = this.dbAccessManager.getUserNoCache(username);

          if (user.pushEndpointArnIsSet()) {
            //each user needs to know how many events they haven't seen for the given group now
            metadata.overwritePayload(User.EVENTS_UNSEEN,
                user.getGroups().get(group.getGroupId()).getEventsUnseen().size());

            if (user.getAppSettings().isMuted() || user.getGroups().get(group.getGroupId())
                .isMuted()) {
              this.snsAccessManager.sendMutedMessage(user.getPushEndpointArn(), metadata);
            } else {
              this.snsAccessManager
                  .sendMessage(user.getPushEndpointArn(), eventChangeTitle, eventChangeBody,
                      eventId, metadata);
            }
          }
        } catch (Exception e) {
          success = false;
          this.metrics.log(new ErrorDescriptor<>(username, classMethod, e));
        }
      }
    }

    this.metrics.commonClose(success);
  }

  //This method takes in a list of TentativeAlgorithmChoices and builds the corresponding
  //default voting numbers map
  private Map<String, Map> getVotingNumbersSetup(
      final Map<String, String> tentativeAlgorithmChoices) {
    final Map<String, Map> votingNumbers = new HashMap<>();

    //we're filling a map keyed by choiceId mapped to empty maps
    for (String choiceId : tentativeAlgorithmChoices.keySet()) {
      votingNumbers.put(choiceId, new HashMap<>());
    }

    return votingNumbers;
  }
}
