package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.GroupsManager;
import handlers.MarkAllEventsSeenHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class MarkAllEventsSeenController implements ApiRequestController {

  @Inject
  public MarkAllEventsSeenHandler markAllEventsSeenHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "MarkAllEventsSeenController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, GroupsManager.GROUP_ID);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(GroupsManager.GROUP_ID);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.markAllEventsSeenHandler.handle(activeUser, groupId);
      } catch (final Exception e) {
        resultStatus = ResultStatus.failure("Exception in " + classMethod);
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}
