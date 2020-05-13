package handlers;

import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import java.util.ArrayList;
import java.util.List;
import managers.DbAccessManager;
import models.Group;
import models.User;
import models.UserGroup;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.UpdateItemData;

public class RejoinGroupHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;
  private Metrics metrics;

  public RejoinGroupHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method inserts a user back into a group they had previously left.
   *
   * @param activeUser The user that made the api request to rejoin a group.
   * @param groupId    The id of the group that the user is trying to rejoin.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String groupId) {
    final String classMethod = "RejoinGroupHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final User user = this.dbAccessManager.getUser(activeUser);
      final Group group = this.dbAccessManager.getGroup(groupId);

      if (user.getGroupsLeft().containsKey(groupId) && group.getMembersLeft()
          .containsKey(activeUser)) {
        final List<TransactWriteItem> actions = new ArrayList<>();

        //remove the user from the members left and add back to the members
        final UpdateItemData groupUpdate = new UpdateItemData(groupId,
            DbAccessManager.GROUPS_TABLE_NAME)
            .withUpdateExpression("remove " + Group.MEMBERS_LEFT + ".#username set " + Group.MEMBERS
                + ".#username = :memberMap")
            .withNameMap(new NameMap().with("#username", activeUser))
            .withValueMap(new ValueMap().withMap(":memberMap", user.asMember().asMap()));

        // remove this group from the GroupsLeft and add it back to the Groups attribute
        final UpdateItemData userUpdate = new UpdateItemData(activeUser,
            DbAccessManager.USERS_TABLE_NAME)
            .withUpdateExpression("remove " + User.GROUPS_LEFT + ".#groupId set " + User.GROUPS
                + ".#groupId = :groupMap")
            .withNameMap(new NameMap().with("#groupId", groupId))
            .withValueMap(
                new ValueMap().withMap(":groupMap", UserGroup.fromNewGroup(group).asMap()));

        actions.add(new TransactWriteItem().withUpdate(groupUpdate.asUpdate()));
        actions.add(new TransactWriteItem().withUpdate(userUpdate.asUpdate()));

        this.dbAccessManager.executeWriteTransaction(actions);

        resultStatus = new ResultStatus(true, "Group rejoined successfully.");
      } else {
        this.metrics.logWithBody(
            new ErrorDescriptor<>(classMethod, "User did not leave group, cannot rejoin"));
        resultStatus = ResultStatus.failure("Error: User did not leave this group, cannot rejoin.");
      }
    } catch (Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
