package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  public static final String EVENT_ID = "EventId";
  public static final String CATEGORY_ID = "CategoryId";
  public static final String EVENT_NAME = "EventName";
  public static final String CREATED_DATE_TIME = "CreatedDateTime";
  public static final String EVENT_START_DATE_TIME = "EventStartDateTime";
  public static final String TYPE = "Type";
  public static final String POLL_DURATION = "PollDuration";
  public static final String POLL_PASS_PERCENT = "PollPassPercent";
  public static final String OPTED_IN = "OptedIn";
  
  private UsersManager usersManager = new UsersManager();

  public GroupsManager() {
    super("groups", "GroupId", Regions.US_EAST_2);
  }

  public ResultStatus getGroups(Map<String, Object> jsonMap) {
    boolean success = true;
    String resultMessage = "";
    List<String> groupIds = new ArrayList<String>();

    if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      String username = (String) jsonMap.get(RequestFields.ACTIVE_USER);
      groupIds = this.usersManager.getAllGroupIds(username);
    } else if (jsonMap.containsKey(RequestFields.GROUP_IDS)) {
      groupIds = (List<String>) jsonMap.get(RequestFields.GROUP_IDS);
    } else {
      success = false;
      resultMessage = "Error: query key not defined.";
    }

    // this will be a json string representing an array of objects
    List<Map> groups = new ArrayList<Map>();
    for (String id : groupIds) {
      Item dbData = super.getItem(new GetItemSpec().withPrimaryKey(super.getPrimaryKeyIndex(), id));
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

  public ResultStatus createNewGroup(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();

    return resultStatus;
  }

  public ResultStatus editGroup(final Map<String, Object> jsonMap) {
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

        if (this.editInputIsValid(groupId, activeUser, groupCreator, members,
            defaultPollPassPercent, defaultPollDuration)) {
          final Map<String, Object> dbGroupDataMap = super.getItemByPrimaryKey(groupId).asMap();

          if (this.editInputHasPermissions(dbGroupDataMap, activeUser, groupCreator)) {
            //all validation is successful, build transaction actions
            final List<TransactWriteItem> actions = new ArrayList<TransactWriteItem>();

            this.updateMembersMapForInsertion(members); // this implicitly changes data

            final Map<String, AttributeValue> groupReplacementItem = new HashMap<String, AttributeValue>();
            groupReplacementItem.put(GROUP_ID, new AttributeValue(groupId));
            groupReplacementItem.put(GROUP_NAME, new AttributeValue(groupName));
            groupReplacementItem.put(ICON, new AttributeValue(icon));
            groupReplacementItem.put(GROUP_CREATOR, new AttributeValue(groupCreator));
            groupReplacementItem
                .put(MEMBERS, new AttributeValue().withM(this.getAttributeValueMapping(members)));
            groupReplacementItem.put(CATEGORIES,
                new AttributeValue().withM(this.getAttributeValueMapping(categories)));
            groupReplacementItem
                .put(DEFAULT_POLL_PASS_PERCENT,
                    new AttributeValue().withN(defaultPollPassPercent.toString()));
            groupReplacementItem
                .put(DEFAULT_POLL_DURATION,
                    new AttributeValue().withN(defaultPollDuration.toString()));

            final Put groupReplacement = new Put()
                .withTableName(this.getTableName())
                .withItem(groupReplacementItem);

            actions.add(new TransactWriteItem().withPut(groupReplacement));

            //add other actions to this transaction for adding/removing groups to/from the users/categories table
            this.addActionsForUsersDelta(actions, (Map<String, Object>) dbGroupDataMap.get(MEMBERS),
                members);
            this.addActionsForCategoriesDelta(actions,
                (Map<String, Object>) dbGroupDataMap.get(CATEGORIES), categories);

            super.executeWriteTransaction(
                new TransactWriteItemsRequest().withTransactItems(actions));

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
  
  public ResultStatus makeEvent(final Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(EVENT_ID, EVENT_NAME, CATEGORY_ID, CREATED_DATE_TIME, EVENT_START_DATE_TIME, TYPE, POLL_DURATION,
                POLL_PASS_PERCENT, OPTED_IN, RequestFields.ACTIVE_USER, GROUP_ID);

    if (IOStreamsHelper.allKeysContained(jsonMap, requiredKeys)) {
      try {
        final String eventId = (String) jsonMap.get(EVENT_ID);
        final String eventName = (String) jsonMap.get(EVENT_NAME);
        final String categoryId = (String) jsonMap.get(CATEGORY_ID);
        final String createdDateTime = (String) jsonMap.get(CREATED_DATE_TIME);
        final String eventStartDateTime = (String) jsonMap.get(EVENT_START_DATE_TIME);
        final Integer type = (Integer) jsonMap.get(TYPE);
        final Integer pollDuration = (Integer) jsonMap.get(POLL_DURATION);
        final Integer pollPassPercent = (Integer) jsonMap.get(POLL_PASS_PERCENT);
        final Map<String, Object> optedIn = (Map<String, Object>) jsonMap.get(OPTED_IN);
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(GROUP_ID);

        if (this.makeEventInputIsValid(eventId, activeUser, groupId, categoryId, pollDuration,pollPassPercent)) {
          final Map<String,Object> eventMap = new HashMap<String,Object>();
          
          eventMap.put(CATEGORY_ID, categoryId);
          eventMap.put(EVENT_NAME, eventName);
          eventMap.put(CREATED_DATE_TIME, createdDateTime);
          eventMap.put(EVENT_START_DATE_TIME, eventStartDateTime);
          eventMap.put(TYPE, type);
          eventMap.put(POLL_DURATION, pollDuration);
          eventMap.put(POLL_PASS_PERCENT, pollPassPercent);
          eventMap.put(OPTED_IN, optedIn);
          
          String updateExpression = "set " + EVENTS + ".#eventId = :map";
          NameMap nameMap = new NameMap().with("#eventId", eventId);
          ValueMap valueMap = new ValueMap().withMap(":map", eventMap);

          UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(super.getPrimaryKeyIndex(), groupId)
            .withNameMap(nameMap)
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);

          super.updateItem(updateItemSpec);

          resultStatus = new ResultStatus(true, "event added successfully!");      
        } else {
          resultStatus.resultMessage = "Invalid request, bad input.";
        }
      } catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request in manager. Message:"+e;
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
      Item user = this.usersManager.getUser(username);

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

  private boolean makeEventInputIsValid(final String eventId, final String activeUser, final String groupId,
                                        final String categoryId, final Integer pollDuration,
                                        final Integer pollPassPercent) {
    boolean isValid = true;
    
    if (StringUtils.isNullOrEmpty(eventId) || StringUtils.isNullOrEmpty(activeUser) ||
        StringUtils.isNullOrEmpty(groupId) || StringUtils.isNullOrEmpty(categoryId)) {
      isValid = false;
    }
    
    if (pollPassPercent < 0 || pollPassPercent > 100) {
     isValid = false; 
    }
    
    if (pollDuration <=0 || pollDuration > 10000) {
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

  private void addActionsForUsersDelta(final Collection<TransactWriteItem> actions,
      final Map<String, Object> oldMembers, final Map<String, Object> newMembers) {
    final List<String> newUsernames = new ArrayList<String>();
    final List<String> removedUsernames = new ArrayList<String>();

    //loop over the input maps, look at their keys (the usernames) to calculate the above lists

    //once the lists are calculated, crated update/delete statements for the user's table accordingly
    //add these to the 'actions' Collection
  }

  private void addActionsForCategoriesDelta(final Collection<TransactWriteItem> actions,
      final Map<String, Object> oldCategories, final Map<String, Object> newCategories) {
    final List<String> newCategoryIds = new ArrayList<String>();
    final List<String> removedCategoryIds = new ArrayList<String>();

    //loop over the input maps, look at their keys (the categoryIds) to calculate the above lists

    //once the lists are calculated, crated update/delete statements for the categories's table accordingly
    //add these to the 'actions' Collection
  }

  public List<String> getAllCategoryIds(String groupId) {
    Item dbData = super
        .getItem(new GetItemSpec().withPrimaryKey(super.getPrimaryKeyIndex(), groupId));

    Map<String, Object> dbDataMap = dbData.asMap(); // specific group record as a map
    Map<String, String> categoryMap = (Map<String, String>) dbDataMap.get(CATEGORIES);

    return new ArrayList<String>(categoryMap.keySet());
  }

  // This function is called when a category is deleted and updates each item in the groups table
  // that was linked to the category accordingly.
  public void removeCategoryFromGroups(List<String> groupIds, String categoryId) {
    try {
      final String updateExpression = "remove Categories.#categoryId";
      final NameMap nameMap = new NameMap().with("#categoryId", categoryId);
      UpdateItemSpec updateItemSpec;

      for (final String groupId : groupIds) {
        updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(super.getPrimaryKeyIndex(), groupId)
            .withNameMap(nameMap)
            .withUpdateExpression(updateExpression);
        super.updateItem(updateItemSpec);
      }
    } catch (Exception e) {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
    }
  }

  private Map<String, AttributeValue> getAttributeValueMapping(final Map<String, Object> inputMap) {
    final Map<String, AttributeValue> returnMapping = new HashMap<String, AttributeValue>();
    for (final String key : inputMap.keySet()) {
      returnMapping.put(key, new AttributeValue((String) inputMap.get(key)));
    }

    return returnMapping;
  }
}
