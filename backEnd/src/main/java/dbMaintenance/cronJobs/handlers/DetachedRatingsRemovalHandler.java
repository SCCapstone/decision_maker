package dbMaintenance.cronJobs.handlers;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import dbMaintenance.managers.MaintenanceDbAccessManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.Group;
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
      //The first thing we need to do is determine all of the category ids associated with the
      // pending consider events in the system.
      final HashSet<String> pendingEventCategoryIds = new HashSet<>();

      final Iterator<Item> pendingEventsTableItems = this.maintenanceDbAccessManager
          .scanPendingEventsTable();

      while (pendingEventsTableItems.hasNext()) {
        final Map<String, Object> pendingEventsData = pendingEventsTableItems.next().asMap();

        try {
          String groupId, eventId;
          Group group;
          List<String> keyPair;
          for (String key : pendingEventsData.keySet()) {
            //skip the scanner id key as it isn't real pending event data
            if (!key.equals(DbAccessManager.PENDING_EVENTS_PRIMARY_KEY)) {
              keyPair = Arrays.asList(key.split(DbAccessManager.DELIM));

              if (keyPair.size() == 2) {
                groupId = keyPair.get(0);
                eventId = keyPair.get(1);

                group = this.maintenanceDbAccessManager.getGroup(groupId);

                //we only need to keep ratings for events in the consider state (aka no choices)
                if (group.getEvents().get(eventId).getTentativeAlgorithmChoices().isEmpty()) {
                  pendingEventCategoryIds.add(group.getEvents().get(eventId).getCategoryId());
                }
              } else {
                metrics.log(new ErrorDescriptor<>(
                    "scanner id: " + pendingEventsData
                        .get(DbAccessManager.PENDING_EVENTS_PRIMARY_KEY)
                        + ", key : " + key,
                    classMethod, "bad format for key in pending events table"));
                resultStatus = ResultStatus.failure("Bad key format in pending events partition.");
              }
            }
          }
        } catch (final Exception e) {
          this.metrics.log(new ErrorDescriptor<>(pendingEventsData, classMethod, e));
          resultStatus = ResultStatus.failure("Exception in " + classMethod);
        }
      }

      //next we loop over the user and look for ratings associated with neither a category nor a
      // pending event
      final Iterator<Item> tableItems = this.maintenanceDbAccessManager.scanUsersTable();

      while (tableItems.hasNext()) {
        final Item userItem = tableItems.next();

        try {
          final User user = new User(userItem);

          final StringBuilder updateExpressionBuilder = new StringBuilder();
          final NameMap nameMap = new NameMap();

          int i = 0;
          for (final String categoryId : user.getCategoryRatings().keySet()) {
            //if there is a pending event associated with this category, do not delete the ratings
            if (pendingEventCategoryIds.contains(categoryId)) {
              continue;
            }

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
              this.metrics.log(
                  new ErrorDescriptor<>(user.getUsername() + " " + categoryId, classMethod, e));
              resultStatus = ResultStatus.failure("Exception in " + classMethod);
            }

            i++;
          }

          if (updateExpressionBuilder.length() > 0) {
            final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withUpdateExpression(updateExpressionBuilder.toString())
                .withNameMap(nameMap);

            this.maintenanceDbAccessManager.updateUser(user.getUsername(), updateItemSpec);
          }
        } catch (final Exception e) {
          this.metrics.log(new ErrorDescriptor<>(userItem.asMap(), classMethod, e));
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
