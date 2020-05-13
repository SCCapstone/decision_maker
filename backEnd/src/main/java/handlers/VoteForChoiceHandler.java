package handlers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.Event;
import models.Group;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class VoteForChoiceHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;
  private Metrics metrics;

  @Inject
  public VoteForChoiceHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method allows a user to vote for a tentative choice on an event.
   *
   * @param activeUser The user that made the api request that is voting for a choice.
   * @param groupId    The id of the group that contains the event that is being voted on.
   * @param eventId    The id of the event that contains the choice that is being voted on.
   * @param choiceId   The id of the choice that is being voted on.
   * @param voteValue  0 or 1 depending on the vote value. Defaults to 0 if not 1.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String groupId, final String eventId,
      final String choiceId, Integer voteValue) {
    final String classMethod = "VoteForChoiceHandler.handle";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final User user = new User(DatabaseManagers.USERS_MANAGER.getMapByPrimaryKey(activeUser));

      //only allow the user to vote if they are in the group
      if (user.getGroups().containsKey(groupId)) {
        if (voteValue != 1) {
          voteValue = 0;
        }

        final String updateExpression =
            "set " + Group.EVENTS + ".#eventId." + Event.VOTING_NUMBERS + ".#choiceId." +
                "#activeUser = :voteValue";
        final ValueMap valueMap = new ValueMap().withInt(":voteValue", voteValue);
        final NameMap nameMap = new NameMap()
            .with("#eventId", eventId)
            .with("#choiceId", choiceId)
            .with("#activeUser", activeUser);

        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap)
            .withValueMap(valueMap);

        this.dbAccessManager.updateGroup(groupId, updateItemSpec);
        resultStatus = new ResultStatus(true, "Voted yes/no successfully!");
      } else {
        resultStatus = ResultStatus.failure("Error: user not in group.");
        metrics.logWithBody(new WarningDescriptor<>(classMethod, "User not in group."));
      }
    } catch (Exception e) {
      resultStatus = ResultStatus.failure("Error: unable to parse request in manager.");
      metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
