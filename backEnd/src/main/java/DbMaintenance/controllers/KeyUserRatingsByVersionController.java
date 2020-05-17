package DbMaintenance.controllers;

import DbMaintenance.handlers.KeyUserRatingsByVersionHandler;
import DbMaintenance.modules.MaintenanceInjector;
import controllers.ApiRequestController;
import exceptions.MissingApiRequestKeyException;
import java.util.Map;
import javax.inject.Inject;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class KeyUserRatingsByVersionController implements ApiRequestController {

  @Inject
  public KeyUserRatingsByVersionHandler keyUserRatingsByVersionHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "KeyUserRatingsByVersionController.processApiRequest";

    ResultStatus resultStatus;

    try {
      MaintenanceInjector.getInjector(metrics).inject(this);
      resultStatus = this.keyUserRatingsByVersionHandler.handle();
    } catch (final Exception e) {
      metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    return resultStatus;
  }
}
