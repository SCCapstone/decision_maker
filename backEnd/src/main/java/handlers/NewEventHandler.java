package handlers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import managers.DbAccessManager;
import models.Category;
import models.Event;
import models.EventWithCategoryChoices;
import models.Group;
import models.GroupForApiResponse;
import models.User;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class NewEventHandler implements ApiRequestHandler {

  private static final Integer MAX_DURATION = 10000;
  private static final Integer MAX_EVENT_NAME_LENGTH = 30;

  private DbAccessManager dbAccessManager;
  private Metrics metrics;

  public NewEventHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method handles creating a new event within a group. It validates the input, updates the
   * table and then updating the denormalized user group maps accordingly. Note: if this event is
   * going to skip rsvp, control is passed to the pending events manager is used to handle this
   * flow.
   *
   * @param activeUser TODO
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String groupId, final String eventName,
      final String categoryId, final Integer rsvpDuration, final Integer votingDuration,
      final String eventStartDateTime, final Integer utcStartSeconds) {
    final String classMethod = "NewEventHandler.handle";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Group oldGroup = this.dbAccessManager.getGroup(groupId);

      final String eventId = UUID.randomUUID().toString();
      final User eventCreator = this.dbAccessManager.getUser(activeUser);

      //we use the 'WithCategoryChoices' variant so that if we need to save the snapshot of the
      //category, then we'll have the data present for that.
      final EventWithCategoryChoices newEvent = new EventWithCategoryChoices();
      newEvent.setEventName(eventName);
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

        String updateExpression =
            "set " + Group.EVENTS + ".#eventId = :map, " + Group.LAST_ACTIVITY + " = :lastActivity";
        NameMap nameMap = new NameMap().with("#eventId", eventId);
        ValueMap valueMap = new ValueMap().withString(":lastActivity", lastActivity);

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
          final ResultStatus pendingEventAdded = DatabaseManagers.PENDING_EVENTS_MANAGER
              .addPendingEvent(groupId, eventId, newEvent.getRsvpDuration(), metrics);
        } else {
          //this will set potential algo choices and create the entry for voting duration timeout
          final Map<String, Object> processPendingEventInput = ImmutableMap.of(
              Group.GROUP_ID, groupId,
              RequestFields.EVENT_ID, eventId,
              PendingEventsManager.SCANNER_ID,
              DatabaseManagers.PENDING_EVENTS_MANAGER.getPartitionKey(),
              RequestFields.NEW_EVENT, true
          );
          final ResultStatus pendingEventAdded = DatabaseManagers.PENDING_EVENTS_MANAGER
              .processPendingEvent(processPendingEventInput, metrics);
        }

        final Group newGroup = oldGroup.clone();
        newGroup.getEvents().put(eventId, newEvent);
        newGroup.setLastActivity(lastActivity);

        //when rsvp is not greater than 0, updateUsersTable gets called by updateEvent
        if (newEvent.getRsvpDuration() > 0) {
          this.updateUsersTable(oldGroup, newGroup, eventId, true, metrics);
        }

        resultStatus = new ResultStatus(true,
            JsonUtils.convertObjectToJson(new GroupForApiResponse(newGroup).asMap()));
      } else {
        metrics.logWithBody(new WarningDescriptor<>(classMethod, errorMessage.get()));
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
}
