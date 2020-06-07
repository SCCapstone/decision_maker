package dbMaintenance.controllers;

import controllers.ApiRequestController;
import dbMaintenance.handlers.AddDurationsToUserSettingsHandler;
import dbMaintenance.modules.MaintenanceInjector;
import exceptions.MissingApiRequestKeyException;
import java.util.Map;
import javax.inject.Inject;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class AddDurationsToUserSettingsController implements ApiRequestController {

  @Inject
  public AddDurationsToUserSettingsHandler addDurationsToUserSettingsHandler;

  @Override
  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "AddDurationsToUserSettingsController.processApiRequest";

    ResultStatus resultStatus;

    try {
      MaintenanceInjector.getInjector(metrics).inject(this);
      resultStatus = this.addDurationsToUserSettingsHandler.handle();
    } catch (final Exception e) {
      metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    return resultStatus;
  }
}
