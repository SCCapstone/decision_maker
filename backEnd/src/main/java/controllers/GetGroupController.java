package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.GetGroupHandler;
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

public class GetGroupController implements ApiRequestController {

  @Inject
  public GetGroupHandler getGroupHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "GetGroupController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, Group.GROUP_ID, RequestFields.BATCH_NUMBER);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(Group.GROUP_ID);
        final Integer batchNumber = (Integer) jsonMap.get(RequestFields.BATCH_NUMBER);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.getGroupHandler.handle(activeUser, groupId, batchNumber);
      } catch (final Exception e) {
        resultStatus = ResultStatus.failure("Error: Unable to parse request.");
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}
