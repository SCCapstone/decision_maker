package dbMaintenance.controllers;

import controllers.ApiRequestController;
import dbMaintenance.handlers.AddCategoryCreatorToGroupHandler;
import dbMaintenance.modules.MaintenanceInjector;
import exceptions.MissingApiRequestKeyException;
import java.util.Map;
import javax.inject.Inject;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class AddCategoryCreatorToGroupController implements ApiRequestController {

  @Inject
  public AddCategoryCreatorToGroupHandler addCategoryCreatorToGroupHandler;

  @Override
  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "AddCategoryCreatorToGroupController.processApiRequest";

    ResultStatus resultStatus;

    try {
      MaintenanceInjector.getInjector(metrics).inject(this);
      resultStatus = this.addCategoryCreatorToGroupHandler.handle();
    } catch (final Exception e) {
      metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    return resultStatus;
  }
}
