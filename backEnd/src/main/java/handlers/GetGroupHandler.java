package handlers;

import javax.inject.Inject;
import managers.DbAccessManager;
import models.Group;
import models.GroupForApiResponse;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class GetGroupHandler implements ApiRequestHandler {

  private final DbAccessManager dbAccessManager;
  private final Metrics metrics;

  @Inject
  public GetGroupHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method gets and returns a group item. It should be noted that returned groups only get a
   * limited number of events which is why the batch number is needed. This tells which events we
   * want information on.
   *
   * @param activeUser  This is the username of the user making the api request.
   * @param groupId     This is the id of the group being gotten.
   * @param batchNumber This is the event batch that we're limiting the returned data to.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String groupId,
      final Integer batchNumber) {
    final String classMethod = "GetGroupHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Group group = this.dbAccessManager.getGroup(groupId);

      //the user should not be able to retrieve info from the group if they are not a member
      if (group.getMembers().containsKey(activeUser)) {
        final GroupForApiResponse groupForApiResponse = new GroupForApiResponse(group,
            batchNumber);

        resultStatus = new ResultStatus(true,
            JsonUtils.convertObjectToJson(groupForApiResponse.asMap()));
      } else {
        resultStatus = ResultStatus.failure("Error: user is not a member of the group.");
        this.metrics.logWithBody(new WarningDescriptor<>(classMethod, "User not in group"));
      }
    } catch (final Exception e) {
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}