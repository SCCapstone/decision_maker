package dbMaintenance.handlers;

import com.amazonaws.services.s3.model.ObjectMetadata;
import dbMaintenance.managers.MaintenanceDbAccessManager;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.google.common.collect.ImmutableMap;
import handlers.ApiRequestHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import models.Category;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class UnkeyUserRatingsByVersionHandler implements ApiRequestHandler {

  private final MaintenanceDbAccessManager maintenanceDbAccessManager;
  private final Metrics metrics;

  @Inject
  public UnkeyUserRatingsByVersionHandler(
      final MaintenanceDbAccessManager maintenanceDbAccessManager,
      final Metrics metrics) {
    this.maintenanceDbAccessManager = maintenanceDbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method loops over all of the users in the users table and updates their category ratings
   * map by keying their current ratings mapping by the current version of the associated category.
   *
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle() {
    final String classMethod = "UnkeyUserRatingsByVersionHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus = ResultStatus
        .successful("User Ratings keyed by version successfully.");

    try {
      final Iterator<Item> tableItems = this.maintenanceDbAccessManager.scanUsersTable();

      while (tableItems.hasNext()) {
        final Item userItem = tableItems.next();

        try {
          final String username = userItem.getString(User.USERNAME);

          if (username.equals("john_andrews12")) {
            continue; // I tested on this user, so no need to reprocess
          }

          final Map<String, Object> oldUserRatings = userItem.getMap(User.CATEGORY_RATINGS);

          for (final String categoryId : oldUserRatings.keySet()) {
            try {
              final Map<String, Object> versionToRatingsMap = (Map<String, Object>) oldUserRatings
                  .get(categoryId);
              String biggestVersion = null;
              for (final String version : versionToRatingsMap.keySet()) {
                if (biggestVersion == null || Integer.parseInt(biggestVersion) < Integer
                    .parseInt(version)) {
                  biggestVersion = version;
                }
              }

              final Map<String, Object> choiceToRatings = (Map<String, Object>) versionToRatingsMap
                  .get(biggestVersion);

              final String updateExpression =
                  "Set " + User.CATEGORY_RATINGS + ".#categoryId = :versionMap";
              final NameMap nameMap = new NameMap().with("#categoryId", categoryId);
              final ValueMap valueMap = new ValueMap()
                  .withMap(":versionMap", choiceToRatings);

              final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                  .withUpdateExpression(updateExpression)
                  .withNameMap(nameMap)
                  .withValueMap(valueMap);

              this.maintenanceDbAccessManager.updateUser(username, updateItemSpec);
            } catch (final Exception e) {
              this.metrics.log(new ErrorDescriptor<>(username + " " + categoryId, classMethod, e));
              resultStatus = ResultStatus.failure("Exception in " + classMethod);
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
