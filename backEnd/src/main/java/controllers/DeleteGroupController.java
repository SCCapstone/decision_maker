package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.DeleteGroupHandler;
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

public class DeleteGroupController implements ApiRequestController {

  @Inject
  public DeleteGroupHandler deleteGroupHandler;

  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "DeleteGroupController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, Group.GROUP_ID);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(Group.GROUP_ID);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.deleteGroupHandler.handle(activeUser, groupId);
      } catch (final Exception e) {
        metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
        resultStatus = ResultStatus.failure("Exception in " + classMethod);
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}
