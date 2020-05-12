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

public class OptUserInOutHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;
  private Metrics metrics;

  @Inject
  public OptUserInOutHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method allows a user to opt in or out of an event.
   *
   * @param activeUser    The user that made the api request to opt in/out of the event.
   * @param groupId       The id of the group that the event belongs to.
   * @param eventId       The id of the event being opted in/out of.
   * @param participating A boolean value corresponding to the in/out of the opt.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String groupId, final String eventId,
      final Boolean participating) {
    final String classMethod = "OptUserInOutHandler.handle";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final User user = new User(DatabaseManagers.USERS_MANAGER.getMapByPrimaryKey(activeUser));

      //only allow the user to opt in/out if they're a member of the group
      if (user.getGroups().containsKey(groupId)) {
        String updateExpression;
        ValueMap valueMap = null;

        if (participating) { // add the user to the optIn
          updateExpression =
              "set " + Group.EVENTS + ".#eventId." + Event.OPTED_IN + ".#username = :userMap";
          valueMap = new ValueMap()
              .withMap(":userMap", user.asMember().asMap());
        } else {
          updateExpression =
              "remove " + Group.EVENTS + ".#eventId." + Event.OPTED_IN + ".#username";
        }

        final NameMap nameMap = new NameMap()
            .with("#eventId", eventId)
            .with("#username", activeUser);

        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap)
            .withValueMap(valueMap);

        this.dbAccessManager.updateGroup(groupId, updateItemSpec);

        resultStatus = ResultStatus.successful("Opted in/out successfully");
      } else {
        metrics.logWithBody(new WarningDescriptor<>(classMethod, "User not in group"));
        resultStatus = ResultStatus.failure("Error: User not in group.");
      }
    } catch (Exception e) {
      metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
