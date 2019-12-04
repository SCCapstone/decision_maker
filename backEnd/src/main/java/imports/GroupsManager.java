package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.util.StringUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import utilities.IOStreamsHelper;
import utilities.JsonEncoders;
import utilities.RequestFields;
import utilities.ResultStatus;

public class GroupsManager extends DatabaseAccessManager {

  public static final String GROUP_ID = "GroupId";
  public static final String GROUP_NAME = "GroupName";
  public static final String ICON = "Icon";
  public static final String GROUP_CREATOR = "GroupCreator";
  public static final String MEMBERS = "Members";
  public static final String CATEGORIES = "Categories";
  public static final String DEFAULT_POLL_PASS_PERCENT = "DefaultPollPassPercent";
  public static final String DEFAULT_POLL_DURATION = "DefaultPollDuration";
  public static final String EVENTS = "Events";
  public static final String LAST_ACTIVITY = "LastActivity";

  public static final String EVENT_ID = "EventId";
  public static final String CATEGORY_ID = "CategoryId";
  public static final String CATEGORY_NAME = "CategoryName";
  public static final String EVENT_NAME = "EventName";
  public static final String EVENT_CREATOR = "EventCreator";
  public static final String CREATED_DATE_TIME = "CreatedDateTime";
  public static final String EVENT_START_DATE_TIME = "EventStartDateTime";
  public static final String TYPE = "Type";
  public static final String POLL_DURATION = "PollDuration";
  public static final String POLL_PASS_PERCENT = "PollPassPercent";
  public static final String OPTED_IN = "OptedIn";
  public static final String NEXT_EVENT_ID = "NextEventId";
  public static final String SELECTED_CHOICE = "SelectedChoice";

  public static final Map EMPTY_MAP = new HashMap();

  public static final GroupsManager GROUPS_MANAGER = new GroupsManager();

  public GroupsManager() {
    super("groups", "GroupId", Regions.US_EAST_2);
  }

  public static ResultStatus getGroups(Map<String, Object> jsonMap) {
    boolean success = true;
    String resultMessage = "";
    List<String> groupIds = new ArrayList<String>();

    if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      String username = (String) jsonMap.get(RequestFields.ACTIVE_USER);
      groupIds = UsersManager.getAllGroupIds(username);
    } else if (jsonMap.containsKey(RequestFields.GROUP_IDS)) {
      groupIds = (List<String>) jsonMap.get(RequestFields.GROUP_IDS);
    } else {
      success = false;
      resultMessage = "Error: query key not defined.";
    }

    // this will be a json string representing an array of objects
    List<Map> groups = new ArrayList<Map>();
    for (String id : groupIds) {
      Item dbData = GROUPS_MANAGER
          .getItem(new GetItemSpec().withPrimaryKey(GROUPS_MANAGER.getPrimaryKeyIndex(), id));
      if (dbData != null) {
        groups.add(dbData.asMap());
      } else {
        //maybe log this idk, we probably shouldn't have ids that don't point to groups in the db?
      }
    }

    if (success) {
      resultMessage = JsonEncoders.convertListToJson(groups);
    }

    return new ResultStatus(success, resultMessage);
  }

  public static ResultStatus createNewGroup(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(GROUP_NAME, ICON, GROUP_CREATOR, MEMBERS, CATEGORIES,
            DEFAULT_POLL_PASS_PERCENT, DEFAULT_POLL_DURATION);

    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
      try {
        final String groupName = (String) jsonMap.get(GROUP_NAME);
        final String icon = (String) jsonMap.get(ICON);
        final String groupCreator = (String) jsonMap.get(GROUP_CREATOR);
        final Map<String, Object> members = (Map<String, Object>) jsonMap.get(MEMBERS);
        final Map<String, Object> categories = (Map<String, Object>) jsonMap.get(CATEGORIES);
        final Integer defaultPollPassPercent = (Integer) jsonMap.get(DEFAULT_POLL_PASS_PERCENT);
        final Integer defaultPollDuration = (Integer) jsonMap.get(DEFAULT_POLL_DURATION);

        final UUID uuid = UUID.randomUUID();
        final String newGroupId = uuid.toString();
        final Date currentDate = new Date(); // UTC

        GROUPS_MANAGER.updateMembersMapForInsertion(members);

        Item newGroup = new Item()
            .withPrimaryKey(GROUPS_MANAGER.getPrimaryKeyIndex(), newGroupId)
            .withString(GROUP_NAME, groupName)
            .withString(ICON, icon)
            .withString(GROUP_CREATOR, groupCreator)
            .withMap(MEMBERS, members)
            .withMap(CATEGORIES, categories)
            .withInt(DEFAULT_POLL_PASS_PERCENT, defaultPollPassPercent)
            .withInt(DEFAULT_POLL_DURATION, defaultPollDuration)
            .withMap(EVENTS, EMPTY_MAP)
            .withInt(NEXT_EVENT_ID, 1)
            .withString(LAST_ACTIVITY, GROUPS_MANAGER.getDbDateFormatter().format(currentDate));

        PutItemSpec putItemSpec = new PutItemSpec()
            .withItem(newGroup);

        GROUPS_MANAGER.putItem(putItemSpec);

        GROUPS_MANAGER.updateUsersTable(EMPTY_MAP, members, newGroupId, "", groupName);
        GROUPS_MANAGER.updateCategoriesTable(EMPTY_MAP, categories, newGroupId, "", groupName);

        resultStatus = new ResultStatus(true, "Group created successfully!");
      } catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request.";
      }
    } else {
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }
    return resultStatus;
  }

  public static ResultStatus editGroup(final Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(GROUP_ID, GROUP_NAME, ICON, GROUP_CREATOR, MEMBERS, CATEGORIES,
            DEFAULT_POLL_PASS_PERCENT, DEFAULT_POLL_DURATION, RequestFields.ACTIVE_USER);

    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final String groupName = (String) jsonMap.get(GROUP_NAME);
        final String icon = (String) jsonMap.get(ICON);
        final String groupCreator = (String) jsonMap.get(GROUP_CREATOR);
        final Map<String, Object> members = (Map<String, Object>) jsonMap.get(MEMBERS);
        final Map<String, Object> categories = (Map<String, Object>) jsonMap.get(CATEGORIES);
        final Integer defaultPollPassPercent = (Integer) jsonMap.get(DEFAULT_POLL_PASS_PERCENT);
        final Integer defaultPollDuration = (Integer) jsonMap.get(DEFAULT_POLL_DURATION);
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        if (GROUPS_MANAGER.editInputIsValid(groupId, activeUser, groupCreator, members,
            defaultPollPassPercent, defaultPollDuration)) {
          final Map<String, Object> dbGroupDataMap = GROUPS_MANAGER.getItemByPrimaryKey(groupId)
              .asMap();

          if (GROUPS_MANAGER.editInputHasPermissions(dbGroupDataMap, activeUser, groupCreator)) {
            //all validation is successful, build transaction actions
            GROUPS_MANAGER.updateMembersMapForInsertion(members); // this implicitly changes data

            //update mappings in users and categories tables
            GROUPS_MANAGER.updateUsersTable((Map<String, Object>) dbGroupDataMap.get(MEMBERS),
                members, groupId, (String) dbGroupDataMap.get(GROUP_NAME), groupName);
            GROUPS_MANAGER.updateCategoriesTable(
                (Map<String, Object>) dbGroupDataMap.get(CATEGORIES), categories, groupId,
                (String) dbGroupDataMap.get(GROUP_NAME), groupName);

            String updateExpression =
                "set " + GROUP_NAME + " = :name, " + ICON + " = :icon, " + GROUP_CREATOR
                    + " = :creator, " + MEMBERS + " = :members, " + CATEGORIES + " = :categories, "
                    + DEFAULT_POLL_DURATION + " = :defaultPollDuration, "
                    + DEFAULT_POLL_PASS_PERCENT + " = :defaultPollPassPercent";
            ValueMap valueMap = new ValueMap()
                .withString(":name", groupName)
                .withString(":icon", icon)
                .withString(":creator", groupCreator)
                .withMap(":members", members)
                .withMap(":categories", categories)
                .withInt(":defaultPollDuration", defaultPollDuration)
                .withInt(":defaultPollPassPercent", defaultPollPassPercent);

            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(GROUPS_MANAGER.getPrimaryKeyIndex(), groupId)
                .withUpdateExpression(updateExpression)
                .withValueMap(valueMap);

            GROUPS_MANAGER.updateItem(updateItemSpec);

            //TODO update the users and categories tables accordinly (https://github.com/SCCapstone/decision_maker/issues/118)

            resultStatus = new ResultStatus(true, "The group was saved successfully!");
          } else {
            resultStatus.resultMessage = "Invalid request, missing permissions";
          }
        } else {
          resultStatus.resultMessage = "Invalid request, bad input.";
        }
      } catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request in manager";
      }
    } else {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    return resultStatus;
  }

  public static ResultStatus newEvent(final Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(EVENT_NAME, CATEGORY_ID, CATEGORY_NAME, CREATED_DATE_TIME, EVENT_START_DATE_TIME,
            TYPE, POLL_DURATION, EVENT_CREATOR, POLL_PASS_PERCENT, GROUP_ID);

    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
      try {
        final String eventName = (String) jsonMap.get(EVENT_NAME);
        final String categoryId = (String) jsonMap.get(CATEGORY_ID);
        final String categoryName = (String) jsonMap.get(CATEGORY_NAME);
        final String createdDateTime = (String) jsonMap.get(CREATED_DATE_TIME);
        final String eventStartDateTime = (String) jsonMap.get(EVENT_START_DATE_TIME);
        final Integer type = (Integer) jsonMap.get(TYPE);
        final Integer pollDuration = (Integer) jsonMap.get(POLL_DURATION);
        final Integer pollPassPercent = (Integer) jsonMap.get(POLL_PASS_PERCENT);
        final Map<String, Object> eventCreator = (Map<String, Object>) jsonMap.get(EVENT_CREATOR);
        final String groupId = (String) jsonMap.get(GROUP_ID);

        Date currentDate = new Date(); // no args gives current date

        BigDecimal nextEventId;
        Map<String, Object> optedIn;

        Item groupData = GROUPS_MANAGER
            .getItem(
                new GetItemSpec().withPrimaryKey(GROUPS_MANAGER.getPrimaryKeyIndex(), groupId));
        if (groupData != null) {
          Map<String, Object> groupDataMapped = groupData.asMap();
          if (groupDataMapped.containsKey(MEMBERS)) {
            optedIn = (Map<String, Object>) groupDataMapped.get(MEMBERS);
            nextEventId = (BigDecimal) groupDataMapped.get(NEXT_EVENT_ID);
          } else {
            resultStatus.resultMessage = "Error: group has no members field";
            return resultStatus;
          }
        } else {
          resultStatus.resultMessage = "Error: group not found";
          return resultStatus;
        }

        final String eventId = nextEventId.toString();

        if (GROUPS_MANAGER
            .makeEventInputIsValid(eventId, "Not empty", groupId, categoryId, pollDuration,
                pollPassPercent)) {
          final Map<String, Object> eventMap = new HashMap<>();

          eventMap.put(CATEGORY_ID, categoryId);
          eventMap.put(CATEGORY_NAME, categoryName);
          eventMap.put(EVENT_NAME, eventName);
          eventMap.put(CREATED_DATE_TIME, createdDateTime);
          eventMap.put(EVENT_START_DATE_TIME, eventStartDateTime);
          eventMap.put(TYPE, type);
          eventMap.put(POLL_DURATION, pollDuration);
          eventMap.put(POLL_PASS_PERCENT, pollPassPercent);
          eventMap.put(OPTED_IN, optedIn);
          eventMap.put(EVENT_CREATOR, eventCreator);
          eventMap.put(SELECTED_CHOICE, "calculating...");

          String updateExpression =
              "set " + EVENTS + ".#eventId = :map, " + NEXT_EVENT_ID + " = :nextEventId, "
                  + LAST_ACTIVITY + " = :lastActivity";
          NameMap nameMap = new NameMap().with("#eventId", eventId);
          ValueMap valueMap = new ValueMap()
              .withMap(":map", eventMap)
              .withNumber(":nextEventId", nextEventId.add(new BigDecimal(1)))
              .withString(":lastActivity", GROUPS_MANAGER.getDbDateFormatter().format(currentDate));

          UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withPrimaryKey(GROUPS_MANAGER.getPrimaryKeyIndex(), groupId)
              .withNameMap(nameMap)
              .withUpdateExpression(updateExpression)
              .withValueMap(valueMap);

          GROUPS_MANAGER.updateItem(updateItemSpec);

          //Hope it works, we aren't using transactions yet (that's why I'm not doing anything with result.
          ResultStatus pendingEventAdded = PendingEventsManager
              .addPendingEvent(groupId, eventId, currentDate, pollDuration);

          resultStatus = new ResultStatus(true, "event added successfully!");
        } else {
          resultStatus.resultMessage = "Invalid request, bad input.";
        }
      } catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request in manager.";
      }
    } else {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }
    return resultStatus;
  }

  public static ResultStatus optInOutOfEvent(final Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(GROUP_ID, RequestFields.PARTICIPATING, RequestFields.EVENT_ID,
            RequestFields.ACTIVE_USER, RequestFields.DISPLAY_NAME);

    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final Boolean participating = (Boolean) jsonMap.get(RequestFields.PARTICIPATING);
        final String eventId = (String) jsonMap.get(RequestFields.EVENT_ID);
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String displayName = (String) jsonMap.get(RequestFields.DISPLAY_NAME);

        String updateExpression;
        ValueMap valueMap = null;

        if (participating) { // add the user to the optIn
          updateExpression =
              "set " + EVENTS + ".#eventId." + OPTED_IN + ".#username = :displayName";
          valueMap = new ValueMap().withString(":displayName", displayName);
        } else {
          updateExpression = "remove " + EVENTS + ".#eventId." + OPTED_IN + ".#username";
        }

        final NameMap nameMap = new NameMap()
            .with("#eventId", eventId)
            .with("#username", activeUser);

        UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(GROUPS_MANAGER.getPrimaryKeyIndex(), groupId)
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap)
            .withValueMap(valueMap);

        GROUPS_MANAGER.updateItem(updateItemSpec);
        resultStatus = new ResultStatus(true, "Opted in/out successfully");
      } catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request in manager.";
      }
    } else {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    return resultStatus;
  }

  //Note we return the value for clarity in some uses, but the actual input is being updated
  private Map<String, Object> updateMembersMapForInsertion(final Map<String, Object> members) {
    List<String> usernamesToDrop = new ArrayList<>();

    for (String username : members.keySet()) {
      Item user = UsersManager.getUser(username);

      if (user != null) {
        try {
          //get user's actual name
          Map<String, Object> userData = user.asMap();
          String firstName = (String) userData.get(UsersManager.FIRST_NAME);
          String lastName = (String) userData.get(UsersManager.LAST_NAME);

          members.replace(username, firstName + " " + lastName);
        } catch (Exception e) {
          //couldn't get the user's data, don't add to the group rn
          //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
          usernamesToDrop.add(username);
        }
      } else {
        usernamesToDrop.add(username); // user not in db
      }
    }

    for (String usernameToDrop : usernamesToDrop) {
      members.remove(usernameToDrop);
    }

    return members;
  }

  private boolean editInputIsValid(final String groupId, final String activeUser,
      final String groupCreator, final Map<String, Object> members,
      final Integer defaultPollPassPercent,
      final Integer defaultPollDuration) {
    boolean isValid = true;

    if (StringUtils.isNullOrEmpty(groupId) || StringUtils.isNullOrEmpty(activeUser)) {
      isValid = false;
    }

    boolean creatorInGroup = false;
    //you can't remove the creator (owner) from the group
    for (String username : members.keySet()) {
      if (groupCreator.equals(username)) {
        creatorInGroup = true;
        break;
      }
    }

    isValid = isValid && creatorInGroup;

    if (defaultPollPassPercent < 0 || defaultPollPassPercent > 100) {
      isValid = false;
    }

    if (defaultPollDuration <= 0 || defaultPollDuration > 10000) {
      isValid = false;
    }

    return isValid;
  }

  private boolean makeEventInputIsValid(final String eventId, final String activeUser,
      final String groupId, final String categoryId, final Integer pollDuration,
      final Integer pollPassPercent) {
    boolean isValid = true;

    if (StringUtils.isNullOrEmpty(eventId) || StringUtils.isNullOrEmpty(activeUser) ||
        StringUtils.isNullOrEmpty(groupId) || StringUtils.isNullOrEmpty(categoryId)) {
      isValid = false;
    }

    if (pollPassPercent < 0 || pollPassPercent > 100) {
      isValid = false;
    }

    if (pollDuration <= 0 || pollDuration > 10000) {
      isValid = false;
    }
    return isValid;
  }

  private boolean editInputHasPermissions(final Map<String, Object> dbGroupDataMap,
      final String activeUser, final String groupCreator) {
    //the group creator is not changed or it is changed and the active user is the current creator
    boolean hasPermission = true;

    if (!dbGroupDataMap.get(GROUP_CREATOR).equals(groupCreator) && !dbGroupDataMap
        .get(GROUP_CREATOR).equals(activeUser)) {
      hasPermission = false;
    }

    return hasPermission;
  }

  private void updateUsersTable(final Map<String, Object> oldMembers,
      final Map<String, Object> newMembers,
      final String groupId, final String oldGroupName, final String newGroupName) {
    final UsersManager usersManager = new UsersManager();
    final Set<String> usersToUpdate = new HashSet<>();
    String updateExpression;
    NameMap nameMap = new NameMap().with("#groupId", groupId);
    ValueMap valueMap = new ValueMap().withString(":groupName", newGroupName);
    UpdateItemSpec updateItemSpec = new UpdateItemSpec().withNameMap(nameMap);

    if (!oldMembers.keySet().equals(newMembers.keySet())) {
      // Make copies of the key sets and use removeAll to figure out where they differ
      final Set<String> newUsernames = new HashSet<>(newMembers.keySet());
      final Set<String> removedUsernames = new HashSet<>(oldMembers.keySet());

      // Note: using removeAll on a HashSet has linear time complexity when
      // another HashSet is passed in
      newUsernames.removeAll(oldMembers.keySet());
      removedUsernames.removeAll((newMembers.keySet()));

      if (newGroupName.equals(oldGroupName) && !newUsernames.isEmpty()) {
        // If the group name wasn't changed and we're adding new users, then only perform
        // updates for the newly added users
        usersToUpdate.addAll(newUsernames);
      } else if (!newGroupName.equals(oldGroupName)) {
        // If the group name was changed, update every user in newMembers to reflect that.
        // In this case, both the list of members and the group name were changed.
        usersToUpdate.addAll(newMembers.keySet());
      }

      if (!removedUsernames.isEmpty()) {
        updateExpression = "remove Groups.#groupId";
        updateItemSpec = new UpdateItemSpec()
            .withNameMap(nameMap)
            .withUpdateExpression(updateExpression);
        for (final String member : removedUsernames) {
          updateItemSpec.withPrimaryKey(usersManager.getPrimaryKeyIndex(), member);
          usersManager.updateItem(updateItemSpec);
        }
      }
    } else if (!newGroupName.equals(oldGroupName)) {
      // If the group name was changed, update every user in newMembers to reflect that.
      // In this case, the list of members wasn't changed, but the group name was.
      usersToUpdate.addAll(newMembers.keySet());
    }

    if (!usersToUpdate.isEmpty()) {
      updateExpression = "set Groups.#groupId = :groupName";
      updateItemSpec.withUpdateExpression(updateExpression).withValueMap(valueMap);
      for (final String member : usersToUpdate) {
        updateItemSpec.withPrimaryKey(usersManager.getPrimaryKeyIndex(), member);
        usersManager.updateItem(updateItemSpec);
      }
    }
  }

  private void updateCategoriesTable(final Map<String, Object> oldCategories,
      final Map<String, Object> newCategories, final String groupId, final String oldGroupName,
      final String newGroupName) {
    final CategoriesManager categoriesManager = new CategoriesManager();
    final Set<String> usersToUpdate = new HashSet<>();
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
        usersToUpdate.addAll(newCategoryIds);
      } else if (!newGroupName.equals(oldGroupName)) {
        // If the group name was changed, update every category in newCategories to reflect that.
        // In this case, both the list of categories and the group name were changed.
        usersToUpdate.addAll(newCategories.keySet());
      }

      if (!removedCategoryIds.isEmpty()) {
        updateExpression = "remove Groups.#groupId";
        updateItemSpec.withUpdateExpression(updateExpression);
        for (final String categoryId : removedCategoryIds) {
          updateItemSpec.withPrimaryKey(categoriesManager.getPrimaryKeyIndex(), categoryId);
          categoriesManager.updateItem(updateItemSpec);
        }
      }
    } else if (!newGroupName.equals(oldGroupName)) {
      // If the group name was changed, update every category in newCategories to reflect that.
      // In this case, the list of categories wasn't changed, but the group name was.
      usersToUpdate.addAll(newCategories.keySet());
    }

    if (!usersToUpdate.isEmpty()) {
      updateExpression = "set Groups.#groupId = :groupName";
      updateItemSpec.withUpdateExpression(updateExpression).withValueMap(valueMap);
      for (final String member : usersToUpdate) {
        updateItemSpec.withPrimaryKey(categoriesManager.getPrimaryKeyIndex(), member);
        categoriesManager.updateItem(updateItemSpec);
      }
    }
  }

  public static List<String> getAllCategoryIds(String groupId) {
    Item dbData = GROUPS_MANAGER
        .getItem(new GetItemSpec().withPrimaryKey(GROUPS_MANAGER.getPrimaryKeyIndex(), groupId));

    Map<String, Object> dbDataMap = dbData.asMap(); // specific group record as a map
    Map<String, String> categoryMap = (Map<String, String>) dbDataMap.get(CATEGORIES);

    return new ArrayList<String>(categoryMap.keySet());
  }

  // This function is called when a category is deleted and updates each item in the groups table
  // that was linked to the category accordingly.
  public static void removeCategoryFromGroups(List<String> groupIds, String categoryId) {
    try {
      final String updateExpression = "remove Categories.#categoryId";
      final NameMap nameMap = new NameMap().with("#categoryId", categoryId);
      UpdateItemSpec updateItemSpec;

      for (final String groupId : groupIds) {
        updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(GROUPS_MANAGER.getPrimaryKeyIndex(), groupId)
            .withNameMap(nameMap)
            .withUpdateExpression(updateExpression);
        GROUPS_MANAGER.updateItem(updateItemSpec);
      }
    } catch (Exception e) {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
    }
  }
}
