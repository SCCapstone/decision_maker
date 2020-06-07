package dbMaintenance.controllers;

import dbMaintenance.handlers.UnkeyUserRatingsByVersionHandler;
import dbMaintenance.modules.MaintenanceInjector;
import controllers.ApiRequestController;
import exceptions.MissingApiRequestKeyException;
import java.util.Map;
import javax.inject.Inject;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class UnkeyUserRatingsByVersionController implements ApiRequestController {

  @Inject
  public UnkeyUserRatingsByVersionHandler unkeyUserRatingsByVersionHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "UnkeyUserRatingsByVersionController.processApiRequest";

    ResultStatus resultStatus;

    try {
      MaintenanceInjector.getInjector(metrics).inject(this);
      resultStatus = this.unkeyUserRatingsByVersionHandler.handle();
    } catch (final Exception e) {
      metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    return resultStatus;
  }
}
