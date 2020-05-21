package dbMaintenance.cronJobs.handlers;

import static utilities.Config.PENDING_EVENTS_DELIM;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import dbMaintenance.managers.MaintenanceDbAccessManager;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.Category;
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
      final Iterator<Item> pendingEventsTableItems = this.maintenanceDbAccessManager
          .scanPendingEventsTable();

      //The first thing we need to do is determine all of the category ids and the versions of said
      // categories of the pending events in the system.
      final HashMap<String, HashSet<String>> pendingEventCategoryIdsToVersionSets = new HashMap<>();

      while (pendingEventsTableItems.hasNext()) {
        final Map<String, Object> pendingEventsData = pendingEventsTableItems.next().asMap();

        String groupId, eventId;
        Group group;
        List<String> keyPair;
        for (String key : pendingEventsData.keySet()) {
          //skip the scanner id key as it isn't real pending event data
          if (!key.equals(DbAccessManager.PENDING_EVENTS_PRIMARY_KEY)) {
            keyPair = Arrays.asList(key.split(PENDING_EVENTS_DELIM));

            if (keyPair.size() == 2) {
              groupId = keyPair.get(0);
              eventId = keyPair.get(1);

              group = this.maintenanceDbAccessManager.getGroup(groupId);

              //add a hashset for the category if the category hasn't been mapped to yet
              pendingEventCategoryIdsToVersionSets
                  .putIfAbsent(group.getEvents().get(eventId).getCategoryId(), new HashSet<>());
              //add the version to the set of pending category versions
              pendingEventCategoryIdsToVersionSets
                  .get(group.getEvents().get(eventId).getCategoryId())
                  .add(group.getEvents().get(eventId).getCategoryVersion().toString());
            } else {
              metrics.log(new ErrorDescriptor<>(
                  "scanner id: " + pendingEventsData.get(DbAccessManager.PENDING_EVENTS_PRIMARY_KEY)
                      + ", key : " + key,
                  classMethod, "bad format for key in pending events table"));
              resultStatus = ResultStatus.failure("Bad key format in pending events partition.");
            }
          }
        }
      }

      final Iterator<Item> userTableItems = this.maintenanceDbAccessManager.scanUsersTable();

      while (userTableItems.hasNext()) {
        final Item userItem = userTableItems.next();

        try {
          final User user = new User(userItem);
          final String username = user.getUsername();

          final StringBuilder updateExpressionBuilder = new StringBuilder();
          final NameMap nameMap = new NameMap();

          int i = 0;
          for (final String categoryId : user.getCategoryRatings().keySet()) {
            try {
              final Item categoryItem = this.maintenanceDbAccessManager.getCategoryItem(categoryId);

              final String categoryIdName = "#categoryId" + i;
              final String dotCategoryIdName = "." + categoryIdName;

              if (categoryItem == null) {
                //The category does not exist, remove non necessary ratings from the user item

                if (pendingEventCategoryIdsToVersionSets.containsKey(categoryId)) {
                  //check if user has ratings for one of the versions, if so we can remove all
                  // mappings except those
                  final Set<String> userCategoryVersionsToKeep = user.getCategoryRatings()
                      .get(categoryId).getVersionSet();

                  //do a set intersection with the existing user versions and the pending versions
                  userCategoryVersionsToKeep
                      .retainAll(pendingEventCategoryIdsToVersionSets.get(categoryId));

                  if (userCategoryVersionsToKeep.size() > 0) {
                    //some of the user's ratings for this deleted category are still pertinent for
                    // pending events -> save those, delete everything else
                    int j = 0;
                    for (final String version : user.getCategoryRatings().get(categoryId)
                        .getVersionSet()) {
                      if (!userCategoryVersionsToKeep.contains(version)) {
                        final String versionName = "#v_" + i + "_" + j;
                        final String dotVersionName = "." + versionName;

                        if (updateExpressionBuilder.length() == 0) {
                          updateExpressionBuilder.append("remove ").append(User.CATEGORY_RATINGS)
                              .append(dotCategoryIdName).append(dotVersionName);
                        } else {
                          updateExpressionBuilder.append(", ").append(User.CATEGORY_RATINGS)
                              .append(dotCategoryIdName).append(dotVersionName);
                        }

                        nameMap.with(categoryIdName, categoryId);
                        nameMap.with(versionName, version);
                      }

                      j++;
                    }
                  } else {
                    //the user has no version maps saved for pending events associated with this
                    // deleted category -> delete the users whole rating map for this category
                    if (updateExpressionBuilder.length() == 0) {
                      updateExpressionBuilder.append("remove ").append(User.CATEGORY_RATINGS)
                          .append(dotCategoryIdName);
                    } else {
                      updateExpressionBuilder.append(", ").append(User.CATEGORY_RATINGS)
                          .append(dotCategoryIdName);
                    }

                    nameMap.with(categoryIdName, categoryId);
                  }
                } else {
                  //nothing pending so were good to remove the entire category mapping
                  if (updateExpressionBuilder.length() == 0) {
                    updateExpressionBuilder.append("remove ").append(User.CATEGORY_RATINGS)
                        .append(dotCategoryIdName);
                  } else {
                    updateExpressionBuilder.append(", ").append(User.CATEGORY_RATINGS)
                        .append(dotCategoryIdName);
                  }

                  nameMap.with(categoryIdName, categoryId);
                }
              } else {
                //the category still exists, but we still need to check to see if the user still has
                // old version mappings that can be removed

                final String currentCategoryVersion = categoryItem.getInt(Category.VERSION) + "";

                final Set<String> userCategoryVersions = user.getCategoryRatings().get(categoryId)
                    .getVersionSet();

                if (pendingEventCategoryIdsToVersionSets.containsKey(categoryId)) {
                  //check if user has ratings for one of the versions, if so we can remove all
                  // mappings except those and the most up to date version

                  //add the current version since we definitely want to keep that if the user has it
                  pendingEventCategoryIdsToVersionSets.get(categoryId).add(currentCategoryVersion);

                  //do a set intersection with the existing user versions and the pending versions
                  userCategoryVersions
                      .retainAll(pendingEventCategoryIdsToVersionSets.get(categoryId));

                  if (userCategoryVersions.size() > 0) {
                    //some of the user's ratings for this deleted category are still pertinent for
                    // pending events -> save those, delete everything else
                    int j = 0;
                    for (final String version : user.getCategoryRatings().get(categoryId)
                        .getVersionSet()) {
                      if (!userCategoryVersions.contains(version)) {
                        final String versionName = "#v_" + i + "_" + j;
                        final String dotVersionName = "." + versionName;

                        if (updateExpressionBuilder.length() == 0) {
                          updateExpressionBuilder.append("remove ").append(User.CATEGORY_RATINGS)
                              .append(dotCategoryIdName).append(dotVersionName);
                        } else {
                          updateExpressionBuilder.append(", ").append(User.CATEGORY_RATINGS)
                              .append(dotCategoryIdName).append(dotVersionName);
                        }

                        nameMap.with(categoryIdName, categoryId);
                        nameMap.with(versionName, version);
                      }

                      j++;
                    }
                  } else {
                    //the user has no version maps saved for pending events associated with this
                    // category or the most current version of this category -> delete the user's
                    // whole rating map for this category
                    if (updateExpressionBuilder.length() == 0) {
                      updateExpressionBuilder.append("remove ").append(User.CATEGORY_RATINGS)
                          .append(dotCategoryIdName);
                    } else {
                      updateExpressionBuilder.append(", ").append(User.CATEGORY_RATINGS)
                          .append(dotCategoryIdName);
                    }

                    nameMap.with(categoryIdName, categoryId);
                  }
                } else {
                  //there are no pending events for this category, we can remove all ratings maps
                  // except for the most up to date version
                  if (userCategoryVersions.contains(currentCategoryVersion)) {
                    //get the ratings that we need to save and overwrite this category map

                    if (userCategoryVersions.size() > 1) {
                      final Map 
                    }
                  } else {
                    //the user doesn't have ratings for the most up to date category version, we can
                    // delete their entire map for this category
                  }
                }
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
