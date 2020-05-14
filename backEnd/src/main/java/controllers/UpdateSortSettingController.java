package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.UpdateSortSettingHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import models.AppSettings;
import models.User;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class UpdateSortSettingController implements ApiRequestController {

  @Inject
  public UpdateSortSettingHandler updateSortSettingHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "UpdateSortSettingController.processApiRequest";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays.asList(RequestFields.ACTIVE_USER);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        Injector.getInjector(metrics).inject(this);
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        if (jsonMap.containsKey(AppSettings.GROUP_SORT)) {
          final Integer newGroupSort = (Integer) jsonMap.get(AppSettings.GROUP_SORT);
          resultStatus = this.updateSortSettingHandler
              .handleGroupSortUpdate(activeUser, newGroupSort);
        } else if (jsonMap.containsKey(AppSettings.CATEGORY_SORT)) {
          final Integer newCategorySort = (Integer) jsonMap.get(AppSettings.CATEGORY_SORT);
          resultStatus = this.updateSortSettingHandler
              .handleCategorySortUpdate(activeUser, newCategorySort);
        } else {
          resultStatus = ResultStatus.failure("Error: No sort key entered.");
        }
      } catch (Exception e) {
        metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
        resultStatus = ResultStatus.failure("Exception in " + classMethod);
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
