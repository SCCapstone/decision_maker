package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.util.StringUtils;
import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import utilities.ErrorDescriptor;
import utilities.IOStreamsHelper;
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
  public static final String CATEGORIES = "Categories";
  public static final String DEFAULT_POLL_PASS_PERCENT = "DefaultPollPassPercent";
  public static final String DEFAULT_POLL_DURATION = "DefaultPollDuration";
  public static final String DEFAULT_RSVP_DURATION = "DefaultRsvpDuration";
  public static final String EVENTS = "Events";
  public static final String LAST_ACTIVITY = "LastActivity";

  public static final String CATEGORY_ID = "CategoryId";
  public static final String CATEGORY_NAME = "CategoryName";
  public static final String EVENT_NAME = "EventName";
  public static final String EVENT_CREATOR = "EventCreator";
  public static final String CREATED_DATE_TIME = "CreatedDateTime";
  public static final String EVENT_START_DATE_TIME = "EventStartDateTime";
  public static final String TYPE = "Type";
  public static final String POLL_DURATION = "PollDuration";
  public static final String RSVP_DURATION = "RsvpDuration";
  public static final String POLL_PASS_PERCENT = "PollPassPercent";
  public static final String OPTED_IN = "OptedIn";
  public static final String NEXT_EVENT_ID = "NextEventId";
  public static final String SELECTED_CHOICE = "SelectedChoice";

  public static final Map EMPTY_MAP = new HashMap();

  public GroupsManager() {
    super("groups", "GroupId", Regions.US_EAST_2);
  }

  public GroupsManager(final DynamoDB dynamoDB) {
    super("groups", "GroupId", Regions.US_EAST_2, dynamoDB);
  }

  public ResultStatus getGroups(final Map<String, Object> jsonMap, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    final String classMethod = "GroupsManager.getGroups";
    metrics.commonSetup(classMethod);

    boolean success = true;
    String resultMessage = "";
    List<String> groupIds = new ArrayList<>();

    if (jsonMap.containsKey(RequestFields.GROUP_IDS)) {
      groupIds = (List<String>) jsonMap.get(RequestFields.GROUP_IDS);
    } else if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      String username = (String) jsonMap.get(RequestFields.ACTIVE_USER);
      groupIds = DatabaseManagers.USERS_MANAGER.getAllGroupIds(username, metrics, lambdaLogger);
    } else {
      success = false;
      resultMessage = "Error: query key not defined.";
      lambdaLogger.log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
          "Required request keys not found").toString());
    }

    // this will be a json string representing an array of objects
    if (success) {
      List<Map> groups = new ArrayList<>();
      for (String groupId : groupIds) {
        try {
          Item groupData = this.getItemByPrimaryKey(groupId);
          groups.add(groupData.asMap());
        } catch (Exception e) {
          lambdaLogger.log(
              new ErrorDescriptor<>(groupId, classMethod, metrics.getRequestId(), e).toString());
        }
      }

      resultMessage = JsonEncoders.convertListToJson(groups);
    }

    metrics.commonClose(success);
    return new ResultStatus(success, resultMessage);
  }

  public ResultStatus createNewGroup(final Map<String, Object> jsonMap, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    final String classMethod = "GroupsManager.createNewGroup";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, GROUP_NAME, MEMBERS, CATEGORIES,
            DEFAULT_POLL_PASS_PERCENT, DEFAULT_POLL_DURATION, DEFAULT_RSVP_DURATION);

    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupName = (String) jsonMap.get(GROUP_NAME);
        final Optional<List<Integer>> newIcon = Optional
            .ofNullable((List<Integer>) jsonMap.get(ICON));
        final Map<String, Object> categories = (Map<String, Object>) jsonMap.get(CATEGORIES);
        final Integer defaultPollPassPercent = (Integer) jsonMap.get(DEFAULT_POLL_PASS_PERCENT);
        final Integer defaultPollDuration = (Integer) jsonMap.get(DEFAULT_POLL_DURATION);
        final Integer defaultRsvpDuration = (Integer) jsonMap.get(DEFAULT_RSVP_DURATION);
        List<String> members = (List<String>) jsonMap.get(MEMBERS);

        final UUID uuid = UUID.randomUUID();
        final String newGroupId = uuid.toString();

        //sanity check, add the active user to this mapping to make sure his data is added
        members.add(activeUser);

        final Map<String, Object> membersMapped = this
            .getMembersMapForInsertion(members, metrics, lambdaLogger);

        //in case any usernames were removed, update the list for use in below methods to keep data consistent
        members = new LinkedList<>(membersMapped.keySet());

        Item newGroup = new Item()
            .withPrimaryKey(this.getPrimaryKeyIndex(), newGroupId)
            .withString(GROUP_NAME, groupName)
            .withString(GROUP_CREATOR, activeUser)
            .withMap(MEMBERS, membersMapped)
            .withMap(CATEGORIES, categories)
            .withInt(DEFAULT_POLL_PASS_PERCENT, defaultPollPassPercent)
            .withInt(DEFAULT_POLL_DURATION, defaultPollDuration)
            .withInt(DEFAULT_RSVP_DURATION, defaultRsvpDuration)
            .withMap(EVENTS, EMPTY_MAP)
            .withInt(NEXT_EVENT_ID, 1)
            .withString(LAST_ACTIVITY,
                LocalDateTime.now(ZoneId.of("UTC")).format(this.getDateTimeFormatter()));

        String newIconFileName = null;
        if (newIcon.isPresent()) { // if it's there, assume it's new image data
          newIconFileName = DatabaseManagers.S3_ACCESS_MANAGER
              .uploadImage(newIcon.get(), metrics, lambdaLogger).orElseThrow(Exception::new);

          newGroup.withString(ICON, newIconFileName);
        } else {
          newGroup.withNull(ICON);
        }

        PutItemSpec putItemSpec = new PutItemSpec()
            .withItem(newGroup);

        this.putItem(putItemSpec);

        this.updateUsersTable(EMPTY_MAP, members, newGroupId, "", groupName, "",
            Optional.ofNullable(newIconFileName), metrics, lambdaLogger);
        this.updateCategoriesTable(EMPTY_MAP, categories, newGroupId, "", groupName);

        resultStatus = new ResultStatus(true, "Group created successfully!");
      } catch (Exception e) {
        lambdaLogger
            .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(), e).toString());
        resultStatus.resultMessage = "Error: Unable to parse request.";
        lambdaLogger.log(
            new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(), e).toString());
      }
    } else {
      lambdaLogger.log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
          "Required request keys not found").toString());
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }
    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus editGroup(final Map<String, Object> jsonMap, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(GROUP_ID, GROUP_NAME, ICON, GROUP_CREATOR, MEMBERS, CATEGORIES,
            DEFAULT_POLL_PASS_PERCENT, DEFAULT_POLL_DURATION, DEFAULT_RSVP_DURATION, RequestFields.ACTIVE_USER);

    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final String groupName = (String) jsonMap.get(GROUP_NAME);
        final Optional<List<Integer>> newIcon = Optional
            .ofNullable((List<Integer>) jsonMap.get(ICON));
        final Map<String, Object> categories = (Map<String, Object>) jsonMap.get(CATEGORIES);
        final Integer defaultPollPassPercent = (Integer) jsonMap.get(DEFAULT_POLL_PASS_PERCENT);
        final Integer defaultPollDuration = (Integer) jsonMap.get(DEFAULT_POLL_DURATION);
        final Integer defaultRsvpDuration = (Integer) jsonMap.get(DEFAULT_RSVP_DURATION);
        List<String> members = (List<String>) jsonMap.get(MEMBERS);

        final Map<String, Object> dbGroupDataMap = this.getItemByPrimaryKey(groupId)
            .asMap();
        final String groupCreator = (String) dbGroupDataMap.get(GROUP_CREATOR);
        if (this.editInputIsValid(groupId, activeUser, groupCreator, members,
            defaultPollPassPercent, defaultPollDuration)) {

          if (this.editInputHasPermissions(dbGroupDataMap, activeUser, groupCreator)) {
            //all validation is successful, build transaction actions
            final Map<String, Object> membersMapped = this
                .getMembersMapForInsertion(members, metrics, lambdaLogger);

            //in case any usernames were removed, update the list for use in below methods to keep data consistent
            members = new LinkedList<>(membersMapped.keySet());

            String updateExpression =
                "set " + GROUP_NAME + " = :name, " + GROUP_CREATOR
                    + " = :creator, " + MEMBERS + " = :members, " + CATEGORIES + " = :categories, "
                    + DEFAULT_POLL_DURATION + " = :defaultPollDuration, "
                    + DEFAULT_RSVP_DURATION + " = :defaultRsvpDuration, "
                    + DEFAULT_POLL_PASS_PERCENT + " = :defaultPollPassPercent";
            ValueMap valueMap = new ValueMap()
                .withString(":name", groupName)
                .withString(":creator", groupCreator)
                .withMap(":members", membersMapped)
                .withMap(":categories", categories)
                .withInt(":defaultPollDuration", defaultPollDuration)
                .withInt(":defaultRsvpDuration", defaultRsvpDuration)
                .withInt(":defaultPollPassPercent", defaultPollPassPercent);

            //assumption - currently we aren't allowing user's to clear a group's image once set
            String newIconFileName = null;
            if (newIcon.isPresent()) {
              newIconFileName = DatabaseManagers.S3_ACCESS_MANAGER
                  .uploadImage(newIcon.get(), metrics, lambdaLogger).orElseThrow(Exception::new);

              updateExpression += ", " + ICON + " = :icon";
              valueMap.withString(":icon", newIconFileName);
            }

            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(this.getPrimaryKeyIndex(), groupId)
                .withUpdateExpression(updateExpression)
                .withValueMap(valueMap);

            this.updateItem(updateItemSpec);

            //update mappings in users and categories tables
            this.updateUsersTable((Map<String, Object>) dbGroupDataMap.get(MEMBERS),
                members, groupId, (String) dbGroupDataMap.get(GROUP_NAME), groupName,
                (String) dbGroupDataMap.get(ICON), Optional.ofNullable(newIconFileName), metrics,
                lambdaLogger);
            this.updateCategoriesTable(
                (Map<String, Object>) dbGroupDataMap.get(CATEGORIES), categories, groupId,
                (String) dbGroupDataMap.get(GROUP_NAME), groupName);

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

  public ResultStatus newEvent(final Map<String, Object> jsonMap, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    final String classMethod = "GroupsManager.newEvent";
    metrics.commonSetup(classMethod);
    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(EVENT_NAME, CATEGORY_ID, CATEGORY_NAME, CREATED_DATE_TIME, EVENT_START_DATE_TIME,
            TYPE, POLL_DURATION, RSVP_DURATION, EVENT_CREATOR, POLL_PASS_PERCENT, GROUP_ID);
    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
      try {
        final String eventName = (String) jsonMap.get(EVENT_NAME);
        final String categoryId = (String) jsonMap.get(CATEGORY_ID);
        final String categoryName = (String) jsonMap.get(CATEGORY_NAME);
        final String createdDateTime = (String) jsonMap.get(CREATED_DATE_TIME);
        final String eventStartDateTime = (String) jsonMap.get(EVENT_START_DATE_TIME);
        final Integer type = (Integer) jsonMap.get(TYPE);
        final Integer pollDuration = (Integer) jsonMap.get(POLL_DURATION);
        final Integer rsvpDuration = (Integer) jsonMap.get(RSVP_DURATION);
        final Integer pollPassPercent = (Integer) jsonMap.get(POLL_PASS_PERCENT);
        final Map<String, Object> eventCreator = (Map<String, Object>) jsonMap.get(EVENT_CREATOR);
        final String groupId = (String) jsonMap.get(GROUP_ID);
        BigDecimal nextEventId;
        Map<String, Object> optedIn;

        Item groupData = this.getItemByPrimaryKey(groupId);
        if (groupData != null) {
          Map<String, Object> groupDataMapped = groupData.asMap();
          if (groupDataMapped.containsKey(MEMBERS)) {
            optedIn = (Map<String, Object>) groupDataMapped.get(MEMBERS);
            nextEventId = (BigDecimal) groupDataMapped.get(NEXT_EVENT_ID);
          } else {
            lambdaLogger
                .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
                    "Groups has no members field").toString());
            resultStatus.resultMessage = "Error: group has no members field";
            metrics.commonClose(resultStatus.success);
            return resultStatus;
          }
        } else {
          lambdaLogger
              .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
                  "Group not found").toString());
          resultStatus.resultMessage = "Error: group not found";
          metrics.commonClose(resultStatus.success);
          return resultStatus;
        }

        final String eventId = nextEventId.toString();

        if (this.validEventInput(groupId, categoryId, pollDuration,
            rsvpDuration, pollPassPercent)) {
          final Map<String, Object> eventMap = new HashMap<>();

          eventMap.put(CATEGORY_ID, categoryId);
          eventMap.put(CATEGORY_NAME, categoryName);
          eventMap.put(EVENT_NAME, eventName);
          eventMap.put(CREATED_DATE_TIME, createdDateTime);
          eventMap.put(EVENT_START_DATE_TIME, eventStartDateTime);
          eventMap.put(TYPE, type);
          eventMap.put(POLL_DURATION, pollDuration);
          eventMap.put(RSVP_DURATION, rsvpDuration);
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
              .withString(":lastActivity",
                  LocalDateTime.now(ZoneId.of("UTC")).format(this.getDateTimeFormatter()));

          UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withPrimaryKey(this.getPrimaryKeyIndex(), groupId)
              .withNameMap(nameMap)
              .withUpdateExpression(updateExpression)
              .withValueMap(valueMap);

          this.updateItem(updateItemSpec);

          //Hope it works, we aren't using transactions yet (that's why I'm not doing anything with result.
          ResultStatus pendingEventAdded = DatabaseManagers.PENDING_EVENTS_MANAGER
              .addPendingEvent(groupId, eventId, pollDuration);

          resultStatus = new ResultStatus(true, "event added successfully!");
        } else {
          lambdaLogger
              .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
                  "Invalid request, bad input").toString());
          resultStatus.resultMessage = "Invalid request, bad input.";
        }
      } catch (Exception e) {
        lambdaLogger
            .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(), e).toString());
        resultStatus.resultMessage = "Error: Unable to parse request in manager.";
      }
    } else {
      lambdaLogger
          .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
              "Required request keys not found").toString());
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }
    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus optInOutOfEvent(final Map<String, Object> jsonMap) {
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
            .withPrimaryKey(this.getPrimaryKeyIndex(), groupId)
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap)
            .withValueMap(valueMap);

        this.updateItem(updateItemSpec);
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

  private Map<String, Object> getMembersMapForInsertion(final List<String> members,
      final Metrics metrics, final LambdaLogger lambdaLogger) {
    final String classMethod = "GroupsManager.getMembersMapForInsertion";
    metrics.commonSetup(classMethod);
    boolean success = true;

    final Map<String, Object> membersMap = new HashMap<>();

    for (String username : members) {
      try {
        //get user's display name
        Item user = DatabaseManagers.USERS_MANAGER.getItemByPrimaryKey(username);
        Map<String, Object> userData = user.asMap();
        String displayName = (String) userData.get(UsersManager.DISPLAY_NAME);
        String icon = (String) userData.get(UsersManager.ICON);

        membersMap.putIfAbsent(username, ImmutableMap.of(
            UsersManager.DISPLAY_NAME, displayName,
            UsersManager.ICON, icon
        ));
      } catch (Exception e) {
        success = false; // this may give false alarms as users may just have put in bad usernames
        lambdaLogger
            .log(
                new ErrorDescriptor<>(username, classMethod, metrics.getRequestId(), e).toString());
      }
    }

    metrics.commonClose(success);
    return membersMap;
  }

  private boolean editInputIsValid(final String groupId, final String activeUser,
      final String groupCreator, final List<String> members,
      final Integer defaultPollPassPercent,
      final Integer defaultPollDuration) {
    boolean isValid = true;

    if (StringUtils.isNullOrEmpty(groupId) || StringUtils.isNullOrEmpty(activeUser)) {
      isValid = false;
    }

    boolean creatorInGroup = false;
    //you can't remove the creator (owner) from the group
    for (String username : members) {
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

  private boolean validEventInput(
      final String groupId, final String categoryId, final Integer pollDuration,
      final Integer rsvpDuration, final Integer pollPassPercent) {
    boolean isValid = true;
    if (StringUtils.isNullOrEmpty(groupId) || StringUtils.isNullOrEmpty(categoryId)) {
      isValid = false;
    }

    if (pollPassPercent < 0 || pollPassPercent > 100) {
      isValid = false;
    }

    if (pollDuration <= 0 || pollDuration > 10000) {
      isValid = false;
    }

    if (rsvpDuration <= 0 || rsvpDuration > 10000) {
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

  private void updateUsersTable(final Map<String, Object> oldMembers, final List<String> newMembers,
      final String groupId, final String oldGroupName, final String newGroupName,
      final String oldIconFileName, final Optional<String> newIconFileName, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    final Set<String> usersToUpdate = new HashSet<>();

    NameMap nameMap = new NameMap().with("#groupId", groupId);
    String updateExpression = null;
    ValueMap valueMap;
    UpdateItemSpec updateItemSpec;

    if (!oldMembers.keySet().equals(newMembers)) {
      // Make copies of the key sets and use removeAll to figure out where they differ
      final Set<String> newUsernames = new HashSet<>(newMembers);
      final Set<String> removedUsernames = new HashSet<>(oldMembers.keySet());

      // Note: using removeAll on a HashSet has linear time complexity when
      // another HashSet is passed in
      newUsernames.removeAll(oldMembers.keySet());
      removedUsernames.removeAll((newMembers));

      if (newGroupName.equals(oldGroupName) && !newUsernames.isEmpty()) {
        // If the group name wasn't changed and we're adding new users, then only perform
        // updates for the newly added users
        usersToUpdate.addAll(newUsernames);
      } else if (!newGroupName.equals(oldGroupName) || newIconFileName.isPresent()) {
        // If the group name was changed, update every user in newMembers to reflect that.
        // In this case, both the list of members and the group name were changed.
        usersToUpdate.addAll(newMembers);
      }

      if (!removedUsernames.isEmpty()) {
        updateExpression = "remove Groups.#groupId";
        updateItemSpec = new UpdateItemSpec()
            .withNameMap(nameMap)
            .withUpdateExpression(updateExpression);
        for (final String member : removedUsernames) {
          updateItemSpec
              .withPrimaryKey(DatabaseManagers.USERS_MANAGER.getPrimaryKeyIndex(), member);
          DatabaseManagers.USERS_MANAGER.updateItem(updateItemSpec);
        }
      }
    } else if (!newGroupName.equals(oldGroupName) || newIconFileName.isPresent()) {
      // If the group name was changed, update every user in newMembers to reflect that.
      // In this case, the list of members wasn't changed, but the group name was.
      usersToUpdate.addAll(newMembers);
    }

    if (!usersToUpdate.isEmpty()) {
      //we blindly update the users groups mapping with the group name and icon file name
      updateExpression =
          "set " + UsersManager.GROUPS + ".#groupId = :nameIconMap";

      if (newIconFileName.isPresent()) {
        valueMap = new ValueMap().withMap(":nameIconMap", ImmutableMap.of(
            GROUP_NAME, newGroupName,
            ICON, newIconFileName.get()
        ));
      } else {
        valueMap = new ValueMap().withMap(":nameIconMap", ImmutableMap.of(
            GROUP_NAME, newGroupName,
            ICON, oldIconFileName
        ));
      }

      updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withValueMap(valueMap)
          .withNameMap(nameMap);

      for (final String member : usersToUpdate) {
        updateItemSpec.withPrimaryKey(DatabaseManagers.USERS_MANAGER.getPrimaryKeyIndex(), member);
        DatabaseManagers.USERS_MANAGER.updateItem(updateItemSpec);
      }
    }
  }

  private void updateCategoriesTable(final Map<String, Object> oldCategories,
      final Map<String, Object> newCategories, final String groupId, final String oldGroupName,
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

  public List<String> getAllCategoryIds(String groupId, Metrics metrics,
      LambdaLogger lambdaLogger) {
    final String classMethod = "GroupsManager.getAllCategoryIds";
    metrics.commonSetup(classMethod);

    ArrayList<String> categoryIds = new ArrayList<>();
    boolean success = false;
    try {
      Item dbData = this.getItemByPrimaryKey(groupId);
      Map<String, Object> dbDataMap = dbData.asMap(); // specific group record as a map
      Map<String, String> categoryMap = (Map<String, String>) dbDataMap.get(CATEGORIES);
      categoryIds = new ArrayList<>(categoryMap.keySet());
      success = true;
    } catch (Exception e) {
      lambdaLogger
          .log(new ErrorDescriptor<>(groupId, classMethod, metrics.getRequestId(), e).toString());
    }

    metrics.commonClose(success);
    return categoryIds;
  }

  // This function is called when a category is deleted and updates each item in the groups table
  // that was linked to the category accordingly.
  public ResultStatus removeCategoryFromGroups(List<String> groupIds, String categoryId,
      Metrics metrics,
      LambdaLogger lambdaLogger) {
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
      lambdaLogger.log(
          new ErrorDescriptor<>(categoryId, classMethod, metrics.getRequestId(), e).toString());
      resultStatus.resultMessage = "Error: Unable to parse request.";
    }
    metrics.commonClose(resultStatus.success);

    return resultStatus;
  }
}
