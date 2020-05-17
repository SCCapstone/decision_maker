package DbMaintenance.handlers;

import DbMaintenance.Managers.MaintenanceDbAccessManager;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.google.common.collect.ImmutableMap;
import handlers.ApiRequestHandler;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import models.Category;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class KeyUserRatingsByVersionHandler implements ApiRequestHandler {

  private final MaintenanceDbAccessManager maintenanceDbAccessManager;
  private final Metrics metrics;

  @Inject
  public KeyUserRatingsByVersionHandler(final MaintenanceDbAccessManager maintenanceDbAccessManager,
      final Metrics metrics) {
    this.maintenanceDbAccessManager = maintenanceDbAccessManager;
    this.metrics = metrics;
  }

  public ResultStatus handle() {
    final String classMethod = "KeyUserRatingsByVersionHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus = ResultStatus
        .successful("User Ratings keyed by version successfully.");

    try {
      final Iterator<Item> tableItems = this.maintenanceDbAccessManager.scanUsersTable();

      while (tableItems.hasNext()) {
        final Item userItem = tableItems.next();

        try {
          final String username = userItem.getString(User.USERNAME);
          final Map<String, Object> oldUserRatings = userItem.getMap(User.CATEGORY_RATINGS);

          for (final String categoryId : oldUserRatings.keySet()) {
            try {
              final Item categoryItem = this.maintenanceDbAccessManager.getCategoryItem(categoryId);

              if (categoryItem != null) {
                //We are assuming the ratings are saved for the newest version. With this being the
                // case, we simply get the newest version and map the current rating map one level
                // below that.

                final Category category = new Category(categoryItem);

                final Map<String, Object> choiceToRatings = (Map<String, Object>) oldUserRatings
                    .get(categoryId);

                final Map versionToRatingsMap = ImmutableMap
                    .of(category.getVersion(), choiceToRatings);

                final String updateExpression =
                    "Set " + User.CATEGORY_RATINGS + ".#categoryId = :versionMap";
                final NameMap nameMap = new NameMap().with("#categoryId", categoryId);
                final ValueMap valueMap = new ValueMap()
                    .withMap(":versionMap", versionToRatingsMap);

                final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withUpdateExpression(updateExpression)
                    .withNameMap(nameMap)
                    .withValueMap(valueMap);

                this.maintenanceDbAccessManager.updateUser(username, updateItemSpec);
              } else {
                //The category couldn't be found. Assume it was deleted and delete the rating entry.
                final String updateExpression = "remove " + User.CATEGORY_RATINGS + ".#categoryId";
                final NameMap nameMap = new NameMap().with("#categoryId", categoryId);

                final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withUpdateExpression(updateExpression)
                    .withNameMap(nameMap);

                this.maintenanceDbAccessManager.updateUser(username, updateItemSpec);
              }
            } catch (final Exception e) {
              this.metrics.log(new ErrorDescriptor<>(username + " " + categoryId, classMethod, e));
            }
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
