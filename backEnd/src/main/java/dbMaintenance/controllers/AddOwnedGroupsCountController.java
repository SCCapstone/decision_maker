package dbMaintenance.controllers;

import controllers.ApiRequestController;
import dbMaintenance.handlers.AddOwnedGroupsCountHandler;
import dbMaintenance.modules.MaintenanceInjector;
import exceptions.MissingApiRequestKeyException;
import java.util.Map;
import javax.inject.Inject;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class AddOwnedGroupsCountController implements ApiRequestController {

  @Inject
  public AddOwnedGroupsCountHandler addOwnedGroupsCountHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "AddOwnedGroupsCountController.processApiRequest";

    ResultStatus resultStatus;

    try {
      MaintenanceInjector.getInjector(metrics).inject(this);
      resultStatus = this.addOwnedGroupsCountHandler.handle();
    } catch (final Exception e) {
      metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    return resultStatus;
  }
}
