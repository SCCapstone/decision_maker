package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
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
import utilities.ExceptionHelper;
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

    if (IOStreamsHelper.allKeysContainted(jsonMap,requiredKeys)) {
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
                .put(DEFAULT_POLL_PASS_PERCENT, new AttributeValue().withN(defaultPollPassPercent.toString()));
            groupReplacementItem
                .put(DEFAULT_POLL_DURATION, new AttributeValue().withN(defaultPollDuration.toString()));

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
        resultStatus.resultMessage =
            "Error: Unable to parse request";
      }
    } else {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    return resultStatus;
  }

  private boolean editInputIsValid(final String groupId, final String activeUser,
      final String groupCreator, final Map<String, Object> members, Integer defaultPollPassPercent,
      Integer defaultPollDuration) {
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

    if (defaultPollDuration < 0 || defaultPollDuration > 100) {
      isValid = false;
    }

    return isValid;
  }

  private boolean editInputHasPermissions(final Map<String, Object> dbGroupDataMap,
      final String activeUser, final String groupCreator) {
    //the group create is not changed or it is changed and the active user is the current creator
    boolean hasPemission = true;

    if (!dbGroupDataMap.get(GROUP_CREATOR).equals(groupCreator) && !dbGroupDataMap
        .get(GROUP_CREATOR).equals(activeUser)) {
      hasPemission = false;
    }

    return hasPemission;
  }

  private void addActionsForUsersDelta(final Collection<TransactWriteItem> actions,
      final Map<String, Object> oldMembers, final Map<String, Object> newMembers) {
    List<String> newUsernames = new ArrayList<String>();
    List<String> removedUsernames = new ArrayList<String>();

    //loop over the input maps, look at their keys (the usernames) to calculate the above lists

    //once the lists are calculated, crated update/delete statements for the user's table accordingly
    //add these to the 'actions' Collection
  }

  private void addActionsForCategoriesDelta(final Collection<TransactWriteItem> actions,
      final Map<String, Object> oldCategories, final Map<String, Object> newCategories) {
    List<String> newCategoryIds = new ArrayList<String>();
    List<String> removedCategoryIds = new ArrayList<String>();

    //loop over the input maps, look at their keys (the categoryIds) to calculate the above lists

    //once the lists are calculated, crated update/delete statements for the categories's table accordingly
    //add these to the 'actions' Collection
  }

  private Map<String, AttributeValue> getAttributeValueMapping(final Map<String, Object> inputMap) {
    final Map<String, AttributeValue> returnMapping = new HashMap<String, AttributeValue>();
    for (String key : inputMap.keySet()) {
      returnMapping.put(key, new AttributeValue((String) inputMap.get(key)));
    }

    return returnMapping;
  }
}
