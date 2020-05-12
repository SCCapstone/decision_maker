package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.OptUserInOutHandler;
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

public class OptUserInOutController implements ApiRequestController {

  @Inject
  public OptUserInOutHandler optUserInOutHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "GroupsManager.optInOutOfEvent";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, Group.GROUP_ID, RequestFields.EVENT_ID,
            RequestFields.PARTICIPATING);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(Group.GROUP_ID);
        final String eventId = (String) jsonMap.get(RequestFields.EVENT_ID);
        final Boolean participating = (Boolean) jsonMap.get(RequestFields.PARTICIPATING);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.optUserInOutHandler.handle(activeUser, groupId, eventId, participating);
      } catch (Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus = ResultStatus.failure("Exception in " + classMethod);
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}
