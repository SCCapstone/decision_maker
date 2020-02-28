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
import models.Group;
import models.Member;
import models.User;
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
  public static final String DEFAULT_VOTING_DURATION = "DefaultVotingDuration";
  public static final String DEFAULT_RSVP_DURATION = "DefaultRsvpDuration";
  public static final String EVENTS = "Events";
  public static final String LAST_ACTIVITY = "LastActivity";

  public static final String CATEGORY_ID = "CategoryId";
  public static final String CATEGORY_NAME = "CategoryName";
  public static final String EVENT_NAME = "EventName";
  public static final String EVENT_CREATOR = "EventCreator";
  public static final String CREATED_DATE_TIME = "CreatedDateTime";
  public static final String EVENT_START_DATE_TIME = "EventStartDateTime";
  public static final String VOTING_DURATION = "VotingDuration";
  public static final String RSVP_DURATION = "RsvpDuration";
  public static final String OPTED_IN = "OptedIn";
  public static final String VOTING_NUMBERS = "VotingNumbers";
  public static final String TENTATIVE_CHOICES = "TentativeAlgorithmChoices";
  public static final String NEXT_EVENT_ID = "NextEventId";
  public static final String SELECTED_CHOICE = "SelectedChoice";

  public static final Integer MAX_DURATION = 10000;

  public static final Map<String, Object> EMPTY_MAP = new HashMap<>();

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
            DEFAULT_VOTING_DURATION, DEFAULT_RSVP_DURATION);

    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupName = (String) jsonMap.get(GROUP_NAME);
        final Optional<List<Integer>> newIcon = Optional
            .ofNullable((List<Integer>) jsonMap.get(ICON));
        final Map<String, Object> categories = (Map<String, Object>) jsonMap.get(CATEGORIES);
        final Integer defaultVotingDuration = (Integer) jsonMap.get(DEFAULT_VOTING_DURATION);
        final Integer defaultRsvpDuration = (Integer) jsonMap.get(DEFAULT_RSVP_DURATION);
        List<String> members = (List<String>) jsonMap.get(MEMBERS);

        final UUID uuid = UUID.randomUUID();
        final String newGroupId = uuid.toString();
        final String lastActivity = LocalDateTime.now(ZoneId.of("UTC"))
            .format(this.getDateTimeFormatter());

        //sanity check, add the active user to this mapping to make sure his data is added
        members.add(activeUser);

        final Map<String, Object> membersMapped = this
            .getMembersMapForInsertion(members, metrics, lambdaLogger);

        Item newGroup = new Item()
            .withPrimaryKey(this.getPrimaryKeyIndex(), newGroupId)
            .withString(GROUP_NAME, groupName)
            .withString(GROUP_CREATOR, activeUser)
            .withMap(MEMBERS, membersMapped)
            .withMap(CATEGORIES, categories)
            .withInt(DEFAULT_VOTING_DURATION, defaultVotingDuration)
            .withInt(DEFAULT_RSVP_DURATION, defaultRsvpDuration)
            .withMap(EVENTS, EMPTY_MAP)
            .withInt(NEXT_EVENT_ID, 1)
            .withString(LAST_ACTIVITY, lastActivity);

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

        final Group oldGroup = new Group();
        oldGroup.setMembers(EMPTY_MAP);
        this.updateUsersTable(oldGroup, new Group(newGroup.asMap()), metrics, lambdaLogger);
        this.updateCategoriesTable(EMPTY_MAP, categories, newGroupId, "", groupName);

        resultStatus = new ResultStatus(true, "Group created successfully!");
      } catch (Exception e) {
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
    final String classMethod = "GroupsManager.editGroup";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, GROUP_ID, GROUP_NAME, MEMBERS, CATEGORIES,
            DEFAULT_VOTING_DURATION, DEFAULT_RSVP_DURATION);

    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
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

        final Map<String, Object> dbGroupDataMap = this.getItemByPrimaryKey(groupId)
            .asMap();
        final String groupCreator = (String) dbGroupDataMap.get(GROUP_CREATOR);
        if (this.editInputIsValid(groupId, activeUser, groupCreator, members,
            defaultVotingDuration, defaultRsvpDuration)) {

          if (this.editInputHasPermissions(dbGroupDataMap, activeUser, groupCreator)) {
            //all validation is successful, build transaction actions
            final Map<String, Object> membersMapped = this
                .getMembersMapForInsertion(members, metrics, lambdaLogger);

            String updateExpression =
                "set " + GROUP_NAME + " = :name, " + GROUP_CREATOR
                    + " = :creator, " + MEMBERS + " = :members, " + CATEGORIES + " = :categories, "
                    + DEFAULT_VOTING_DURATION + " = :defaultVotingDuration, "
                    + DEFAULT_RSVP_DURATION + " = :defaultRsvpDuration";
            ValueMap valueMap = new ValueMap()
                .withString(":name", groupName)
                .withString(":creator", groupCreator)
                .withMap(":members", membersMapped)
                .withMap(":categories", categories)
                .withInt(":defaultVotingDuration", defaultVotingDuration)
                .withInt(":defaultRsvpDuration", defaultRsvpDuration);

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
            final Group oldGroup = new Group(dbGroupDataMap);
            final Group newGroup = Group.builder()
                .groupId(groupId)
                .groupName(groupName)
                .icon(newIconFileName)
                .lastActivity(oldGroup.getLastActivity())
                .build();
            newGroup.setMembers(membersMapped);
            this.updateUsersTable(oldGroup, newGroup, metrics, lambdaLogger);
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
        resultStatus.resultMessage = "Error: Unable to parse request in manager";
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

  public ResultStatus newEvent(final Map<String, Object> jsonMap, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    final String classMethod = "GroupsManager.newEvent";
    metrics.commonSetup(classMethod);
    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, EVENT_NAME, CATEGORY_ID, CATEGORY_NAME,
            EVENT_START_DATE_TIME, VOTING_DURATION, RSVP_DURATION, GROUP_ID);
    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String eventName = (String) jsonMap.get(EVENT_NAME);
        final String categoryId = (String) jsonMap.get(CATEGORY_ID);
        final String categoryName = (String) jsonMap.get(CATEGORY_NAME);
        final String eventStartDateTime = (String) jsonMap.get(EVENT_START_DATE_TIME);
        final Integer votingDuration = (Integer) jsonMap.get(VOTING_DURATION);
        final Integer rsvpDuration = (Integer) jsonMap.get(RSVP_DURATION);
        final String groupId = (String) jsonMap.get(GROUP_ID);

        final Item groupData = this.getItemByPrimaryKey(groupId);
        final Map<String, Object> groupDataMapped = groupData.asMap();
        final Group oldGroup = new Group(groupDataMapped);
        final Map<String, Object> optedIn = (Map<String, Object>) groupDataMapped
            .get(MEMBERS); //TODO figure out how use oldGroup for this
        final String eventId = oldGroup.getNextEventId().toString();
        final String lastActivity = LocalDateTime.now(ZoneId.of("UTC"))
            .format(this.getDateTimeFormatter());

        if (this.validEventInput(groupId, categoryId, votingDuration,
            rsvpDuration)) {
          final Map<String, Object> eventMap = new HashMap<>();
          final Map<String, Object> eventCreator = new HashMap<>();
          eventCreator.put("username", activeUser);

          eventMap.put(CATEGORY_ID, categoryId);
          eventMap.put(CATEGORY_NAME, categoryName);
          eventMap.put(EVENT_NAME, eventName);
          eventMap.put(CREATED_DATE_TIME,
              LocalDateTime.now(ZoneId.of("UTC")).format(this.getDateTimeFormatter()));
          eventMap.put(EVENT_START_DATE_TIME, eventStartDateTime);
          eventMap.put(VOTING_DURATION, votingDuration);
          eventMap.put(RSVP_DURATION, rsvpDuration);
          eventMap.put(OPTED_IN, optedIn);
          eventMap.put(EVENT_CREATOR, eventCreator);
          eventMap.put(SELECTED_CHOICE, null);
          eventMap.put(TENTATIVE_CHOICES, EMPTY_MAP);
          eventMap.put(VOTING_NUMBERS, EMPTY_MAP);

          String updateExpression =
              "set " + EVENTS + ".#eventId = :map, " + NEXT_EVENT_ID + " = :nextEventId, "
                  + LAST_ACTIVITY + " = :lastActivity";
          NameMap nameMap = new NameMap().with("#eventId", eventId);
          ValueMap valueMap = new ValueMap()
              .withMap(":map", eventMap)
              .withNumber(":nextEventId", oldGroup.getNextEventId() + 1)
              .withString(":lastActivity", lastActivity);

          UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withPrimaryKey(this.getPrimaryKeyIndex(), groupId)
              .withNameMap(nameMap)
              .withUpdateExpression(updateExpression)
              .withValueMap(valueMap);

          this.updateItem(updateItemSpec);

          //Hope it works, we aren't using transactions yet (that's why I'm not doing anything with result.
          if (rsvpDuration > 0) {
            ResultStatus pendingEventAdded = DatabaseManagers.PENDING_EVENTS_MANAGER
                .addPendingEvent(groupId, eventId, rsvpDuration, metrics, lambdaLogger);
          } else {
            //this will set potential algo choices and create the entry for voting duration timeout
            Map<String, Object> processPendingEventInput = ImmutableMap.of(GROUP_ID, groupId,
                RequestFields.EVENT_ID, eventId, PendingEventsManager.SCANNER_ID,
                DatabaseManagers.PENDING_EVENTS_MANAGER.getPartitionKey());
            ResultStatus pendingEventAdded = DatabaseManagers.PENDING_EVENTS_MANAGER
                .processPendingEvent(processPendingEventInput, metrics, lambdaLogger);
          }

          this.updateUsersTable(
              oldGroup,
              oldGroup.toBuilder().lastActivity(lastActivity).build(),
              metrics,
              lambdaLogger
          );

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

  public ResultStatus optInOutOfEvent(final Map<String, Object> jsonMap, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    final String classMethod = "GroupsManager.optInOutOfEvent";
    metrics.commonSetup(classMethod);
    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(GROUP_ID, RequestFields.PARTICIPATING, RequestFields.EVENT_ID,
            RequestFields.ACTIVE_USER);

    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final Boolean participating = (Boolean) jsonMap.get(RequestFields.PARTICIPATING);
        final String eventId = (String) jsonMap.get(RequestFields.EVENT_ID);
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final Map<String, Object> userMap =
            this.getMembersMapForInsertion(Arrays.asList(activeUser), metrics, lambdaLogger);

        String updateExpression;
        ValueMap valueMap = null;

        if (participating) { // add the user to the optIn
          updateExpression =
              "set " + EVENTS + ".#eventId." + OPTED_IN + ".#username = :userMap";
          valueMap = new ValueMap()
              .withMap(":userMap", (Map<String, Object>) userMap.get(activeUser));
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

  public ResultStatus leaveGroup(final Map<String, Object> jsonMap, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    final String classMethod = "GroupsManager.leaveGroup";
    metrics.commonSetup(classMethod);
    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(GROUP_ID, RequestFields.ACTIVE_USER);
    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        Item groupData = this.getItemByPrimaryKey(groupId);

        if (groupData != null) {
          Map<String, Object> groupDataMapped = groupData.asMap();
          final String groupCreator = (String) groupDataMapped.get(GROUP_CREATOR);
          if (!groupCreator.equals(activeUser)) {
            String updateExpression = "remove " + MEMBERS + ".#username";

            NameMap nameMap = new NameMap()
                .with("#username", activeUser);

            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(this.getPrimaryKeyIndex(), groupId)
                .withUpdateExpression(updateExpression)
                .withNameMap(nameMap);

            this.updateItem(updateItemSpec);

            // remove this group from the group attribute in active user object
            updateExpression = "remove " + UsersManager.GROUPS + ".#groupId";
            nameMap = new NameMap()
                .with("#groupId", groupId);
            updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(this.getPrimaryKeyIndex(), groupId)
                .withUpdateExpression(updateExpression)
                .withNameMap(nameMap)
                .withPrimaryKey(DatabaseManagers.USERS_MANAGER.getPrimaryKeyIndex(), activeUser);
            DatabaseManagers.USERS_MANAGER.updateItem(updateItemSpec);

            // add this now left group to the groupsLeft attribute in active user object
            updateExpression =
                "set " + UsersManager.GROUPS_LEFT + ".#groupId = :groupMap";
            ValueMap valueMap = new ValueMap()
                .withMap(":groupMap", new HashMap<String, Object>() {{
                  put(GROUP_NAME, groupDataMapped.get(GROUP_NAME));
                  put(ICON, groupDataMapped.get(ICON));
                }});

            updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression(updateExpression)
                .withValueMap(valueMap)
                .withNameMap(nameMap)
                .withPrimaryKey(DatabaseManagers.USERS_MANAGER.getPrimaryKeyIndex(), activeUser);
            DatabaseManagers.USERS_MANAGER.updateItem(updateItemSpec);

            resultStatus = new ResultStatus(true, "Group left successfully.");
          } else {
            lambdaLogger
                .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
                    "Owner cannot leave group").toString());
            resultStatus.resultMessage = "Error: Owner cannot leave group.";
          }
        } else {
          lambdaLogger
              .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
                  "Group not found").toString());
          resultStatus.resultMessage = "Error: Group not found.";
        }
      } catch (Exception e) {
        lambdaLogger
            .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(), e).toString());
        resultStatus.resultMessage = "Error: Unable to parse request in manager.";
      }
    } else {
      lambdaLogger
          .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
              "Request keys not found").toString());
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus voteForChoice(final Map<String, Object> jsonMap, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    final String classMethod = "GroupsManager.voteForChoice";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(GROUP_ID, RequestFields.EVENT_ID, RequestFields.CHOICE_ID, RequestFields.VOTE_VALUE,
            RequestFields.ACTIVE_USER);

    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
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
        lambdaLogger
            .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(), e).toString());
      }
    } else {
      resultStatus.resultMessage = "Error: required request keys not found.";
      lambdaLogger
          .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
              "Error: required request keys not found.").toString());
    }

    metrics.commonClose(resultStatus.success);
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

        membersMap.putIfAbsent(username, new HashMap<String, Object>() {{
          put(UsersManager.DISPLAY_NAME, displayName);
          put(UsersManager.ICON, icon);
        }});
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
      final String groupCreator, final List<String> members, final Integer defaultVotingDuration,
      final Integer defaultRsvpDuration) {
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

    if (defaultVotingDuration <= 0 || defaultVotingDuration > MAX_DURATION) {
      isValid = false;
    }

    if (defaultRsvpDuration < 0 || defaultRsvpDuration > MAX_DURATION) {
      isValid = false;
    }

    return isValid;
  }

  private boolean validEventInput(
      final String groupId, final String categoryId, final Integer votingDuration,
      final Integer rsvpDuration) {
    boolean isValid = true;
    if (StringUtils.isNullOrEmpty(groupId) || StringUtils.isNullOrEmpty(categoryId)) {
      isValid = false;
    }

    if (votingDuration <= 0 || votingDuration > MAX_DURATION) {
      isValid = false;
    }

    if (rsvpDuration < 0 || rsvpDuration > MAX_DURATION) {
      isValid = false;
    }
    return isValid;
  }

  private boolean editInputHasPermissions(final Map<String, Object> dbGroupDataMap,
      final String activeUser, final String groupCreator) {
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

  private void sendAddedToGroupNotifications(final List<String> usernames, final Group addedTo,
      final Metrics metrics, final LambdaLogger lambdaLogger) {
    final String classMethod = "GroupsManager.sendAddedToGroupNotifications";
    metrics.commonSetup(classMethod);

    boolean success = true;

    for (String username : usernames) {
      try {
        final User user = new User(
            DatabaseManagers.USERS_MANAGER.getItemByPrimaryKey(username).asMap());

        if (!username.equals(addedTo.getGroupCreator())) {
          if (user.pushEndpointArnIsSet() && user.getAppSettings().getMuted() == 0) {
            DatabaseManagers.SNS_ACCESS_MANAGER.sendMessage(user.getPushEndpointArn(),
                "You have been added to new group: " + addedTo.getGroupName());
          }
        }
      } catch (Exception e) {
        success = false;
        lambdaLogger
            .log(
                new ErrorDescriptor<>(username, classMethod, metrics.getRequestId(), e).toString());
      }
    }

    metrics.commonClose(success);
  }

  private void updateUsersTable(final Group oldGroup, final Group newGroup, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    final String classMethod = "GroupsManager.updateUsersTable";
    metrics.commonSetup(classMethod);
    boolean success = true;

    final Set<String> usersToUpdate = new HashSet<>();

    NameMap nameMap = new NameMap().with("#groupId", newGroup.getGroupId());
    String updateExpression;
    ValueMap valueMap;
    UpdateItemSpec updateItemSpec;

    final Set<String> newMembers = newGroup.getMembers().keySet();
    final Set<String> addedUsernames = new HashSet<>(newMembers);

    final Set<String> oldMembers = oldGroup.getMembers().keySet();
    final Set<String> removedUsernames = new HashSet<>(oldMembers);

    // Note: using removeAll on a HashSet has linear time complexity when another HashSet is passed in
    addedUsernames.removeAll(oldMembers);
    removedUsernames.removeAll(newMembers);

    if (newGroup.groupNameIsSet() && !newGroup.getGroupName().equals(oldGroup.getGroupName())) {
      usersToUpdate.addAll(newMembers);
    } else if (newGroup.iconIsSet() && !newGroup.getIcon().equals(oldGroup.getIcon())) {
      usersToUpdate.addAll(newMembers);
    } else if (newGroup.lastActivityIsSet() && !newGroup.getLastActivity()
        .equals(oldGroup.getLastActivity())) {
      usersToUpdate.addAll(newMembers);
    } else if (!oldMembers.equals(newMembers)) {
      usersToUpdate.addAll(addedUsernames);
    }

    //update users with new group mapping based on which attributes were updated
    if (!usersToUpdate.isEmpty()) {
      //we have to update the entire mapping because the groupId key may
      //not be in the mapping yet if this is a new group
      updateExpression = "set " + UsersManager.GROUPS + ".#groupId = :nameIconMap";

      Map<String, Object> groupDataForUser = new HashMap<>();
      groupDataForUser.put(GROUP_NAME, newGroup.getGroupName());

      if (newGroup.iconIsSet()) {
        groupDataForUser.put(ICON, newGroup.getIcon());
      } else {
        groupDataForUser.put(ICON, oldGroup.getIcon());
      }

      if (newGroup.lastActivityIsSet()) {
        groupDataForUser.put(LAST_ACTIVITY, newGroup.getLastActivity());
      } else {
        groupDataForUser.put(LAST_ACTIVITY, oldGroup.getLastActivity());
      }

      valueMap = new ValueMap().withMap(":nameIconMap", groupDataForUser);

      updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withValueMap(valueMap)
          .withNameMap(nameMap);

      for (final String member : usersToUpdate) {
        try {
          updateItemSpec
              .withPrimaryKey(DatabaseManagers.USERS_MANAGER.getPrimaryKeyIndex(), member);
          DatabaseManagers.USERS_MANAGER.updateItem(updateItemSpec);
        } catch (Exception e) {
          success = false;
          lambdaLogger
              .log(
                  new ErrorDescriptor<>(member, classMethod, metrics.getRequestId(), e).toString());
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
          lambdaLogger
              .log(new ErrorDescriptor<>(member, classMethod, metrics.getRequestId(), e)
                  .toString());
        }
      }
    }

    try {
      //blind send...
      this.sendAddedToGroupNotifications(new ArrayList<>(addedUsernames), newGroup, metrics,
          lambdaLogger);
    } catch (final Exception e) {
      success = false;
      lambdaLogger
          .log(
              new ErrorDescriptor<>(new ArrayList<>(addedUsernames), classMethod,
                  metrics.getRequestId(), e).toString());
    }

    metrics.commonClose(success);
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

  public ResultStatus setEventTentativeChoices(final String groupId, final String eventId,
      final Map<String, Object> tentativeChoices, final Map<String, Object> groupDataMapped,
      final Metrics metrics, final LambdaLogger lambdaLogger) {
    final String classMethod = "GroupsManager.setEventTentativeChoices";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      final Map<String, Object> votingNumbersSetup = this
          .getVotingNumbersSetup(tentativeChoices);

      final String lastActivity = LocalDateTime.now(ZoneId.of("UTC"))
          .format(this.getDateTimeFormatter());

      //update the event
      String updateExpression =
          "set " + GroupsManager.EVENTS + ".#eventId." + GroupsManager.TENTATIVE_CHOICES
              + " = :tentativeChoices, " + GroupsManager.LAST_ACTIVITY + " = :currentDate, "
              + GroupsManager.EVENTS + ".#eventId." + GroupsManager.VOTING_NUMBERS
              + " = :votingNumbers";
      NameMap nameMap = new NameMap().with("#eventId", eventId);
      ValueMap valueMap = new ValueMap()
          .withMap(":tentativeChoices", tentativeChoices)
          .withMap(":votingNumbers", votingNumbersSetup)
          .withString(":currentDate", lastActivity);

      UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withPrimaryKey(GroupsManager.GROUP_ID, groupId)
          .withUpdateExpression(updateExpression)
          .withNameMap(nameMap)
          .withValueMap(valueMap);

      this.updateItem(updateItemSpec);

      final Group oldGroup = new Group(groupDataMapped);
      this.updateUsersTable(
          oldGroup,
          oldGroup.clone().toBuilder().lastActivity(lastActivity).build(),
          metrics,
          lambdaLogger
      );
    } catch (Exception e) {
      resultStatus.resultMessage = "Error setting tentative algorithm choices";
      lambdaLogger.log(
          new ErrorDescriptor<>(String.format("GroupId: %s, EventId: %s", groupId, eventId),
              classMethod, metrics.getRequestId(), e).toString());
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private Map<String, Object> getVotingNumbersSetup(
      final Map<String, Object> tentativeAlgorithmChoices) {
    final Map<String, Object> votingNumbers = new HashMap<>();

    //we're filling a map keyed by choiceId with empty maps
    for (String choiceId : tentativeAlgorithmChoices.keySet()) {
      votingNumbers.put(choiceId, ImmutableMap.of());
    }

    return votingNumbers;
  }

  public ResultStatus setEventSelectedChoice(final String groupId, final String eventId,
      final String result, final Map<String, Object> groupDataMapped,
      final Metrics metrics, final LambdaLogger lambdaLogger) {
    final String classMethod = "GroupsManager.setEventSelectedChoice";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      final String lastActivity = LocalDateTime.now(ZoneId.of("UTC"))
          .format(this.getDateTimeFormatter());

      //update the event
      String updateExpression =
          "set " + GroupsManager.EVENTS + ".#eventId." + GroupsManager.SELECTED_CHOICE
              + " = :selectedChoice, " + GroupsManager.LAST_ACTIVITY + " = :currentDate";
      NameMap nameMap = new NameMap().with("#eventId", eventId);
      ValueMap valueMap = new ValueMap().withString(":selectedChoice", result)
          .withString(":currentDate", lastActivity);

      UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withPrimaryKey(GroupsManager.GROUP_ID, groupId)
          .withUpdateExpression(updateExpression)
          .withNameMap(nameMap)
          .withValueMap(valueMap);

      DatabaseManagers.GROUPS_MANAGER.updateItem(updateItemSpec);

      this.updateItem(updateItemSpec);

      final Group oldGroup = new Group(groupDataMapped);
      this.updateUsersTable(
          oldGroup,
          oldGroup.clone().toBuilder().lastActivity(lastActivity).build(),
          metrics,
          lambdaLogger
      );
    } catch (Exception e) {
      resultStatus.resultMessage = "Error setting selected choice";
      lambdaLogger.log(
          new ErrorDescriptor<>(String.format("GroupId: %s, EventId: %s", groupId, eventId),
              classMethod, metrics.getRequestId(), e).toString());
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
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
