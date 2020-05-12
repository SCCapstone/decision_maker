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

  public static final Integer MAX_DURATION = 10000;
  public static final Integer MAX_EVENT_NAME_LENGTH = 30;
  public static final Integer EVENTS_BATCH_SIZE = 25;

  public GroupsManager() {
    super("groups", "GroupId", Regions.US_EAST_2);
  }

  public GroupsManager(final DynamoDB dynamoDB) {
    super("groups", "GroupId", Regions.US_EAST_2, dynamoDB);
  }

  /**
   * This method is imperative for not overloading the front end with data. Since a group can have
   * an unlimited number of events, we need to limit how many we return at any one time. This is why
   * we're batching events. This function sorts all of the events from the first one to the oldest
   * in the batch, then it flips the sort and gets the top 'n' where 'n' is the number in the
   * batch.
   *
   * @param group       The group we are trying to get a batch of events for.
   * @param batchNumber The index of events to get. Index i gets events (i, (i + 1) * batch size]
   * @return A mapping of eventIds to event objects contained in the requested batch.
   */
  public Map<String, Event> getBatchOfEvents(final Group group, final Integer batchNumber) {
    Integer newestEventIndex = (batchNumber * EVENTS_BATCH_SIZE);
    Integer oldestEventIndex = (batchNumber + 1) * EVENTS_BATCH_SIZE;

    //linked hash maps maintain order whereas normal hash maps do not
    Map<String, Event> eventsBatch = new LinkedHashMap<>();

    //get all of the events from the first event to the oldest event in the batch
    if (group.getEvents().size() > newestEventIndex) {
      //we adjust this so that the .limit(oldestEvent - newestEvent) gets the correct number of items
      if (group.getEvents().size() < oldestEventIndex) {
        oldestEventIndex = group.getEvents().size();
      }

      final LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));

      //first of all we convert the map to be events for sorting before we sort
      //this way we don't have to do all of the sorting setup for every comparison in the sort
      //if setting up sorting params takes n time and sorting takes m times, then doing
      //this first leads to n + m complexity vs n * m complexity if we calculated every comparison
      Map<String, EventForSorting> searchingEventsBatch = group.getEvents()
          .entrySet()
          .stream()
          .collect(collectingAndThen(
              toMap(Entry::getKey, (Map.Entry e) -> new EventForSorting((Event) e.getValue(), now)),
              LinkedHashMap::new));

      //then we sort all of the events up to the oldestEvent being asked for
      eventsBatch = searchingEventsBatch
          .entrySet()
          .stream()
          .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
          .limit(oldestEventIndex)
          .collect(collectingAndThen(toMap(Entry::getKey, (Map.Entry e) -> (Event) e.getValue()),
              LinkedHashMap::new));
    } // else there are no events in this range and we return the empty map

    //then we sort in the opposite direction and get the appropriate number of events for the batch
    final List<String> reverseOrderKeys = new ArrayList<>(eventsBatch.keySet());
    Collections.reverse(reverseOrderKeys);

    Map<String, Event> temp = new HashMap<>();
    for (String eventId : reverseOrderKeys) {
      temp.put(eventId, eventsBatch.get(eventId));

      if (temp.size() >= oldestEventIndex - newestEventIndex) {
        break;
      }
    }

    eventsBatch = temp;

    return eventsBatch;
  }

  public ResultStatus createNewGroup(final Map<String, Object> jsonMap, final Metrics metrics) {

  }

  /**
   * This method handles creating a new event within a group. It validates the input, updates the
   * table and then updating the denormalized user group maps accordingly. Note: if this event is
   * going to skip rsvp, control is passed to the pending events manager is used to handle this
   * flow.
   *
   * @param jsonMap Common request map from endpoint handler containing api input.
   * @param metrics Standard metrics object for profiling and logging.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus newEvent(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "GroupsManager.newEvent";
    metrics.commonSetup(classMethod);
    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, EVENT_NAME, CATEGORY_ID, RSVP_DURATION,
            EVENT_START_DATE_TIME, VOTING_DURATION, GROUP_ID, UTC_EVENT_START_SECONDS);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final Group oldGroup = new Group(this.getMapByPrimaryKey(groupId));

        final String eventId = UUID.randomUUID().toString();
        final String lastActivity = LocalDateTime.now(ZoneId.of("UTC"))
            .format(this.getDateTimeFormatter());
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final User eventCreator = new User(
            DatabaseManagers.USERS_MANAGER.getMapByPrimaryKey(activeUser));

        //we use the 'WithCategoryChoices' variant so that if we need to save the snapshot of the
        //category, then we'll have the data present for that.
        final EventWithCategoryChoices newEvent = new EventWithCategoryChoices(jsonMap);
        newEvent.setEventCreator(ImmutableMap.of(activeUser, eventCreator.asMember()));

        final Optional<String> errorMessage = this.newEventInputIsValid(oldGroup, newEvent);
        if (!errorMessage.isPresent()) {
          //get the category and set category fields
          final Category category = new Category(
              DatabaseManagers.CATEGORIES_MANAGER.getMapByPrimaryKey(newEvent.getCategoryId()));
          newEvent.setCategoryFields(category);

          newEvent.setOptedIn(oldGroup.getMembers());
          newEvent.setCreatedDateTime(lastActivity);
          newEvent.setSelectedChoice(null);
          newEvent.setTentativeAlgorithmChoices(Collections.emptyMap());
          newEvent.setVotingNumbers(Collections.emptyMap());

          String updateExpression =
              "set " + EVENTS + ".#eventId = :map, " + LAST_ACTIVITY + " = :lastActivity";
          NameMap nameMap = new NameMap().with("#eventId", eventId);
          ValueMap valueMap = new ValueMap()
              .withString(":lastActivity", lastActivity);

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

          this.updateItem(groupId, updateItemSpec);

          //Hope it works, we aren't using transactions yet (that's why nothing done with result).
          if (newEvent.getRsvpDuration() > 0) {
            final ResultStatus pendingEventAdded = DatabaseManagers.PENDING_EVENTS_MANAGER
                .addPendingEvent(groupId, eventId, newEvent.getRsvpDuration(), metrics);
          } else {
            //this will set potential algo choices and create the entry for voting duration timeout
            final Map<String, Object> processPendingEventInput = ImmutableMap.of(
                GROUP_ID, groupId,
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
          metrics.log(new WarningDescriptor<>(jsonMap, classMethod, errorMessage.get()));
          resultStatus.resultMessage = errorMessage.get();
        }
      } catch (Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus.resultMessage = "Error: Unable to parse request in manager.";
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found"));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }
    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This method inserts a user back into a group they had previously left.
   *
   * @param jsonMap The map containing the json request sent from the front end. Must contain the
   *                GroupId for the group the user is attempting to rejoin.
   * @param metrics Standard metrics object for profiling and logging
   */
  public ResultStatus rejoinGroup(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "GroupsManager.rejoinGroup";
    metrics.commonSetup(classMethod);
    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays.asList(GROUP_ID, RequestFields.ACTIVE_USER);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        final Group group = new Group(this.getMapByPrimaryKey(groupId));
        final User user = new User(DatabaseManagers.USERS_MANAGER.getMapByPrimaryKey(activeUser));

        final Set<String> groupsLeftIds = user.getGroupsLeft().keySet();
        final Set<String> membersLeftIds = group.getMembersLeft().keySet();
        if (groupsLeftIds.contains(groupId) && membersLeftIds.contains(activeUser)) {
          String updateExpression =
              "remove " + MEMBERS_LEFT + ".#username set " + MEMBERS + ".#username = :memberMap";

          NameMap nameMap = new NameMap()
              .with("#username", activeUser);
          ValueMap valueMap = new ValueMap()
              .withMap(":memberMap", user.asMember().asMap());

          UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withUpdateExpression(updateExpression)
              .withNameMap(nameMap)
              .withValueMap(valueMap);

          this.updateItem(groupId, updateItemSpec);

          // remove this group from the GroupsLeft attribute in active user object
          // and add it to the Groups attribute
          updateExpression =
              "remove " + UsersManager.GROUPS_LEFT + ".#groupId set " + UsersManager.GROUPS
                  + ".#groupId = :groupMap";
          nameMap = new NameMap()
              .with("#groupId", groupId);
          valueMap = new ValueMap()
              .withMap(":groupMap", UserGroup.fromNewGroup(group).asMap());
          updateItemSpec = new UpdateItemSpec()
              .withUpdateExpression(updateExpression)
              .withNameMap(nameMap)
              .withValueMap(valueMap);
          DatabaseManagers.USERS_MANAGER.updateItem(activeUser, updateItemSpec);

          resultStatus = new ResultStatus(true, "Group rejoined successfully.");
        } else {
          metrics.log(
              new ErrorDescriptor<>(jsonMap, classMethod,
                  "User did not leave group, cannot rejoin"));
          resultStatus.resultMessage = "Error: User did not leave this group, cannot rejoin.";
        }
      } catch (Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus.resultMessage = "Error: Unable to parse request in manager.";
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Request keys not found"));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This method allows a user to vote for a tentative choice on an event.
   *
   * @param jsonMap Common request map from endpoint handler containing api input.
   * @param metrics Standard metrics object for profiling and logging.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus voteForChoice(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "GroupsManager.voteForChoice";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(GROUP_ID, RequestFields.EVENT_ID, RequestFields.CHOICE_ID, RequestFields.VOTE_VALUE,
            RequestFields.ACTIVE_USER);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final String eventId = (String) jsonMap.get(RequestFields.EVENT_ID);
        final String choiceId = (String) jsonMap.get(RequestFields.CHOICE_ID);
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        Integer voteValue = (Integer) jsonMap.get(RequestFields.VOTE_VALUE);

        final User user = new User(DatabaseManagers.USERS_MANAGER.getMapByPrimaryKey(activeUser));

        //only allow the user to vote if they are in the group
        if (user.getGroups().containsKey(groupId)) {
          if (voteValue != 1) {
            voteValue = 0;
          }

          final String updateExpression =
              "set " + EVENTS + ".#eventId." + VOTING_NUMBERS + ".#choiceId." +
                  "#activeUser = :voteValue";
          final ValueMap valueMap = new ValueMap().withInt(":voteValue", voteValue);
          final NameMap nameMap = new NameMap()
              .with("#eventId", eventId)
              .with("#choiceId", choiceId)
              .with("#activeUser", activeUser);

          final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withUpdateExpression(updateExpression)
              .withNameMap(nameMap)
              .withValueMap(valueMap);

          this.updateItem(groupId, updateItemSpec);
          resultStatus = new ResultStatus(true, "Voted yes/no successfully!");
        } else {
          resultStatus.resultMessage = "Error: user not in group.";
          metrics.log(new WarningDescriptor<>(jsonMap, classMethod, "User not in group."));
        }
      } catch (Exception e) {
        resultStatus.resultMessage = "Error: unable to parse request in manager.";
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
      }
    } else {
      resultStatus.resultMessage = "Error: required request keys not found.";
      metrics.log(
          new ErrorDescriptor<>(jsonMap, classMethod, "Error: required request keys not found."));
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
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
        updateExpression += " remove " + EVENTS + ".#eventId." + CategoriesManager.CHOICES;
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

  /**
   * This method gets all of the category ids on a group.
   *
   * @param groupId The group id to be pulled.
   * @param metrics Standard metrics object for profiling and logging.
   * @return This list of categoryIds or any empty list if there was an exception.
   */
  public List<String> getAllCategoryIds(final String groupId, final Metrics metrics) {
    final String classMethod = "GroupsManager.getAllCategoryIds";
    metrics.commonSetup(classMethod);

    ArrayList<String> categoryIds = new ArrayList<>();
    boolean success = false;
    try {
      final Group group = new Group(this.getMapByPrimaryKey(groupId));
      categoryIds = new ArrayList<>(group.getCategories().keySet());
      success = true;
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>(groupId, classMethod, e));
    }

    metrics.commonClose(success);
    return categoryIds;
  }

  /**
   * This function takes in a group and batch number and gets the appropriate map of events for the
   * group based on the batch number. This method is used in the 'infinitely' scrolling list of
   * events on a group's page. Using this, we continue to get the next set or 'batch' of events.
   *
   * @param jsonMap Common request map from endpoint handler containing api input
   * @param metrics Standard metrics object for profiling and logging
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handleGetBatchOfEvents(final Map<String, Object> jsonMap,
      final Metrics metrics) {
    final String classMethod = "GroupsManager.handleGetBatchOfEvents";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, GroupsManager.GROUP_ID, RequestFields.BATCH_NUMBER);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final Integer batchNumber = (Integer) jsonMap.get(RequestFields.BATCH_NUMBER);

        final Group group = new Group(this.getMapByPrimaryKey(groupId));

        //the user should not be able to retrieve info from the group if they are not a member
        if (group.getMembers().containsKey(activeUser)) {
          //we set the events on the group so we can use the group's getEventsMap method
          group.setEvents(this.getBatchOfEvents(group, batchNumber));

          resultStatus = new ResultStatus(true,
              JsonUtils.convertObjectToJson(group.getEventsMap()));
        } else {
          resultStatus.resultMessage = "Error: user is not a member of the group.";
        }
      } catch (final Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus.resultMessage = "Exception inside of: " + classMethod;
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found."));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
