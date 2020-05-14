package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.LeaveGroupHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import models.Group;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class LeaveGroupController implements ApiRequestController {

  @Inject
  public LeaveGroupHandler leaveGroupHandler;

  @Override
  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "LeaveGroupController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays.asList(RequestFields.ACTIVE_USER, Group.GROUP_ID);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String groupId = (String) jsonMap.get(Group.GROUP_ID);
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.leaveGroupHandler.handle(activeUser, groupId);
      } catch (final Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus = ResultStatus.failure("Exception in " + classMethod);
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}
