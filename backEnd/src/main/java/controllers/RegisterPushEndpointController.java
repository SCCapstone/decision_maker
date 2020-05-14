package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.RegisterPushEndpointHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class RegisterPushEndpointController implements ApiRequestController {

  @Inject
  public RegisterPushEndpointHandler registerPushEndpointHandler;

  @Override
  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "RegisterPushEndpointController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, RequestFields.DEVICE_TOKEN);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String deviceToken = (String) jsonMap.get(RequestFields.DEVICE_TOKEN);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.registerPushEndpointHandler.handle(activeUser, deviceToken);
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
