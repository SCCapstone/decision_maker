package dbMaintenance.cronJobs.controllers;

import controllers.ApiRequestController;
import dbMaintenance.cronJobs.handlers.DetachedRatingsRemovalHandler;
import dbMaintenance.modules.MaintenanceInjector;
import exceptions.MissingApiRequestKeyException;
import java.util.Map;
import javax.inject.Inject;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class DetachedRatingsRemovalController implements ApiRequestController {

  @Inject
  public DetachedRatingsRemovalHandler detachedRatingsRemovalHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "DetachedRatingsRemovalController.processApiRequest";

    ResultStatus resultStatus;

    try {
      MaintenanceInjector.getInjector(metrics).inject(this);
      resultStatus = this.detachedRatingsRemovalHandler.handle();
    } catch (final Exception e) {
      metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    return resultStatus;
  }
}
