package imports;

import static java.util.stream.Collectors.toMap;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
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
import models.Event;
import models.Group;
import models.User;
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
  public static final Integer INITIAL_EVENTS_PULLED = 25;

  public GroupsManager() {
    super("groups", "GroupId", Regions.US_EAST_2);
  }

  public GroupsManager(final DynamoDB dynamoDB) {
    super("groups", "GroupId", Regions.US_EAST_2, dynamoDB);
  }

  public ResultStatus getGroups(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "GroupsManager.getGroups";
    metrics.commonSetup(classMethod);

    boolean success = true;
    String resultMessage = "";
    List<String> groupIds = new ArrayList<>();

    if (jsonMap.containsKey(RequestFields.GROUP_IDS)) {
      groupIds = (List<String>) jsonMap.get(RequestFields.GROUP_IDS);
    } else if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      String username = (String) jsonMap.get(RequestFields.ACTIVE_USER);
      groupIds = DatabaseManagers.USERS_MANAGER.getAllGroupIds(username, metrics);
    } else {
      success = false;
      resultMessage = "Error: query key not defined.";
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found"));
    }

    //we should now have the groupIds that we are getting groups for
    if (success) {
      List<Map> groups = new ArrayList<>();
      for (String groupId : groupIds) {
        try {
          final Group group = new Group(this.getItemByPrimaryKey(groupId).asMap());
          this.limitNumberOfEvents(group, INITIAL_EVENTS_PULLED);
          groups.add(group.asMap());
        } catch (Exception e) {
          metrics.log(new ErrorDescriptor<>(groupId, classMethod, e));
        }
      }

      resultMessage = JsonEncoders.convertListToJson(groups);
    }

    metrics.commonClose(success);
    return new ResultStatus(success, resultMessage);
  }

  private void limitNumberOfEvents(final Group group, final Integer count) {
    if (group.getEvents().size() > count) {
      //we stream each key pair in the entrySet to be sorted, limited, then collected into a new map
      Map<String, Event> sortedEvents = group.getEvents()
          .entrySet()
          .stream()
          .sorted((e1, e2) -> this.isEventXAfterY(e1.getValue(), e2.getValue()))
          .limit(count)
          .collect(toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

      group.setEvents(sortedEvents);
    }
  }

  private int isEventXAfterY(final Event x, final Event y) {
    final LocalDateTime xCreationDate = LocalDateTime
        .parse(x.getCreatedDateTime(), this.getDateTimeFormatter());
    final LocalDateTime yCreationDate = LocalDateTime
        .parse(y.getCreatedDateTime(), this.getDateTimeFormatter());

    return yCreationDate.compareTo(xCreationDate);
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
        jsonMap.putIfAbsent(GROUP_CREATOR, activeUser);

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
        jsonMap.put(GROUP_ID, newGroupId);

        final String lastActivity = LocalDateTime.now(ZoneId.of("UTC"))
            .format(this.getDateTimeFormatter());
        jsonMap.put(LAST_ACTIVITY, lastActivity);

        jsonMap.put(EVENTS, Collections.emptyMap());
        jsonMap.put(NEXT_EVENT_ID, 1);

        final Group newGroup = new Group(jsonMap);
        PutItemSpec putItemSpec = new PutItemSpec()
            .withItem(newGroup.asItem());

        this.putItem(putItemSpec);

        final Group oldGroup = new Group();
        oldGroup.setMembers(Collections.emptyMap());
        this.updateUsersTable(oldGroup, newGroup, metrics);
        this.updateCategoriesTable(Collections.emptyMap(), newGroup.getCategories(), newGroupId, "",
            newGroup.getGroupName());

        resultStatus = new ResultStatus(true, "Group created successfully!");
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

        if (this.editInputIsValid(groupId, activeUser, oldGroup.getGroupCreator(), members,
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
            final Group newGroup = new Group(this.getItemByPrimaryKey(groupId).asMap());
            this.updateUsersTable(oldGroup, newGroup, metrics);
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
          Set<String> categoryIds = group.getCategories().keySet();

          ResultStatus removeFromUsersResult = DatabaseManagers.USERS_MANAGER
              .removeGroupFromUsers(members, groupId, metrics);
          ResultStatus removeFromCategoriesResult = DatabaseManagers.CATEGORIES_MANAGER
              .removeGroupFromCategories(categoryIds, groupId, metrics);

          if (removeFromUsersResult.success && removeFromCategoriesResult.success) {
            //TODO can probably put this into a transaction
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
        .asList(RequestFields.ACTIVE_USER, EVENT_NAME, CATEGORY_ID, CATEGORY_NAME,
            EVENT_START_DATE_TIME, VOTING_DURATION, RSVP_DURATION, GROUP_ID);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(GROUP_ID);
        final Group oldGroup = new Group(this.getItemByPrimaryKey(groupId).asMap());
        final String eventId = oldGroup.getNextEventId().toString();
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
              "set " + EVENTS + ".#eventId = :map, " + NEXT_EVENT_ID + " = :nextEventId, "
                  + LAST_ACTIVITY + " = :lastActivity";
          NameMap nameMap = new NameMap().with("#eventId", eventId);
          ValueMap valueMap = new ValueMap()
              .withMap(":map", newEvent.asMap())
              .withNumber(":nextEventId", oldGroup.getNextEventId() + 1)
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
            final Map<String, Object> processPendingEventInput = ImmutableMap
                .of(GROUP_ID, groupId, RequestFields.EVENT_ID, eventId,
                    PendingEventsManager.SCANNER_ID,
                    DatabaseManagers.PENDING_EVENTS_MANAGER.getPartitionKey());
            final ResultStatus pendingEventAdded = DatabaseManagers.PENDING_EVENTS_MANAGER
                .processPendingEvent(processPendingEventInput, metrics);
          }

          final Group newGroup = new Group(this.getItemByPrimaryKey(groupId).asMap());

          this.updateUsersTable(oldGroup, newGroup, metrics);

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
            metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Owner cannot leave group"));
            resultStatus.resultMessage = "Error: Owner cannot leave group.";
          }
        } else {
          metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Group not found"));
          resultStatus.resultMessage = "Error: Group not found.";
        }
      } catch (Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus.resultMessage = "Error: Unable to parse request in manager.";
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Request keys not found").toString());
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

  private void sendAddedToGroupNotifications(final List<String> usernames, final Group addedTo,
      final Metrics metrics) {
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
        metrics.log(new ErrorDescriptor<>(username, classMethod, e));
      }
    }

    metrics.commonClose(success);
  }

  private void updateUsersTable(final Group oldGroup, final Group newGroup, final Metrics metrics) {
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
          metrics.log(new ErrorDescriptor<>(member, classMethod, e));
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
      this.sendAddedToGroupNotifications(new ArrayList<>(addedUsernames), newGroup, metrics);
    } catch (final Exception e) {
      success = false;
      metrics.log(new ErrorDescriptor<>(new ArrayList<>(addedUsernames), classMethod, e));
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

  public ResultStatus setEventTentativeChoices(final String groupId, final String eventId,
      final Map<String, Object> tentativeChoices, final Group oldGroup, final Metrics metrics) {
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

      this.updateUsersTable(
          oldGroup,
          oldGroup.clone().toBuilder().lastActivity(lastActivity).build(),
          metrics
      );
    } catch (Exception e) {
      resultStatus.resultMessage = "Error setting tentative algorithm choices";
      metrics.log(new ErrorDescriptor<>(String.format("GroupId: %s, EventId: %s", groupId, eventId),
          classMethod, e));
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
      final String result, final Group oldGroup, final Metrics metrics) {
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

      this.updateUsersTable(
          oldGroup,
          oldGroup.clone().toBuilder().lastActivity(lastActivity).build(),
          metrics
      );
    } catch (Exception e) {
      resultStatus.resultMessage = "Error setting selected choice";
      metrics.log(new ErrorDescriptor<>(String.format("GroupId: %s, EventId: %s", groupId, eventId),
          classMethod, e));
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
  public ResultStatus removeCategoryFromGroups(final List<String> groupIds, final String categoryId,
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
}
