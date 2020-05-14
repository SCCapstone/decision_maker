package handlers;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.google.common.collect.ImmutableMap;
import exceptions.InvalidAttributeValueException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import models.Category;
import models.Event;
import models.EventForSorting;
import models.EventWithCategoryChoices;
import models.Group;
import models.GroupForApiResponse;
import models.Metadata;
import models.User;
import models.UserGroup;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class GroupsManager extends DatabaseAccessManager {

  public static final String GROUP_ID = "GroupId";
  public static final String GROUP_NAME = "GroupName";
  public static final String ICON = "Icon";
  public static final String GROUP_CREATOR = "GroupCreator";
  public static final String MEMBERS = "Members";
  public static final String MEMBERS_LEFT = "MembersLeft";
  public static final String CATEGORIES = "Categories";
  public static final String DEFAULT_VOTING_DURATION = "DefaultVotingDuration";
  public static final String DEFAULT_RSVP_DURATION = "DefaultRsvpDuration";
  public static final String EVENTS = "Events";
  public static final String LAST_ACTIVITY = "LastActivity";
  public static final String IS_OPEN = "IsOpen";

  public static final String CATEGORY_ID = "CategoryId";
  public static final String CATEGORY_NAME = "CategoryName";
  public static final String EVENT_NAME = "EventName";
  public static final String EVENT_CREATOR = "EventCreator";
  public static final String CREATED_DATE_TIME = "CreatedDateTime";
  public static final String EVENT_START_DATE_TIME = "EventStartDateTime";
  public static final String UTC_EVENT_START_SECONDS = "UtcEventStartSeconds";
  public static final String VOTING_DURATION = "VotingDuration";
  public static final String RSVP_DURATION = "RsvpDuration";
  public static final String OPTED_IN = "OptedIn";
  public static final String VOTING_NUMBERS = "VotingNumbers";
  public static final String TENTATIVE_CHOICES = "TentativeAlgorithmChoices";
  public static final String SELECTED_CHOICE = "SelectedChoice";

  public GroupsManager() {
    super("groups", "GroupId", Regions.US_EAST_2);
  }

  public GroupsManager(final DynamoDB dynamoDB) {
    super("groups", "GroupId", Regions.US_EAST_2, dynamoDB);
  }

  private void sendAddedToGroupNotifications(final Set<String> usernames, final Group addedTo,
      final Metrics metrics) {
    final String classMethod = "GroupsManager.sendAddedToGroupNotifications";
    metrics.commonSetup(classMethod);

    boolean success = true;

    final Map<String, Object> payload = UserGroup.fromNewGroup(addedTo).asMap();
    payload.putIfAbsent(GROUP_ID, addedTo.getGroupId());

    final Metadata metadata = new Metadata("addedToGroup", payload);

    for (String username : usernames) {
      try {
        final User user = new User(DatabaseManagers.USERS_MANAGER.getMapByPrimaryKey(username));

        //don't send a notification to the creator as they know they just created the group
        if (!username.equals(addedTo.getGroupCreator())) {
          if (user.pushEndpointArnIsSet()) {
            //Note: no need to check user's group muted settings since they're just being added
            if (user.getAppSettings().isMuted()) {
              DatabaseManagers.SNS_ACCESS_MANAGER
                  .sendMutedMessage(user.getPushEndpointArn(), metadata);
            } else {
              DatabaseManagers.SNS_ACCESS_MANAGER.sendMessage(user.getPushEndpointArn(),
                  "Added to new group!", addedTo.getGroupName(), addedTo.getGroupId(), metadata);
            }
          }
        }
      } catch (Exception e) {
        success = false;
        metrics.log(new ErrorDescriptor<>(username, classMethod, e));
      }
    }

    metrics.commonClose(success);
  }

  private ResultStatus removeUsersFromGroupAndSendNotificationsOnEdit(final Set<String> usernames,
      final Group removedFrom, final Metrics metrics) {
    return this.removeUsersFromGroupAndSendNotifications(usernames, removedFrom, false, metrics);
  }

  private ResultStatus removeUsersFromGroupAndSendNotificationsOnDelete(final Set<String> usernames,
      final Group removedFrom, final Metrics metrics) {
    return this.removeUsersFromGroupAndSendNotifications(usernames, removedFrom, true, metrics);
  }

  private ResultStatus removeUsersFromGroupAndSendNotifications(final Set<String> usernames,
      final Group removedFrom, final boolean isGroupDelete, final Metrics metrics) {
    final String classMethod = "GroupsManager.sendRemovedFromGroupNotifications";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus(true,
        "Active members removed and notified successfully.");

    final Metadata metadata = new Metadata("removedFromGroup",
        ImmutableMap.of(GROUP_ID, removedFrom.getGroupId()));

    final String updateExpression = "remove Groups.#groupId";
    final NameMap nameMap = new NameMap().with("#groupId", removedFrom.getGroupId());
    final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
        .withNameMap(nameMap)
        .withUpdateExpression(updateExpression);

    for (final String username : usernames) {
      try {
        //pull the user before deleting so we have their group isMuted mapping for the push notice
        final User removedUser = new User(
            DatabaseManagers.USERS_MANAGER.getMapByPrimaryKey(username));

        //actually do the delete
        DatabaseManagers.USERS_MANAGER.updateItem(username, updateItemSpec);

        //if the delete went through, send the notification
        if (removedUser.pushEndpointArnIsSet() && !removedFrom.getGroupCreator().equals(username)) {
          if (removedUser.getAppSettings().isMuted() || removedUser.getGroups()
              .get(removedFrom.getGroupId()).isMuted()) {
            DatabaseManagers.SNS_ACCESS_MANAGER
                .sendMutedMessage(removedUser.getPushEndpointArn(), metadata);
          } else {
            DatabaseManagers.SNS_ACCESS_MANAGER.sendMessage(removedUser.getPushEndpointArn(),
                isGroupDelete ? "Group Deleted" : "Removed from group", removedFrom.getGroupName(),
                removedFrom.getGroupId(), metadata);
          }
        }
      } catch (Exception e) {
        resultStatus = new ResultStatus(false, "Exception removing users from group");
        metrics.log(new ErrorDescriptor<>(username, classMethod, e));
      }
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private void sendEventUpdatedNotification(final Set<String> usernames,
      final Group group, final String eventId, final Boolean isNewEvent, final Metrics metrics) {
    final String classMethod = "GroupsManager.sendEventUpdatedNotification";
    metrics.commonSetup(classMethod);

    boolean success = true;

    final Event updatedEvent = group.getEvents().get(eventId);
    final String updatedEventCreator = updatedEvent.getEventCreatorUsername();

    final Map<String, Object> payload = updatedEvent.asMap();
    payload.putIfAbsent(GROUP_ID, group.getGroupId());
    payload.putIfAbsent(GROUP_NAME, group.getGroupName());
    payload.putIfAbsent(LAST_ACTIVITY, group.getLastActivity());
    payload.putIfAbsent(RequestFields.EVENT_ID, eventId);

    String action = "eventCreated";

    String eventChangeTitle = "Event in " + group.getGroupName();

    //assume the event just got created
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
    } // else the event was indeed just created

    final Metadata metadata = new Metadata(action, payload);

    for (String username : usernames) {
      if (!(isNewEvent && username.equals(updatedEventCreator))) {
        try {
          final User user = new User(DatabaseManagers.USERS_MANAGER.getMapByPrimaryKey(username));

          if (user.pushEndpointArnIsSet()) {
            if (user.getAppSettings().isMuted() || user.getGroups().get(group.getGroupId())
                .isMuted()) {
              DatabaseManagers.SNS_ACCESS_MANAGER
                  .sendMutedMessage(user.getPushEndpointArn(), metadata);
            } else {
              DatabaseManagers.SNS_ACCESS_MANAGER
                  .sendMessage(user.getPushEndpointArn(), eventChangeTitle, eventChangeBody,
                      eventId, metadata);
            }
          }
        } catch (Exception e) {
          success = false;
          metrics.log(new ErrorDescriptor<>(username, classMethod, e));
        }
      }
    }

    metrics.commonClose(success);
  }

  /**
   * This method updates user items based on the changed definition of a group
   *
   * @param oldGroup       The old group definition before the update. If this param is null, then
   *                       that signals that a new group is being created.
   * @param newGroup       The new group definition after the update.
   * @param updatedEventId This is the event id of an event that just changed states. Null means
   *                       this isn't being called from an event update.
   * @param metrics        Standard metrics object for profiling and logging
   */
  private void updateUsersTable(final Group oldGroup, final Group newGroup,
      final String updatedEventId, final Boolean isNewEvent, final Metrics metrics) {
    final String classMethod = "GroupsManager.updateUsersTable";
    metrics.commonSetup(classMethod);
    boolean success = true;

    String newEventCreator = null;
    if (isNewEvent) {
      newEventCreator = newGroup.getEvents().get(updatedEventId).getEventCreatorUsername();
    }

    NameMap nameMap = new NameMap().with("#groupId", newGroup.getGroupId());

    final Set<String> newMembers = newGroup.getMembers().keySet();
    final Set<String> addedUsernames = new HashSet<>(newMembers);
    final Set<String> persistingUsernames = new HashSet<>(newMembers);
    final Set<String> removedUsernames = new HashSet<>();

    if (oldGroup != null) {
      final Set<String> oldMembers = oldGroup.getMembers().keySet();
      removedUsernames.addAll(oldMembers);

      persistingUsernames.retainAll(oldMembers);

      // Note: using removeAll on a HashSet has linear time complexity when another HashSet is passed in
      addedUsernames.removeAll(oldMembers);
      removedUsernames.removeAll(newMembers);
    }

    String updateExpression;
    ValueMap valueMap;
    UpdateItemSpec updateItemSpec;

    //update users with new group mapping based on which attributes were updated
    if (oldGroup != null) {
      //NOTE: There will always be at least 1 user in the group (the creator)

      //since this group already exists, we're just updating the mappings that have changed for existing users
      //for simplicity in the code, we'll always update the group name
      updateExpression = "set " + UsersManager.GROUPS + ".#groupId." + GROUP_NAME + " = :groupName";
      valueMap = new ValueMap().withString(":groupName", newGroup.getGroupName());

      if (newGroup.iconIsSet() && !newGroup.getIcon().equals(oldGroup.getIcon())) {
        updateExpression += ", " + UsersManager.GROUPS + ".#groupId." + ICON + " = :groupIcon";
        valueMap.withString(":groupIcon", newGroup.getIcon());
      }

      if (!newGroup.getLastActivity().equals(oldGroup.getLastActivity()) || isNewEvent) {
        updateExpression +=
            ", " + UsersManager.GROUPS + ".#groupId." + LAST_ACTIVITY + " = :lastActivity";
        valueMap.withString(":lastActivity", newGroup.getLastActivity());
      }

      if (updatedEventId != null) {
        updateExpression +=
            ", " + UsersManager.GROUPS + ".#groupId." + UsersManager.EVENTS_UNSEEN
                + ".#eventId = :true";
        nameMap.with("#eventId", updatedEventId);
        valueMap.withBoolean(":true", true);
      }

      updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withValueMap(valueMap)
          .withNameMap(nameMap);

      for (final String oldMember : persistingUsernames) {
        try {
          if (!isNewEvent || !oldMember.equals(newEventCreator)) {
            DatabaseManagers.USERS_MANAGER.updateItem(oldMember, updateItemSpec);
          } else { // This must be a new event because boolean logic
            // this means the oldMember is the event creator, we should only update the last activity
            final String updateExpressionEventCreator =
                "set " + UsersManager.GROUPS + ".#groupId." + LAST_ACTIVITY + " = :lastActivity";
            final ValueMap valueMapEventCreator = new ValueMap()
                .withString(":lastActivity", newGroup.getLastActivity());
            final NameMap nameMapEventCreator = new NameMap()
                .with("#groupId", newGroup.getGroupId());
            final UpdateItemSpec updateItemSpecEventCreator = new UpdateItemSpec()
                .withUpdateExpression(updateExpressionEventCreator)
                .withValueMap(valueMapEventCreator)
                .withNameMap(nameMapEventCreator);

            DatabaseManagers.USERS_MANAGER.updateItem(oldMember, updateItemSpecEventCreator);
          }
        } catch (final Exception e) {
          success = false;
          metrics.log(new ErrorDescriptor<>(oldMember, classMethod, e));
        }
      }
    }

    if (addedUsernames.size() > 0) {
      //if this is a new Group we need to create the entire map
      updateExpression = "set " + UsersManager.GROUPS + ".#groupId = :userGroupMap";
      valueMap = new ValueMap().withMap(":userGroupMap", UserGroup.fromNewGroup(newGroup).asMap());

      updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withValueMap(valueMap)
          .withNameMap(nameMap);

      for (final String newMember : addedUsernames) {
        try {
          DatabaseManagers.USERS_MANAGER.updateItem(newMember, updateItemSpec);
        } catch (Exception e) {
          success = false;
          metrics.log(new ErrorDescriptor<>(newMember, classMethod, e));
        }
      }
    }

    //update user objects of all of the users removed - if oldGroup is null, nothing to remove from
    if (oldGroup != null) {
      this.removeUsersFromGroupAndSendNotificationsOnEdit(removedUsernames, oldGroup, metrics);
    }

    //blind send...
    this.sendAddedToGroupNotifications(addedUsernames, newGroup, metrics);

    if (updatedEventId != null) {
      //blind send...
      this.sendEventUpdatedNotification(newMembers, newGroup, updatedEventId, isNewEvent,
          metrics);
    }

    metrics.commonClose(success);
  }

  //This method takes in a list of TentatvieAlgorithmChoices and builds the corresponding
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
   * @param metrics      Standard metrics object for profiling and logging.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus updateEvent(final Group oldGroup, final String eventId,
      final Event updatedEvent, final Boolean isNewEvent, final Metrics metrics) {
    final String classMethod = "GroupsManager.updateEvent";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      final Event oldEvent = oldGroup.getEvents().get(eventId);

      final String lastActivity = LocalDateTime.now(ZoneId.of("UTC"))
          .format(this.getDateTimeFormatter());

      String updateExpression = "set " + LAST_ACTIVITY + " = :currentDate";
      ValueMap valueMap = new ValueMap().withString(":currentDate", lastActivity);
      NameMap nameMap = new NameMap();

      //set all of the update statements
      if (oldEvent.getTentativeAlgorithmChoices().isEmpty()) {
        //if the old event did not have tentative choices set, it must have be getting them
        if (updatedEvent.getTentativeAlgorithmChoices().isEmpty()) {
          throw new Exception("Empty tentative choices must be filled!");
        }

        updateExpression +=
            ", " + EVENTS + ".#eventId." + TENTATIVE_CHOICES + " = :tentativeChoices, "
                + EVENTS + ".#eventId." + VOTING_NUMBERS + " = :votingNumbers";

        nameMap.with("#eventId", eventId);
        valueMap.withMap(":tentativeChoices", updatedEvent.getTentativeAlgorithmChoices())
            .withMap(":votingNumbers",
                this.getVotingNumbersSetup(updatedEvent.getTentativeAlgorithmChoices()));
      }

      if (oldEvent.getSelectedChoice() == null && updatedEvent.getSelectedChoice() != null) {
        updateExpression += ", " + EVENTS + ".#eventId." + SELECTED_CHOICE + " = :selectedChoice";
        nameMap.with("#eventId", eventId);
        valueMap.withString(":selectedChoice", updatedEvent.getSelectedChoice());
      }
      //end setting update statements

      //we're setting the tentative choices and this isn't a new event so remove duplicated choices
      if (valueMap.containsKey(":tentativeChoices") && !isNewEvent) {
        //we need to remove the duplicated category choices
        updateExpression += " remove " + EVENTS + ".#eventId." + Category.CHOICES;
      }

      if (nameMap.containsKey("#eventId")) {
        UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(GROUP_ID, oldGroup.getGroupId())
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap)
            .withValueMap(valueMap);

        this.updateItem(updateItemSpec);

        final Group newGroup = oldGroup.clone();
        newGroup.setLastActivity(lastActivity);
        newGroup.getEvents().put(eventId, updatedEvent);

        this.updateUsersTable(oldGroup, newGroup, eventId, isNewEvent, metrics);

        resultStatus = new ResultStatus(true, "Event updated successfully.");
      } else {
        // this method shouldn't get called if there is nothing to update
        throw new Exception("Nothing to update");
      }
    } catch (final Exception e) {
      metrics.log(new ErrorDescriptor<>(oldGroup.asMap(), classMethod, e));
      resultStatus.resultMessage = "Exception in " + classMethod;
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
