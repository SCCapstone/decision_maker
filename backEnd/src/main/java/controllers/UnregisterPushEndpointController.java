package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.UnregisterPushEndpointHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class UnregisterPushEndpointController implements ApiRequestController {

  @Inject
  public UnregisterPushEndpointHandler unregisterPushEndpointHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "UnregisterPushEndpointController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays.asList(RequestFields.ACTIVE_USER);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.unregisterPushEndpointHandler.handle(activeUser);
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
