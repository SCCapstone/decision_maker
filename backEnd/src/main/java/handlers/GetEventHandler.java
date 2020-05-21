package handlers;

import javax.inject.Inject;
import managers.DbAccessManager;
import models.Group;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class GetEventHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;
  private Metrics metrics;

  @Inject
  public GetEventHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This function is used to get the event map for a specific event within a group.
   *
   * @param activeUser The username of the user making the api request.
   * @param groupId    The id of the group that the event is associated with.
   * @param eventId    The id of the event to get the data of.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String groupId, final String eventId) {
    final String classMethod = "GetEventHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Group group = this.dbAccessManager.getGroup(groupId);

      if (group.getMembers().containsKey(activeUser)) {
        resultStatus = ResultStatus
            .successful(JsonUtils.convertObjectToJson(group.getEventsMap().get(eventId)));
      } else {
        this.metrics.logWithBody(new WarningDescriptor<>(classMethod, "User not in group"));
        resultStatus = ResultStatus.failure("Error: user is not a member of the group.");
      }
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
