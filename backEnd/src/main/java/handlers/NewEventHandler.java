package handlers;

import static utilities.Config.MAX_DURATION;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import managers.DbAccessManager;
import managers.SnsAccessManager;
import models.Category;
import models.Event;
import models.EventWithCategoryChoices;
import models.Group;
import models.GroupForApiResponse;
import models.Metadata;
import models.User;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class NewEventHandler implements ApiRequestHandler {

  private static final Integer MAX_EVENT_NAME_LENGTH = 30;

  private final DbAccessManager dbAccessManager;
  private final SnsAccessManager snsAccessManager;
  private final AddPendingEventHandler addPendingEventHandler;
  private final ProcessPendingEventHandler processPendingEventHandler;
  private final Metrics metrics;

  @Inject
  public NewEventHandler(final DbAccessManager dbAccessManager,
      final SnsAccessManager snsAccessManager, final AddPendingEventHandler addPendingEventHandler,
      final ProcessPendingEventHandler processPendingEventHandler, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.snsAccessManager = snsAccessManager;
    this.addPendingEventHandler = addPendingEventHandler;
    this.processPendingEventHandler = processPendingEventHandler;
    this.metrics = metrics;
  }

  /**
   * This method handles creating a new event within a group. It validates the input, updates the
   * table and then updating the denormalized user group maps accordingly. Note: if this event is
   * going to skip rsvp, control is passed to the pending events manager is used to handle this
   * flow.
   *
   * @param activeUser         The user making the api request.
   * @param groupId            The id of the group to associate the event within.
   * @param eventName          The name of the event.
   * @param categoryId         The id of the category to associate the event to.
   * @param rsvpDuration       The rsvp duration for the event.
   * @param votingDuration     The voting duration for the event.
   * @param eventStartDateTime The textual datetime start date time of the event.
   * @param utcStartSeconds    The utc seconds of the event.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String groupId, final String eventName,
      final String categoryId, final Integer rsvpDuration, final Integer votingDuration,
      final String eventStartDateTime, final Integer utcStartSeconds) {
    final String classMethod = "NewEventHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Group oldGroup = this.dbAccessManager.getGroup(groupId);
      final String eventId = UUID.randomUUID().toString();

      //we use the 'WithCategoryChoices' variant so that if we need to save the snapshot of the
      //category, then we'll have the data present for that.
      final EventWithCategoryChoices newEvent = new EventWithCategoryChoices();
      newEvent.setEventName(eventName);
      newEvent.setRsvpDuration(rsvpDuration);
      newEvent.setVotingDuration(votingDuration);
      newEvent.setEventStartDateTime(eventStartDateTime);
      newEvent.setUtcEventStartSeconds(utcStartSeconds);

      final User eventCreator = this.dbAccessManager.getUser(activeUser);
      newEvent.setEventCreator(ImmutableMap.of(activeUser, eventCreator.asMember()));

      final Optional<String> errorMessage = this.newEventInputIsValid(oldGroup, newEvent);
      if (!errorMessage.isPresent()) {
        //get the category and set category fields
        final String lastActivity = this.dbAccessManager.now();
        final Category category = this.dbAccessManager.getCategory(categoryId);
        newEvent.setCategoryFields(category);

        newEvent.setOptedIn(oldGroup.getMembers());
        newEvent.setCreatedDateTime(lastActivity);
        newEvent.setSelectedChoice(null);
        newEvent.setTentativeAlgorithmChoices(Collections.emptyMap());
        newEvent.setVotingNumbers(Collections.emptyMap());

        final String updateExpression =
            "set " + Group.EVENTS + ".#eventId = :map, " + Group.LAST_ACTIVITY + " = :lastActivity";
        final NameMap nameMap = new NameMap().with("#eventId", eventId);
        final ValueMap valueMap = new ValueMap().withString(":lastActivity", lastActivity);

        //if skipping rsvp -> only need to store the event map (don't need category choices)
        if (newEvent.getRsvpDuration() > 0) {
          valueMap.withMap(":map", newEvent.asMap());
        } else {
          valueMap.withMap(":map", newEvent.asEventMap());
        }

        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withNameMap(nameMap)
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);

        this.dbAccessManager.updateGroup(groupId, updateItemSpec);

        //Hope it works, we aren't using transactions yet (that's why nothing done with result).
        if (newEvent.getRsvpDuration() > 0) {
          final ResultStatus pendingEventAdded = this.addPendingEventHandler
              .handle(groupId, eventId, newEvent.getRsvpDuration());
        } else {
          //this will set potential algo choices and create the entry for voting duration timeout
          final ResultStatus pendingEventAdded = this.processPendingEventHandler
              .handle(groupId, eventId, null);
        }

        //since the event could have been updated by skipping consider, we need to pull to get the most up to date event
        final Group newGroup = this.dbAccessManager.getGroupNoCache(groupId);
//        final Group newGroup = oldGroup.clone();
//        newGroup.getEvents().put(eventId, newEvent);
//        newGroup.setLastActivity(lastActivity);

        //when rsvp is not greater than 0, updateUsersTable gets called by updateEvent
        if (newEvent.getRsvpDuration() > 0) {
          this.updateUsersTable(newGroup, eventId);
        }

        resultStatus = ResultStatus
            .successful(JsonUtils.convertObjectToJson(new GroupForApiResponse(newGroup).asMap()));
      } else {
        this.metrics.logWithBody(new WarningDescriptor<>(classMethod, errorMessage.get()));
        resultStatus = ResultStatus.failure(errorMessage.get());
      }
    } catch (Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private Optional<String> newEventInputIsValid(final Group oldGroup, final Event newEvent) {
    String errorMessage = null;

    if (!oldGroup.getMembers().keySet().contains(newEvent.getEventCreatorUsername())) {
      errorMessage = this.getUpdatedErrorMessage(errorMessage, "Error: creator not in group");
    }

    if (newEvent.getRsvpDuration() < 0 || newEvent.getRsvpDuration() > MAX_DURATION) {
      errorMessage = this.getUpdatedErrorMessage(errorMessage, "Error: invalid consider duration");
    }

    if (newEvent.getVotingDuration() < 0 || newEvent.getVotingDuration() > MAX_DURATION) {
      errorMessage = this.getUpdatedErrorMessage(errorMessage, "Error: invalid voting duration");
    }

    if (newEvent.getEventName().length() <= 0) {
      errorMessage = this.getUpdatedErrorMessage(errorMessage, "Error: event name is empty");
    }

    if (newEvent.getEventName().length() > MAX_EVENT_NAME_LENGTH) {
      errorMessage = this.getUpdatedErrorMessage(errorMessage, "Error: event name too long");
    }

    return Optional.ofNullable(errorMessage);
  }

  private String getUpdatedErrorMessage(final String current, final String update) {
    String invalidString;
    if (current == null) {
      invalidString = update;
    } else {
      invalidString = current + "\n" + update;
    }

    return invalidString;
  }

  /**
   * This method updates user items based on the changed definition of a group
   *
   * @param newGroup       The new group definition after the event has been added.
   * @param updatedEventId This is the event id of an event that just changed states. Null means
   *                       this isn't being called from an event update.
   */
  private void updateUsersTable(final Group newGroup, final String updatedEventId) {
    final String classMethod = "NewEventHandler.updateUsersTable";
    this.metrics.commonSetup(classMethod);

    boolean success = true;

    final String newEventCreator = newGroup.getEvents().get(updatedEventId)
        .getEventCreatorUsername();

    final String updateExpression = "set " + User.GROUPS + ".#groupId." + User.EVENTS_UNSEEN
        + ".#eventId = :true, " + User.GROUPS + ".#groupId." + Group.LAST_ACTIVITY
        + " = :lastActivity";
    final NameMap nameMap = new NameMap().with("#groupId", newGroup.getGroupId())
        .with("#eventId", updatedEventId);
    final ValueMap valueMap = new ValueMap().withBoolean(":true", true)
        .withString(":lastActivity", newGroup.getLastActivity());

    final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
        .withUpdateExpression(updateExpression)
        .withValueMap(valueMap)
        .withNameMap(nameMap);

    for (final String username : newGroup.getMembers().keySet()) {
      try {
        if (!username.equals(newEventCreator)) {
          this.dbAccessManager.updateUser(username, updateItemSpec);
        } else {
          // the username is the event creator -> we should only update the last activity
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
        this.metrics.log(new ErrorDescriptor<>(username, classMethod, e));
      }
    }

    //blind send...
    this.sendEventUpdatedNotification(newGroup, updatedEventId);

    this.metrics.commonClose(success);
  }

  private void sendEventUpdatedNotification(final Group group, final String eventId) {
    final String classMethod = "NewEventHandler.sendEventUpdatedNotification";
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

    //assume the event just got created with no skips
    String eventChangeBody =
        "'" + updatedEvent.getEventName() + "' created by: " + updatedEvent
            .getEventCreatorDisplayName();

    if (updatedEvent.getSelectedChoice() != null) {
      //we just transitioned to a having a selected choice -> stage: occurring
      action = "eventChosen";
      eventChangeBody =
          updatedEvent.getEventName() + ": " + updatedEvent.getSelectedChoice() + " Won!";
    } else if (!updatedEvent.getTentativeAlgorithmChoices().isEmpty()) {
      //we just transitioned to getting tentative choices -> stage: voting
      action = "eventVoting";
      eventChangeBody = "Vote for " + updatedEvent.getEventName();
    } // else the event was indeed just created without skips

    final Metadata metadata = new Metadata(action, payload);

    for (String username : group.getMembers().keySet()) {
      //for new events the creator doesn't need to be alerted that an event was created.
      if (!username.equals(updatedEventCreator)) {
        try {
          final User user = this.dbAccessManager.getUser(username);

          if (user.pushEndpointArnIsSet()) {
            //each user needs to know how many events they haven't seen for the given group now
            metadata.addToPayload(User.EVENTS_UNSEEN,
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
}
