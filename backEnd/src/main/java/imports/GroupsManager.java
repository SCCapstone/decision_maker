package imports;

import static java.util.stream.Collectors.toMap;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.util.StringUtils;
import com.google.common.collect.ImmutableMap;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import models.Event;
import models.EventForSorting;
import models.Group;
import models.Metadata;
import models.User;
import models.UserGroup;
import utilities.ErrorDescriptor;
import utilities.JsonEncoders;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

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
  public static final Integer EVENTS_BATCH_SIZE = 25;

  public GroupsManager() {
    super("groups", "GroupId", Regions.US_EAST_2);
  }

  public GroupsManager(final DynamoDB dynamoDB) {
    super("groups", "GroupId", Regions.US_EAST_2, dynamoDB);
  }

  public ResultStatus getGroup(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "GroupsManager.getGroups";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, GROUP_ID, RequestFields.BATCH_NUMBER);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final Integer batchNumber = (Integer) jsonMap.get(RequestFields.BATCH_NUMBER);

        final Group group = new Group(this.getMapByPrimaryKey(groupId));

        //the user should not be able to retrieve info from the group if they are not a member
        if (group.getMembers().containsKey(activeUser)) {
          group.setEvents(this.getBatchOfEvents(group, batchNumber));

          resultStatus = new ResultStatus(true,
              JsonEncoders.convertObjectToJson(group.asMap()));
        } else {
          resultStatus.resultMessage = "Error: user is not a member of the group.";
        }
      } catch (final Exception e) {
        resultStatus.resultMessage = "Error: Unable to parse request.";
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found"));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private Map<String, Event> getBatchOfEvents(final Group group, final Integer batchNumber) {
    Integer newestEventIndex = (batchNumber * EVENTS_BATCH_SIZE);
    Integer oldestEventIndex = (batchNumber + 1) * EVENTS_BATCH_SIZE;

    //linked hash maps maintain order whereas normal hash maps do not
    Map<String, Event> eventsBatch = new LinkedHashMap<>();

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
          .collect(toMap(
              Entry::getKey,
              (e) -> new EventForSorting(e.getValue(), now),
              (e1, e2) -> e2,
              LinkedHashMap::new));

      //then we sort all of the events up to the oldestEvent being asked for
      eventsBatch = searchingEventsBatch
          .entrySet()
          .stream()
          .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
          .limit(oldestEventIndex)
          .collect(toMap(Entry::getKey, (e) -> (Event) e.getValue(), (e1, e2) -> e2,
              LinkedHashMap::new));

      //then we sort in the opposite direction and get the appropriate number of events
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
    } // else there are no events in this range and we return the empty map

    return eventsBatch;
  }

  public ResultStatus createNewGroup(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "GroupsManager.createNewGroup";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, GROUP_NAME, MEMBERS, CATEGORIES,
            DEFAULT_VOTING_DURATION, DEFAULT_RSVP_DURATION);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        if (jsonMap.containsKey(ICON)) { // if it's there, assume it's new image data
          final String newIconFileName = DatabaseManagers.S3_ACCESS_MANAGER
              .uploadImage((List<Integer>) jsonMap.get(ICON), metrics)
              .orElseThrow(Exception::new);

          jsonMap.put(ICON, newIconFileName); // put overwrites current value
        }

        final List<String> members = (List<String>) jsonMap.get(MEMBERS);
        //sanity check, add the active user to this mapping to make sure his data is added
        members.add(activeUser);

        final Map<String, Object> membersMapped = this.getMembersMapForInsertion(members, metrics);
        jsonMap.put(MEMBERS, membersMapped); // put overwrites current value

        //TODO update the categories passed in to be a list of ids, then create categories map
        //TODO similar to what we're doing with the members above (currently we're just relying on
        //TODO user input which is bad

        final String newGroupId = UUID.randomUUID().toString();
        final String lastActivity = LocalDateTime.now(ZoneId.of("UTC"))
            .format(this.getDateTimeFormatter());

        final Group newGroup = new Group(jsonMap);
        newGroup.setGroupId(newGroupId);
        newGroup.setGroupCreator(activeUser);
        newGroup.setOpen(false); // TODO get from 'required' request key (it's not required rn)
        newGroup.setMembersLeft(Collections.emptyMap());
        newGroup.setEvents(Collections.emptyMap());
        newGroup.setLastActivity(lastActivity);
        this.putItem(newGroup);

        //old group being null signals we're creating a new group
        //updatedEventId being null signals this isn't an event update
        this.updateUsersTable(null, newGroup, null, false, metrics);
        this.updateCategoriesTable(Collections.emptyMap(), newGroup.getCategories(), newGroupId, "",
            newGroup.getGroupName());

        resultStatus = new ResultStatus(true, JsonEncoders.convertObjectToJson(newGroup.asMap()));
      } catch (Exception e) {
        resultStatus.resultMessage = "Error: Unable to parse request.";
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found"));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }
    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus editGroup(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "GroupsManager.editGroup";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, GROUP_ID, GROUP_NAME, MEMBERS, CATEGORIES,
            DEFAULT_VOTING_DURATION, DEFAULT_RSVP_DURATION);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final String groupName = (String) jsonMap.get(GROUP_NAME);
        final Optional<List<Integer>> newIcon = Optional
            .ofNullable((List<Integer>) jsonMap.get(ICON));
        final Map<String, Object> categories = (Map<String, Object>) jsonMap.get(CATEGORIES);
        final Integer defaultVotingDuration = (Integer) jsonMap.get(DEFAULT_VOTING_DURATION);
        final Integer defaultRsvpDuration = (Integer) jsonMap.get(DEFAULT_RSVP_DURATION);
        List<String> members = (List<String>) jsonMap.get(MEMBERS);

        //TODO update the categories passed in to be a list of ids, then create categories map
        //TODO similar to what we're doing with the members above (currently we're just relying on
        //TODO user input which is bad

        final Group oldGroup = new Group(this.getItemByPrimaryKey(groupId).asMap());

        if (this.editGroupInputIsValid(groupId, activeUser, oldGroup, members,
            defaultVotingDuration, defaultRsvpDuration)) {

          if (this.editInputHasPermissions(oldGroup, activeUser)) {
            //all validation is successful, build transaction actions
            final Map<String, Object> membersMapped = this
                .getMembersMapForInsertion(members, metrics);

            String updateExpression =
                "set " + GROUP_NAME + " = :name, " + MEMBERS + " = :members, " + CATEGORIES
                    + " = :categories, " + DEFAULT_VOTING_DURATION + " = :defaultVotingDuration, "
                    + DEFAULT_RSVP_DURATION + " = :defaultRsvpDuration";
            ValueMap valueMap = new ValueMap()
                .withString(":name", groupName)
                .withMap(":members", membersMapped)
                .withMap(":categories", categories)
                .withInt(":defaultVotingDuration", defaultVotingDuration)
                .withInt(":defaultRsvpDuration", defaultRsvpDuration);

            //assumption - currently we aren't allowing user's to clear a group's image once set
            String newIconFileName = null;
            if (newIcon.isPresent()) {
              newIconFileName = DatabaseManagers.S3_ACCESS_MANAGER
                  .uploadImage(newIcon.get(), metrics).orElseThrow(Exception::new);

              updateExpression += ", " + ICON + " = :icon";
              valueMap.withString(":icon", newIconFileName);
            }

            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(this.getPrimaryKeyIndex(), groupId)
                .withUpdateExpression(updateExpression)
                .withValueMap(valueMap);

            this.updateItem(updateItemSpec);

            //update mappings in users and categories tables
            final Group newGroup = new Group(this.getMapByPrimaryKey(groupId));
            this.updateUsersTable(oldGroup, newGroup, null, false, metrics);
            this.updateCategoriesTable(oldGroup.getCategories(), newGroup.getCategories(), groupId,
                oldGroup.getGroupName(), groupName);

            resultStatus = new ResultStatus(true,
                JsonEncoders.convertObjectToJson(newGroup.asMap()));
          } else {
            resultStatus.resultMessage = "Invalid request, missing permissions";
          }
        } else {
          resultStatus.resultMessage = "Invalid request, bad input.";
        }
      } catch (Exception e) {
        resultStatus.resultMessage = "Error: Unable to parse request in manager";
        metrics.log(
            new ErrorDescriptor<>(jsonMap, classMethod, e));
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod,
          "Required request keys not found"));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }
    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus deleteGroup(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "GroupsManager.deleteGroup";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(GROUP_ID, RequestFields.ACTIVE_USER);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        final Group group = new Group(this.getItemByPrimaryKey(groupId).asMap());
        if (activeUser.equals(group.getGroupCreator())) {
          Set<String> members = group.getMembers().keySet();
          Set<String> membersLeft = group.getMembersLeft().keySet();
          Set<String> categoryIds = group.getCategories().keySet();

          // Remove the group from the users and categories tables
          ResultStatus removeFromUsersResult = DatabaseManagers.USERS_MANAGER
              .removeGroupFromUsers(members, membersLeft, groupId, metrics);
          ResultStatus removeFromCategoriesResult = DatabaseManagers.CATEGORIES_MANAGER
              .removeGroupFromCategories(categoryIds, groupId, metrics);

          if (removeFromUsersResult.success && removeFromCategoriesResult.success) {
            DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
                .withPrimaryKey(this.getPrimaryKeyIndex(), groupId);

            this.deleteItem(deleteItemSpec);

            resultStatus = new ResultStatus(true, "Group deleted successfully!");
          } else {
            resultStatus = removeFromUsersResult.applyResultStatus(removeFromCategoriesResult);
            metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, resultStatus.resultMessage));
          }

        } else {
          metrics.log(
              new ErrorDescriptor<>(jsonMap, classMethod,
                  "User is not the owner of the group"));
          resultStatus.resultMessage = "Error: User is not the owner of the group.";
        }
      } catch (Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus.resultMessage = "Error: Unable to parse request in manager.";
      }
    } else {
      metrics
          .log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found"));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }
    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus newEvent(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "GroupsManager.newEvent";
    metrics.commonSetup(classMethod);
    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, EVENT_NAME, CATEGORY_ID, CATEGORY_NAME, RSVP_DURATION,
            EVENT_START_DATE_TIME, VOTING_DURATION, GROUP_ID, UTC_EVENT_START_SECONDS);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final Group oldGroup = new Group(this.getItemByPrimaryKey(groupId).asMap());
        final String eventId = UUID.randomUUID().toString();
        final String lastActivity = LocalDateTime.now(ZoneId.of("UTC"))
            .format(this.getDateTimeFormatter());
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final User eventCreator = new User(
            DatabaseManagers.USERS_MANAGER.getItemByPrimaryKey(activeUser).asMap());

        final Event newEvent = new Event(jsonMap);

        if (this.validEventInput(oldGroup, newEvent)) {
          newEvent.setEventCreator(ImmutableMap.of(activeUser, eventCreator.asMember()));
          newEvent.setOptedIn(oldGroup.getMembers());
          newEvent.setCreatedDateTime(lastActivity);
          newEvent.setSelectedChoice(null);
          newEvent.setTentativeAlgorithmChoices(Collections.emptyMap());
          newEvent.setVotingNumbers(Collections.emptyMap());

          String updateExpression =
              "set " + EVENTS + ".#eventId = :map, " + LAST_ACTIVITY + " = :lastActivity";
          NameMap nameMap = new NameMap().with("#eventId", eventId);
          ValueMap valueMap = new ValueMap()
              .withMap(":map", newEvent.asMap())
              .withString(":lastActivity", lastActivity);

          UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withPrimaryKey(this.getPrimaryKeyIndex(), groupId)
              .withNameMap(nameMap)
              .withUpdateExpression(updateExpression)
              .withValueMap(valueMap);

          this.updateItem(updateItemSpec);

          //Hope it works, we aren't using transactions yet (that's why I'm not doing anything with result.
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

          final Group newGroup = new Group(this.getItemByPrimaryKey(groupId).asMap());

          //when rsvp is not greater than 0, updateUsersTable gets called by setEventTentativeChoices
          if (newEvent.getRsvpDuration() > 0) {
            this.updateUsersTable(oldGroup, newGroup, eventId, true, metrics);
          }

          resultStatus = new ResultStatus(true, JsonEncoders.convertObjectToJson(newGroup.asMap()));
        } else {
          metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Invalid request, bad input"));
          resultStatus.resultMessage = "Invalid request, bad input.";
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

  public ResultStatus optInOutOfEvent(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "GroupsManager.optInOutOfEvent";
    metrics.commonSetup(classMethod);
    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(GROUP_ID, RequestFields.PARTICIPATING, RequestFields.EVENT_ID,
            RequestFields.ACTIVE_USER);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final Boolean participating = (Boolean) jsonMap.get(RequestFields.PARTICIPATING);
        final String eventId = (String) jsonMap.get(RequestFields.EVENT_ID);
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final User user = new User(
            DatabaseManagers.USERS_MANAGER.getItemByPrimaryKey(activeUser).asMap());

        String updateExpression;
        ValueMap valueMap = null;

        if (participating) { // add the user to the optIn
          updateExpression =
              "set " + EVENTS + ".#eventId." + OPTED_IN + ".#username = :userMap";
          valueMap = new ValueMap()
              .withMap(":userMap", user.asMember().asMap());
        } else {
          updateExpression = "remove " + EVENTS + ".#eventId." + OPTED_IN + ".#username";
        }

        final NameMap nameMap = new NameMap()
            .with("#eventId", eventId)
            .with("#username", activeUser);

        UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(this.getPrimaryKeyIndex(), groupId)
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap)
            .withValueMap(valueMap);

        this.updateItem(updateItemSpec);

        final Group group = new Group(this.getItemByPrimaryKey(groupId).asMap());

        resultStatus = new ResultStatus(true, JsonEncoders.convertObjectToJson(group.asMap()));
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

  //TODO revisit this all together - was never implemented on the front end maybe?
  public ResultStatus leaveGroup(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "GroupsManager.leaveGroup";
    metrics.commonSetup(classMethod);
    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays.asList(GROUP_ID, RequestFields.ACTIVE_USER);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        final Group group = new Group(this.getItemByPrimaryKey(groupId).asMap());
        final String groupCreator = group.getGroupCreator();

        if (!groupCreator.equals(activeUser)) {
          String updateExpression =
              "remove " + MEMBERS + ".#username set " + MEMBERS_LEFT
                  + ".#username = :memberLeftMap";

          NameMap nameMap = new NameMap()
              .with("#username", activeUser);
          ValueMap valueMap = new ValueMap()
              .withBoolean(":memberLeftMap", true);

          UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withPrimaryKey(this.getPrimaryKeyIndex(), groupId)
              .withUpdateExpression(updateExpression)
              .withNameMap(nameMap)
              .withValueMap(valueMap);

          this.updateItem(updateItemSpec);

          // remove this group from the Groups attribute in active user object
          // and add it to the GroupsLeft attribute
          updateExpression =
              "remove " + UsersManager.GROUPS + ".#groupId set " + UsersManager.GROUPS_LEFT
                  + ".#groupId = :groupMap";
          nameMap = new NameMap()
              .with("#groupId", groupId);
          valueMap = new ValueMap()
              .withMap(":groupMap", new HashMap<String, Object>() {{
                put(GROUP_NAME, group.getGroupName());
                put(ICON, group.getIcon());
              }});
          updateItemSpec = new UpdateItemSpec()
              .withUpdateExpression(updateExpression)
              .withValueMap(valueMap)
              .withNameMap(nameMap)
              .withPrimaryKey(DatabaseManagers.USERS_MANAGER.getPrimaryKeyIndex(), activeUser);
          DatabaseManagers.USERS_MANAGER.updateItem(updateItemSpec);

          resultStatus = new ResultStatus(true, "Group left successfully.");
        } else {
          metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Owner cannot leave group"));
          resultStatus.resultMessage = "Error: Owner cannot leave group.";
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

        final Group group = new Group(this.getItemByPrimaryKey(groupId).asMap());
        final User user = new User(
            DatabaseManagers.USERS_MANAGER.getItemByPrimaryKey(activeUser).asMap());

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
              .withPrimaryKey(this.getPrimaryKeyIndex(), groupId)
              .withUpdateExpression(updateExpression)
              .withNameMap(nameMap)
              .withValueMap(valueMap);

          this.updateItem(updateItemSpec);

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
              .withValueMap(valueMap)
              .withPrimaryKey(DatabaseManagers.USERS_MANAGER.getPrimaryKeyIndex(), activeUser);
          DatabaseManagers.USERS_MANAGER.updateItem(updateItemSpec);

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

        if (voteValue != 1) {
          voteValue = 0;
        }

        String updateExpression =
            "set " + EVENTS + ".#eventId." + VOTING_NUMBERS + ".#choiceId." +
                "#activeUser = :voteValue";
        ValueMap valueMap = new ValueMap().withInt(":voteValue", voteValue);

        final NameMap nameMap = new NameMap()
            .with("#eventId", eventId)
            .with("#choiceId", choiceId)
            .with("#activeUser", activeUser);

        UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(this.getPrimaryKeyIndex(), groupId)
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap)
            .withValueMap(valueMap);

        this.updateItem(updateItemSpec);
        resultStatus = new ResultStatus(true, "Voted yes/no successfully!");
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

  private Map<String, Object> getMembersMapForInsertion(final List<String> members,
      final Metrics metrics) {
    final String classMethod = "GroupsManager.getMembersMapForInsertion";
    metrics.commonSetup(classMethod);
    boolean success = true;

    final Map<String, Object> membersMap = new HashMap<>();

    for (String username : members) {
      try {
        //get user's display name
        final User user = new User(
            DatabaseManagers.USERS_MANAGER.getItemByPrimaryKey(username).asMap());

        membersMap.putIfAbsent(username, user.asMember().asMap());
      } catch (Exception e) {
        success = false; // this may give false alarms as users may just have put in bad usernames
        metrics.log(new ErrorDescriptor<>(username, classMethod, e));
      }
    }

    metrics.commonClose(success);
    return membersMap;
  }

  private boolean editGroupInputIsValid(final String groupId, final String activeUser,
      final Group oldGroup, final List<String> members, final Integer defaultVotingDuration,
      final Integer defaultRsvpDuration) {
    final String groupCreator = oldGroup.getGroupCreator();
    final Set<String> membersLeft = oldGroup.getMembersLeft().keySet();
    boolean isValid = true;

    if (StringUtils.isNullOrEmpty(groupId) || StringUtils.isNullOrEmpty(activeUser)) {
      isValid = false;
    }

    boolean creatorInGroup = false;
    //you can't remove the creator (owner) from the group
    //you can't add someone who has left the group
    for (String username : members) {
      if (groupCreator.equals(username)) {
        creatorInGroup = true;
      }
      if (membersLeft.contains(username)) {
        isValid = false;
        break;
      }
    }

    isValid = isValid && creatorInGroup;

    if (defaultVotingDuration < 0 || defaultVotingDuration > MAX_DURATION) {
      isValid = false;
    }

    if (defaultRsvpDuration < 0 || defaultRsvpDuration > MAX_DURATION) {
      isValid = false;
    }

    return isValid;
  }

  private boolean validEventInput(final Group oldGroup, final Event newEvent) {
    boolean isValid = true;
    //TODO - make this make sense - or maybe put thrown exceptions in the setting of the model attributes!
//    if (StringUtils.isNullOrEmpty(groupId) || StringUtils.isNullOrEmpty(categoryId)) {
//      isValid = false;
//    }
//
//    if (votingDuration <= 0 || votingDuration > MAX_DURATION) {
//      isValid = false;
//    }
//
//    if (rsvpDuration < 0 || rsvpDuration > MAX_DURATION) {
//      isValid = false;
//    }
    return isValid;
  }

  private boolean editInputHasPermissions(final Group oldGroup, final String activeUser) {
    //the group creator is not changed or it is changed and the active user is the current creator
    boolean hasPermission = true;

    //the below if is always false because of how groupCreator is getting set in edit group
    //for testing lets let all users be able to edit the group and we can touch base on this after beta
//    if (!dbGroupDataMap.get(GROUP_CREATOR).equals(groupCreator) && !dbGroupDataMap
//        .get(GROUP_CREATOR).equals(activeUser)) {
//      hasPermission = false;
//    }

    return hasPermission;
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
        final User user = new User(
            DatabaseManagers.USERS_MANAGER.getItemByPrimaryKey(username).asMap());

        if (!username.equals(addedTo.getGroupCreator())) {
          //Note: no need to check user's group muted settings since they're just being added
          if (user.pushEndpointArnIsSet() && !user.getAppSettings().isMuted()) {
            DatabaseManagers.SNS_ACCESS_MANAGER.sendMessage(user.getPushEndpointArn(),
                "Added to new group!", addedTo.getGroupName(), addedTo.getGroupId(), metadata);
          }
        }
      } catch (Exception e) {
        success = false;
        metrics.log(new ErrorDescriptor<>(username, classMethod, e));
      }
    }

    metrics.commonClose(success);
  }

  private void sendRemovedFromGroupNotifications(final Set<String> usernames,
      final Group removedFrom, final Metrics metrics) {
    final String classMethod = "GroupsManager.sendRemovedFromGroupNotifications";
    metrics.commonSetup(classMethod);

    boolean success = true;

    final Metadata metadata = new Metadata("removedFromGroup",
        ImmutableMap.of(GROUP_ID, removedFrom.getGroupId()));

    for (String username : usernames) {
      try {
        final User user = new User(
            DatabaseManagers.USERS_MANAGER.getItemByPrimaryKey(username).asMap());

        //Note: no need to check user's group muted settings since they're just being added
        if (user.pushEndpointArnIsSet() && !user.getAppSettings().isMuted()) {
          DatabaseManagers.SNS_ACCESS_MANAGER.sendMessage(user.getPushEndpointArn(),
              "Removed from group", removedFrom.getGroupName(), removedFrom.getGroupId(), metadata);
        }
      } catch (Exception e) {
        success = false;
        metrics.log(new ErrorDescriptor<>(username, classMethod, e));
      }
    }

    metrics.commonClose(success);
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
    payload.putIfAbsent(RequestFields.EVENT_ID, eventId);

    String action = "eventCreated";

    String eventChangeTitle = "Event in " + group.getGroupName();

    //assume the event just got created
    String eventChangeBody =
        "'" + updatedEvent.getEventName() + "' created by: " + updatedEvent
            .getEventCreatorDisplayName();

    if (updatedEvent.getSelectedChoice() != null) {
      //we just transitioned to a having a selected choice -> occurring
      action = "eventChosen";
      eventChangeBody =
          updatedEvent.getEventName() + ": " + updatedEvent.getSelectedChoice() + " Won!";
    } else if (!updatedEvent.getTentativeAlgorithmChoices().isEmpty()) {
      //we just transitioned to getting tentative choices -> we need to vote
      action = "eventVoting";
      eventChangeBody = "Vote for " + updatedEvent.getEventName();
    } // else the event was indeed just created

    final Metadata metadata = new Metadata(action, payload);

    for (String username : usernames) {
      if (!(isNewEvent && username.equals(updatedEventCreator))) {
        try {
          final User user = new User(
              DatabaseManagers.USERS_MANAGER.getItemByPrimaryKey(username).asMap());

          if (user.pushEndpointArnIsSet() && !user.getAppSettings().isMuted() && !user.getGroups()
              .get(group.getGroupId()).isMuted()) {
            DatabaseManagers.SNS_ACCESS_MANAGER
                .sendMessage(user.getPushEndpointArn(), eventChangeTitle, eventChangeBody, eventId,
                    metadata);
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
    if (oldGroup != null && persistingUsernames.size() > 0) {
      //since this group already exists, we're just updating the mappings that have changed for existing users
      //for simplicity in the code, we'll always update the group name
      updateExpression = "set " + UsersManager.GROUPS + ".#groupId." + GROUP_NAME + " = :groupName";
      valueMap = new ValueMap().withString(":groupName", newGroup.getGroupName());

      if (newGroup.iconIsSet() && !newGroup.getIcon().equals(oldGroup.getIcon())) {
        updateExpression += ", " + UsersManager.GROUPS + ".#groupId." + ICON + " = :groupIcon";
        valueMap.withString(":groupIcon", newGroup.getIcon());
      }

      if (newGroup.lastActivityIsSet() && !newGroup.getLastActivity()
          .equals(oldGroup.getLastActivity())) {
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
        if (!(isNewEvent && oldMember.equals(newEventCreator))) {
          try {
            updateItemSpec
                .withPrimaryKey(DatabaseManagers.USERS_MANAGER.getPrimaryKeyIndex(), oldMember);
            DatabaseManagers.USERS_MANAGER.updateItem(updateItemSpec);
          } catch (Exception e) {
            success = false;
            metrics.log(new ErrorDescriptor<>(oldMember, classMethod, e));
          }
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
          updateItemSpec
              .withPrimaryKey(DatabaseManagers.USERS_MANAGER.getPrimaryKeyIndex(), newMember);
          DatabaseManagers.USERS_MANAGER.updateItem(updateItemSpec);
        } catch (Exception e) {
          success = false;
          metrics.log(new ErrorDescriptor<>(newMember, classMethod, e));
        }
      }
    }

    //update user objects of all of the users removed
    if (!removedUsernames.isEmpty()) {
      updateExpression = "remove Groups.#groupId";
      updateItemSpec = new UpdateItemSpec()
          .withNameMap(nameMap)
          .withUpdateExpression(updateExpression);

      for (final String member : removedUsernames) {
        try {
          updateItemSpec
              .withPrimaryKey(DatabaseManagers.USERS_MANAGER.getPrimaryKeyIndex(), member);
          DatabaseManagers.USERS_MANAGER.updateItem(updateItemSpec);
        } catch (Exception e) {
          success = false;
          metrics.log(new ErrorDescriptor<>(member, classMethod, e));
        }
      }
    }

    try {
      //blind send...
      this.sendAddedToGroupNotifications(addedUsernames, newGroup, metrics);
    } catch (final Exception e) {
      success = false;
      metrics.log(new ErrorDescriptor<>(addedUsernames, classMethod, e));
    }

    if (oldGroup != null) { // users can only be removed from the old group
      try {
        //blind send...
        this.sendRemovedFromGroupNotifications(removedUsernames, oldGroup, metrics);
      } catch (final Exception e) {
        success = false;
        metrics.log(new ErrorDescriptor<>(addedUsernames, classMethod, e));
      }
    }

    if (updatedEventId != null) {
      try {
        this.sendEventUpdatedNotification(newMembers, newGroup, updatedEventId, isNewEvent,
            metrics);
      } catch (Exception e) {
        success = false;
        metrics.log(new ErrorDescriptor<>(newMembers, classMethod, e));
      }
    }

    metrics.commonClose(success);
  }

  private void updateCategoriesTable(final Map<String, String> oldCategories,
      final Map<String, String> newCategories, final String groupId, final String oldGroupName,
      final String newGroupName) {
    final Set<String> categoriesToUpdate = new HashSet<>();
    String updateExpression;
    NameMap nameMap = new NameMap().with("#groupId", groupId);
    ValueMap valueMap = new ValueMap().withString(":groupName", newGroupName);
    UpdateItemSpec updateItemSpec = new UpdateItemSpec().withNameMap(nameMap);

    if (!oldCategories.keySet().equals(newCategories.keySet())) {
      // Make copies of the key sets and use removeAll to figure out where they differ
      final Set<String> newCategoryIds = new HashSet<>(newCategories.keySet());
      final Set<String> removedCategoryIds = new HashSet<>(oldCategories.keySet());

      // Note: using removeAll on a HashSet has linear time complexity when
      // another HashSet is passed in
      newCategoryIds.removeAll(oldCategories.keySet());
      removedCategoryIds.removeAll((newCategories.keySet()));

      if (newGroupName.equals(oldGroupName) && !newCategoryIds.isEmpty()) {
        // If the group name wasn't changed and we're adding new categories, then only perform
        // updates for the newly added categories
        categoriesToUpdate.addAll(newCategoryIds);
      } else if (!newGroupName.equals(oldGroupName)) {
        // If the group name was changed, update every category in newCategories to reflect that.
        // In this case, both the list of categories and the group name were changed.
        categoriesToUpdate.addAll(newCategories.keySet());
      }

      if (!removedCategoryIds.isEmpty()) {
        updateExpression = "remove Groups.#groupId";
        updateItemSpec.withUpdateExpression(updateExpression);
        for (final String categoryId : removedCategoryIds) {
          updateItemSpec
              .withPrimaryKey(DatabaseManagers.CATEGORIES_MANAGER.getPrimaryKeyIndex(), categoryId);
          DatabaseManagers.CATEGORIES_MANAGER.updateItem(updateItemSpec);
        }
      }
    } else if (!newGroupName.equals(oldGroupName)) {
      // If the group name was changed, update every category in newCategories to reflect that.
      // In this case, the list of categories wasn't changed, but the group name was.
      categoriesToUpdate.addAll(newCategories.keySet());
    }

    if (!categoriesToUpdate.isEmpty()) {
      updateExpression = "set Groups.#groupId = :groupName";
      updateItemSpec.withUpdateExpression(updateExpression).withValueMap(valueMap);
      for (final String categoryId : categoriesToUpdate) {
        updateItemSpec
            .withPrimaryKey(DatabaseManagers.CATEGORIES_MANAGER.getPrimaryKeyIndex(), categoryId);
        DatabaseManagers.CATEGORIES_MANAGER.updateItem(updateItemSpec);
      }
    }
  }

  private Map<String, Map> getVotingNumbersSetup(
      final Map<String, String> tentativeAlgorithmChoices) {
    final Map<String, Map> votingNumbers = new HashMap<>();

    //we're filling a map keyed by choiceId with empty maps
    for (String choiceId : tentativeAlgorithmChoices.keySet()) {
      votingNumbers.put(choiceId, ImmutableMap.of());
    }

    return votingNumbers;
  }

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

      if (oldEvent.getTentativeAlgorithmChoices().isEmpty() && !updatedEvent
          .getTentativeAlgorithmChoices().isEmpty()) {
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

  public List<String> getAllCategoryIds(final String groupId, final Metrics metrics) {
    final String classMethod = "GroupsManager.getAllCategoryIds";
    metrics.commonSetup(classMethod);

    ArrayList<String> categoryIds = new ArrayList<>();
    boolean success = false;
    try {
      final Group group = new Group(this.getItemByPrimaryKey(groupId).asMap());
      categoryIds = new ArrayList<>(group.getCategories().keySet());
      success = true;
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>(groupId, classMethod, e));
    }

    metrics.commonClose(success);
    return categoryIds;
  }

  // This function is called when a category is deleted and updates each item in the groups table
  // that was linked to the category accordingly.
  public ResultStatus removeCategoryFromGroups(final Set<String> groupIds, final String categoryId,
      final Metrics metrics) {
    final String classMethod = "GroupsManager.removeCategoryFromGroups";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      final String updateExpression = "remove Categories.#categoryId";
      final NameMap nameMap = new NameMap().with("#categoryId", categoryId);
      UpdateItemSpec updateItemSpec;

      for (final String groupId : groupIds) {
        updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(this.getPrimaryKeyIndex(), groupId)
            .withNameMap(nameMap)
            .withUpdateExpression(updateExpression);
        this.updateItem(updateItemSpec);
      }
      resultStatus.success = true;
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>(categoryId, classMethod, e));
      resultStatus.resultMessage = "Error: Unable to parse request.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus handleGetBatchOfEvents(final Map<String, Object> jsonMap,
      final Metrics metrics) {
    final String classMethod = "GroupsManager.getBatchOfEvents";
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
              JsonEncoders.convertObjectToJson(group.getEventsMap()));
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
