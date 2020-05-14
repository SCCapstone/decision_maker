package handlers;

import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.Group;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.UpdateItemData;

public class LeaveGroupHandler implements ApiRequestHandler {

  private final DbAccessManager dbAccessManager;
  private final Metrics metrics;

  @Inject
  public LeaveGroupHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method allows a user to remove themselves from a group. Anyone who leaves a group via this
   * method can't be added back to the group by someone else. They have to rejoin the group
   * themselves.
   *
   * @param activeUser The user that made the api request and it trying to leave a group.
   * @param groupId    This id of the group being left.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String groupId) {
    final String classMethod = "LeaveGroupHandler.handle";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Group group = this.dbAccessManager.getGroup(groupId);

      Optional<String> errorMessage = this.leaveGroupIsValid(group, activeUser);
      if (!errorMessage.isPresent()) {
        String updateExpression =
            "remove " + Group.MEMBERS + ".#username set " + Group.MEMBERS_LEFT
                + ".#username = :memberLeftMap";

        NameMap nameMap = new NameMap()
            .with("#username", activeUser);
        ValueMap valueMap = new ValueMap()
            .withBoolean(":memberLeftMap", true);

        UpdateItemData groupUpdate = new UpdateItemData(groupId, DbAccessManager.GROUPS_TABLE_NAME)
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap)
            .withValueMap(valueMap);

        // remove group from Groups attribute of the active user item and add to the GroupsLeft
        updateExpression =
            "remove " + User.GROUPS + ".#groupId set " + User.GROUPS_LEFT + ".#groupId = :groupMap";
        nameMap = new NameMap().with("#groupId", groupId);
        valueMap = new ValueMap()
            .withMap(":groupMap", new HashMap<String, Object>() {{
              put(Group.GROUP_NAME, group.getGroupName());
              put(Group.ICON, group.getIcon());
            }});

        UpdateItemData userUpdate = new UpdateItemData(activeUser, DbAccessManager.USERS_TABLE_NAME)
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap)
            .withValueMap(valueMap);

        final List<TransactWriteItem> actions = new ArrayList<>();
        actions.add(new TransactWriteItem().withUpdate(groupUpdate.asUpdate()));
        actions.add(new TransactWriteItem().withUpdate(userUpdate.asUpdate()));
        this.dbAccessManager.executeWriteTransaction(actions);

        resultStatus = new ResultStatus(true, "Group left successfully.");
      } else {
        metrics.logWithBody(new ErrorDescriptor<>(classMethod, errorMessage.get()));
        resultStatus = ResultStatus.failure(errorMessage.get());
      }
    } catch (Exception e) {
      metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private Optional<String> leaveGroupIsValid(final Group group, final String activeUser) {
    String errorMessage = null;

    if (group.getGroupCreator().equals(activeUser)) {
      errorMessage = this
          .getUpdatedErrorMessage(errorMessage, "Error: the group creator cannot leave the group.");
    }

    if (!group.getMembers().containsKey(activeUser)) {
      errorMessage = this
          .getUpdatedErrorMessage(errorMessage, "Error: the user is not in the group.");
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
}
