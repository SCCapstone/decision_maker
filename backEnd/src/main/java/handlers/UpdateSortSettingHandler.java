package handlers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class UpdateSortSettingHandler implements ApiRequestHandler {

  private final DbAccessManager dbAccessManager;
  private final Metrics metrics;

  @Inject
  public UpdateSortSettingHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method is used to update the the group sort settings associated with a specific user.
   *
   * @param activeUser   The user making the api request that has just updated a sort settings.
   * @param newGroupSort The new sort integer for group sort. This value decodes to a
   *                     method/direction of sorting on the front end.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handleGroupSortUpdate(final String activeUser, final Integer newGroupSort) {
    final String classMethod = "UpdateSortSettingHandler.handleGroupSortUpdate";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final User user = this.dbAccessManager.getUser(activeUser);
      user.getAppSettings().setGroupSort(newGroupSort); // this throws if invalid

      final String updateExpression = "set " + User.APP_SETTINGS + " = :settingsMap";
      final ValueMap valueMap = new ValueMap()
          .withMap(":settingsMap", user.getAppSettings().asMap());

      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withValueMap(valueMap);

      this.dbAccessManager.updateUser(activeUser, updateItemSpec);
      resultStatus = ResultStatus.successful("Sort value updated successfully.");
    } catch (Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This method is used to update the the category sort settings associated with a specific user.
   *
   * @param activeUser      The user making the api request that has just updated a sort settings.
   * @param newCategorySort The new sort integer for category sort. This value decodes to a
   *                        method/direction of sorting on the front end.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handleCategorySortUpdate(final String activeUser,
      final Integer newCategorySort) {
    final String classMethod = "UpdateSortSettingHandler.handleCategorySortUpdate";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final User user = this.dbAccessManager.getUser(activeUser);
      user.getAppSettings().setCategorySort(newCategorySort); // this throws if invalid

      final String updateExpression = "set " + User.APP_SETTINGS + " = :settingsMap";
      final ValueMap valueMap = new ValueMap()
          .withMap(":settingsMap", user.getAppSettings().asMap());

      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withValueMap(valueMap);

      this.dbAccessManager.updateUser(activeUser, updateItemSpec);
      resultStatus = ResultStatus.successful("Sort value updated successfully.");
    } catch (Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
