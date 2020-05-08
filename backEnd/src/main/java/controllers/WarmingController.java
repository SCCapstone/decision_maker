package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.WarmingHandler;
import java.util.Map;
import javax.inject.Inject;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class WarmingController implements ApiRequestController {

  @Inject
  public WarmingHandler warmingHandler;

  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "WarmingController.processApiRequest";

    ResultStatus resultStatus = new ResultStatus();

    try {
      Injector.getInjector(metrics).inject(this);
      resultStatus = this.warmingHandler.handle();
    } catch (final Exception e) {
      metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    return resultStatus;
  }
}
