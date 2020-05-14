package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.MarkEventAsSeenHandler;
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

public class MarkEventAsSeenController implements ApiRequestController {

  @Inject
  public MarkEventAsSeenHandler markEventAsSeenHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "MarkEventAsSeenController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, Group.GROUP_ID, RequestFields.EVENT_ID);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(Group.GROUP_ID);
        final String eventId = (String) jsonMap.get(RequestFields.EVENT_ID);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.markEventAsSeenHandler.handle(activeUser, groupId, eventId);
      } catch (final Exception e) {
        resultStatus = ResultStatus.failure("Exception in " + classMethod);
        metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}
