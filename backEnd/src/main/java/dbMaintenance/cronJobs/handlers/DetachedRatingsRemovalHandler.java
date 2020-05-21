package dbMaintenance.cronJobs.handlers;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import dbMaintenance.managers.MaintenanceDbAccessManager;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class DetachedRatingsRemovalHandler {

  private MaintenanceDbAccessManager maintenanceDbAccessManager;
  private Metrics metrics;

  @Inject
  public DetachedRatingsRemovalHandler(final MaintenanceDbAccessManager maintenanceDbAccessManager,
      final Metrics metrics) {
    this.maintenanceDbAccessManager = maintenanceDbAccessManager;
    this.metrics = metrics;
  }

  /**
   * Category ratings become detached when a category is deleted. We do not currently have a clean
   * way of knowing which users have ratings for a category when deleting a category. So in light of
   * that, we simply check all the user's ratings periodically to cleanup bad data.
   *
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle() {
    final String classMethod = "DetachedRatingsRemovalHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus = ResultStatus
        .successful("Detached category ratings removed successfully.");

    try {
      final Iterator<Item> tableItems = this.maintenanceDbAccessManager.scanUsersTable();

      while (tableItems.hasNext()) {
        final Item userItem = tableItems.next();

        try {
          final String username = userItem.getString(User.USERNAME);

          final StringBuilder updateExpressionBuilder = new StringBuilder();
          final NameMap nameMap = new NameMap();

          final Map<String, Object> oldCategoryRatingsMap = userItem.getMap(User.CATEGORY_RATINGS);

          int i = 0;
          for (final String categoryId : oldCategoryRatingsMap.keySet()) {
            try {
              final Item categoryItem = this.maintenanceDbAccessManager.getCategoryItem(categoryId);

              if (categoryItem == null) {
                //The category does not exist, remove it's ratings from the user item
                final String categoryIdName = "#categoryId" + i;
                final String dotCategoryIdName = "." + categoryIdName;

                if (updateExpressionBuilder.length() == 0) {
                  updateExpressionBuilder.append("remove ").append(User.CATEGORY_RATINGS)
                      .append(dotCategoryIdName);
                } else {
                  updateExpressionBuilder.append(", ").append(User.CATEGORY_RATINGS)
                      .append(dotCategoryIdName);
                }

                nameMap.with(categoryIdName, categoryId);
              }
            } catch (final Exception e) {
              this.metrics.log(new ErrorDescriptor<>(username + " " + categoryId, classMethod, e));
              resultStatus = ResultStatus.failure("Exception in " + classMethod);
            }

            i++;
          }

          if (updateExpressionBuilder.length() > 0) {
            final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression(updateExpressionBuilder.toString())
                .withNameMap(nameMap);

            this.maintenanceDbAccessManager.updateUser(username, updateItemSpec);
          }
        } catch (final Exception e) {
          this.metrics.log(new ErrorDescriptor<>(userItem.get(User.USERNAME), classMethod, e));
          resultStatus = ResultStatus.failure("Exception in " + classMethod);
        }
      }
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
