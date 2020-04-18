package imports;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
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
          final GroupForApiResponse groupForApiResponse = new GroupForApiResponse(group,
              batchNumber);

          resultStatus = new ResultStatus(true,
              JsonUtils.convertObjectToJson(groupForApiResponse.asMap()));
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

  /**
   * This method is imperative for not overloading the front end with data. Since a group can have
   * an unlimited number of events, we need to limit how many we return at any one time. This is why
   * we're watching events. This function sorts all of the events from the first one to the oldest
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

    //get all of the events from the first event to the oldest event
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
    final String classMethod = "GroupsManager.createNewGroup";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, GROUP_NAME, MEMBERS, CATEGORIES,
            DEFAULT_VOTING_DURATION, DEFAULT_RSVP_DURATION, IS_OPEN);

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
        newGroup.setMembersLeft(Collections.emptyMap());
        newGroup.setEvents(Collections.emptyMap());
        newGroup.setLastActivity(lastActivity);
        this.putItem(newGroup);

        //old group being null signals we're creating a new group
        //updatedEventId being null signals this isn't an event update
        this.updateUsersTable(null, newGroup, null, false, metrics);
        this.updateCategoriesTable(null, newGroup, metrics);

        resultStatus = new ResultStatus(true,
            JsonUtils.convertObjectToJson(new GroupForApiResponse(newGroup).asMap()));
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

  /**
   * This methods takes in all of the editable group attributes via that json map. If the inputs are
   * valid and the requesting user has permissions, then the group gets updated and the necessary
   * data for denormalization is sent to the groups table and the categories table respectively.
   *
   * @param jsonMap Common request map from endpoint handler containing api input.
   * @param metrics Standard metrics object for profiling and logging
   * @return Standard result status object giving insight on whether the request was successful
   */
  public ResultStatus editGroup(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "GroupsManager.editGroup";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, GROUP_ID, GROUP_NAME, MEMBERS, CATEGORIES,
            DEFAULT_VOTING_DURATION, DEFAULT_RSVP_DURATION, IS_OPEN, RequestFields.BATCH_NUMBER);

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
        final Integer batchNumber = (Integer) jsonMap.get(RequestFields.BATCH_NUMBER);
        final boolean isOpen = (boolean) jsonMap.get(IS_OPEN);
        List<String> members = (List<String>) jsonMap.get(MEMBERS);

        //TODO update the categories passed in to be a list of ids, then create categories map
        //TODO similar to what we're doing with the members above (currently we're just relying on
        //TODO user input which is bad

        final Group oldGroup = new Group(this.getMapByPrimaryKey(groupId));

        final Optional<String> errorMessage = this
            .editGroupInputIsValid(oldGroup, activeUser, members, defaultVotingDuration,
                defaultRsvpDuration);
        if (!errorMessage.isPresent()) {
          //all validation is successful, build transaction actions
          final Map<String, Object> membersMapped = this
              .getMembersMapForInsertion(members, metrics);

          String updateExpression =
              "set " + GROUP_NAME + " = :name, " + MEMBERS + " = :members, " + CATEGORIES
                  + " = :categories, " + DEFAULT_VOTING_DURATION + " = :defaultVotingDuration, "
                  + DEFAULT_RSVP_DURATION + " = :defaultRsvpDuration, " + IS_OPEN + " = :isOpen";
          ValueMap valueMap = new ValueMap()
              .withString(":name", groupName)
              .withMap(":members", membersMapped)
              .withMap(":categories", categories)
              .withInt(":defaultVotingDuration", defaultVotingDuration)
              .withInt(":defaultRsvpDuration", defaultRsvpDuration)
              .withBoolean(":isOpen", isOpen);

          //assumption - currently we aren't allowing user's to clear a group's image once set
          String newIconFileName = null;
          if (newIcon.isPresent()) {
            newIconFileName = DatabaseManagers.S3_ACCESS_MANAGER
                .uploadImage(newIcon.get(), metrics).orElseThrow(Exception::new);

            updateExpression += ", " + ICON + " = :icon";
            valueMap.withString(":icon", newIconFileName);
          }

          UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withUpdateExpression(updateExpression)
              .withValueMap(valueMap);

          this.updateItem(groupId, updateItemSpec);

          //clone the old group and update all of the fields since we successfully updated the db
          final Group newGroup = oldGroup.clone();
          newGroup.setGroupName(groupName);
          newGroup.setMembers(membersMapped);
          newGroup.setCategories(categories);
          newGroup.setDefaultVotingDuration(defaultVotingDuration);
          newGroup.setDefaultRsvpDuration(defaultRsvpDuration);
          newGroup.setOpen(isOpen);
          if (newIconFileName != null) {
            newGroup.setIcon(newIconFileName);
          }

          //update mappings in users and categories tables
          this.updateUsersTable(oldGroup, newGroup, null, false, metrics);
          this.updateCategoriesTable(oldGroup, newGroup, metrics);

          resultStatus = new ResultStatus(true,
              JsonUtils
                  .convertObjectToJson(new GroupForApiResponse(newGroup, batchNumber).asMap()));
        } else {
          resultStatus.resultMessage = errorMessage.get();
          metrics.log(new WarningDescriptor<>(jsonMap, classMethod, errorMessage.get()));
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

        final Group group = new Group(this.getMapByPrimaryKey(groupId));
        if (activeUser.equals(group.getGroupCreator())) {
          Set<String> members = group.getMembers().keySet();
          Set<String> membersLeft = group.getMembersLeft().keySet();
          Set<String> categoryIds = group.getCategories().keySet();

          // Remove the group from the users and categories tables
          final ResultStatus removeFromLeftUsersResult = DatabaseManagers.USERS_MANAGER
              .removeGroupsLeftFromUsers(membersLeft, groupId, metrics);
          final ResultStatus removeGroupFromActiveUsers = this
              .removeUsersFromGroupAndSendNotificationsOnDelete(members, group, metrics);
          final ResultStatus removeFromCategoriesResult = DatabaseManagers.CATEGORIES_MANAGER
              .removeGroupFromCategories(categoryIds, groupId, metrics);

          if (removeFromLeftUsersResult.success && removeGroupFromActiveUsers.success
              && removeFromCategoriesResult.success) {
            this.deleteItem(groupId);

            resultStatus = new ResultStatus(true, "Group deleted successfully!");

            //blind attempt to delete the group's icon and pending events
            //if either fail, we'll get a notification and we can manually delete if necessary
            DatabaseManagers.S3_ACCESS_MANAGER.deleteImage(group.getIcon(), metrics);

            final Set<String> pendingEventIds = group.getEvents().entrySet().stream()
                .filter((e) -> (new EventForSorting(e.getValue()).isPending()))
                .map(Entry::getKey).collect(Collectors.toSet());
            DatabaseManagers.PENDING_EVENTS_MANAGER
                .deleteAllPendingGroupEvents(groupId, pendingEventIds, metrics);
          } else {
            resultStatus = removeFromLeftUsersResult.applyResultStatus(removeGroupFromActiveUsers)
                .applyResultStatus(removeFromCategoriesResult);
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

          if (newEvent.getRsvpDuration() > 0) {
            valueMap.withMap(":map", newEvent.asMap());
          } else {
            valueMap.withMap(":map", newEvent.asEventMap());
          }

          UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withNameMap(nameMap)
              .withUpdateExpression(updateExpression)
              .withValueMap(valueMap);

          this.updateItem(groupId, updateItemSpec);

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

          final Group newGroup = new Group(this.getMapByPrimaryKey(groupId));

          //when rsvp is not greater than 0, updateUsersTable gets called by setEventTentativeChoices
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
        final User user = new User(DatabaseManagers.USERS_MANAGER.getMapByPrimaryKey(activeUser));

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
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap)
            .withValueMap(valueMap);

        this.updateItem(groupId, updateItemSpec);

        resultStatus = new ResultStatus(true, "Opted in/out successfully");
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

  public ResultStatus leaveGroup(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "GroupsManager.leaveGroup";
    metrics.commonSetup(classMethod);
    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays.asList(GROUP_ID, RequestFields.ACTIVE_USER);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        final Group group = new Group(this.getMapByPrimaryKey(groupId));
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
              .withUpdateExpression(updateExpression)
              .withNameMap(nameMap)
              .withValueMap(valueMap);

          this.updateItem(groupId, updateItemSpec);

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
              .withNameMap(nameMap);
          DatabaseManagers.USERS_MANAGER.updateItem(activeUser, updateItemSpec);

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
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap)
            .withValueMap(valueMap);

        this.updateItem(groupId, updateItemSpec);
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
        final User user = new User(DatabaseManagers.USERS_MANAGER.getMapByPrimaryKey(username));

        membersMap.putIfAbsent(username, user.asMember().asMap());
      } catch (Exception e) {
        success = false; // this may give false alarms as users may just have put in bad usernames
        metrics.log(new ErrorDescriptor<>(username, classMethod, e));
      }
    }

    metrics.commonClose(success);
    return membersMap;
  }

  /**
   * This function takes the old definition of a group and checks to see if proposed edits are
   * valid.
   *
   * @param oldGroup              this is the old group definition that is attempting to be edited
   * @param activeUser            the user doing the edit
   * @param members               the new list of members for the group
   * @param defaultVotingDuration the new voting duration
   * @param defaultRsvpDuration   the new rsvp duration
   * @return A nullable errorMessage. If null, then there was no error and it is valid
   */
  private Optional<String> editGroupInputIsValid(final Group oldGroup, final String activeUser,
      final List<String> members, final Integer defaultVotingDuration,
      final Integer defaultRsvpDuration) {

    String errorMessage = null;

    if (oldGroup.isOpen()) {
      if (!oldGroup.getMembers().containsKey(activeUser)) {
        errorMessage = this.getUpdatedErrorMessage(errorMessage, "Error: Bad permissions.");
      }
    } else {
      if (!oldGroup.getGroupCreator().equals(activeUser)) {
        errorMessage = this.getUpdatedErrorMessage(errorMessage, "Error: Bad permissions.");
      }
    }

    if (!members.contains(oldGroup.getGroupCreator())) {
      errorMessage = this.getUpdatedErrorMessage(errorMessage,
          "Error: Group creator cannot be removed from group.");
    }

    //make a copy so we don't actually update the member map on the old group
    final Set<String> membersLeftCopy = new HashSet<>(oldGroup.getMembersLeft().keySet());
    membersLeftCopy.retainAll(members); // calculates the intersection of members left and members
    if (membersLeftCopy.size() > 0) {
      errorMessage = this.getUpdatedErrorMessage(errorMessage,
          "Error: Error: Cannot add a user that left.");
    }

    if (defaultVotingDuration < 0 || defaultVotingDuration > MAX_DURATION) {
      errorMessage = this.getUpdatedErrorMessage(errorMessage, "Error: Bad voting duration.");
    }

    if (defaultRsvpDuration < 0 || defaultRsvpDuration > MAX_DURATION) {
      errorMessage = this.getUpdatedErrorMessage(errorMessage, "Error: Bad consider duration.");
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

        if (!username.equals(addedTo.getGroupCreator())) {
          //Note: no need to check user's group muted settings since they're just being added
          if (user.pushEndpointArnIsSet()) {
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
                removedFrom.getGroupId(),
                metadata);
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
            DatabaseManagers.USERS_MANAGER.updateItem(oldMember, updateItemSpec);
          } catch (final Exception e) {
            success = false;
            metrics.log(new ErrorDescriptor<>(oldMember, classMethod, e));
          }
        } else if (isNewEvent) {
          // this means the oldMember is the event creator, we should only update the last activity
          try {
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
          } catch (final Exception e) {
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

    try {
      //blind send...
      this.sendAddedToGroupNotifications(addedUsernames, newGroup, metrics);
    } catch (final Exception e) {
      success = false;
      metrics.log(new ErrorDescriptor<>(addedUsernames, classMethod, e));
    }

    if (updatedEventId != null) {
      try {
        //blind send...
        this.sendEventUpdatedNotification(newMembers, newGroup, updatedEventId, isNewEvent,
            metrics);
      } catch (Exception e) {
        success = false;
        metrics.log(new ErrorDescriptor<>(newMembers, classMethod, e));
      }
    }

    metrics.commonClose(success);
  }

  /**
   * This method takes in the old version of a group and the new version of a group after being
   * edited or created. Depending on the differences, the appropriate denormalized data gets updated
   * in the categories table.
   *
   * @param oldGroup The group definition before an edit or null if this is a group create.
   * @param newGroup The group definition after a change has been made.
   * @param metrics  Standard metrics object for profiling and logging.
   */
  private void updateCategoriesTable(final Group oldGroup, final Group newGroup,
      final Metrics metrics) {
    final String classMethod = "GroupsManager.updateCategoriesTable";
    metrics.commonSetup(classMethod);

    boolean success = true;

    final Set<String> categoriesToUpdate = new HashSet<>();

    String updateExpression;
    UpdateItemSpec updateItemSpec;
    final NameMap nameMap = new NameMap().with("#groupId", newGroup.getGroupId());
    final ValueMap valueMap = new ValueMap().withString(":groupName", newGroup.getGroupName());

    //If this is a new group or the group name was changed all categories need updating
    if (oldGroup == null || !newGroup.getGroupName().equals(oldGroup.getGroupName())) {
      categoriesToUpdate.addAll(newGroup.getCategories().keySet());
    }

    //if the categories aren't the same, something needs to be removed/added
    if (oldGroup != null && !oldGroup.getCategories().keySet()
        .equals(newGroup.getCategories().keySet())) {
      // Make copies of the key sets and use removeAll to figure out where they differ
      final Set<String> newCategoryIds = new HashSet<>(newGroup.getCategories().keySet());
      final Set<String> removedCategoryIds = new HashSet<>(oldGroup.getCategories().keySet());

      // Note: using removeAll on a HashSet has linear time complexity when
      // another HashSet is passed in
      newCategoryIds.removeAll(oldGroup.getCategories().keySet());
      removedCategoryIds.removeAll(newGroup.getCategories().keySet());

      //add these newly added categories for update
      categoriesToUpdate.addAll(newCategoryIds);

      if (!removedCategoryIds.isEmpty()) {
        updateExpression = "remove Groups.#groupId";
        updateItemSpec = new UpdateItemSpec()
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);
        for (final String categoryId : removedCategoryIds) {
          try {
            DatabaseManagers.CATEGORIES_MANAGER.updateItem(categoryId, updateItemSpec);
          } catch (final Exception e) {
            success = false;
            metrics.log(new ErrorDescriptor<>(categoryId, classMethod, e));
          }
        }
      }
    }

    if (!categoriesToUpdate.isEmpty()) {
      updateExpression = "set Groups.#groupId = :groupName";
      updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withNameMap(nameMap)
          .withValueMap(valueMap);
      for (final String categoryId : categoriesToUpdate) {
        try {
          DatabaseManagers.CATEGORIES_MANAGER.updateItem(categoryId, updateItemSpec);
        } catch (final Exception e) {
          success = false;
          metrics.log(new ErrorDescriptor<>(categoryId, classMethod, e));
        }
      }
    }

    metrics.commonClose(success);
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

      //set all of the update statements
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
            .withNameMap(nameMap)
            .withUpdateExpression(updateExpression);
        this.updateItem(groupId, updateItemSpec);
      }
      resultStatus.success = true;
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>(categoryId, classMethod, e));
      resultStatus.resultMessage = "Error: Unable to parse request.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This function takes in a group and batch number and gets the appropriate map of events for the
   * group based on the batch number. This method is used in the 'infinitely' scrolling list of
   * events on a group's page. Using this, we continue to get the next set or 'batch' of events.
   *
   * @param jsonMap Common request map from endpoint handler containing api input
   * @param metrics Standard metrics object for profiling and logging
   * @return Standard result status object giving insight on whether the request was successful
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
