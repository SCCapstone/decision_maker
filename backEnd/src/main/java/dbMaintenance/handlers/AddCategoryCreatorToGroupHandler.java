package dbMaintenance.handlers;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import dbMaintenance.managers.MaintenanceDbAccessManager;
import handlers.ApiRequestHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import models.Group;
import models.GroupCategory;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class AddCategoryCreatorToGroupHandler implements ApiRequestHandler {

  private final MaintenanceDbAccessManager maintenanceDbAccessManager;
  private final Metrics metrics;

  public AddCategoryCreatorToGroupHandler(
      final MaintenanceDbAccessManager maintenanceDbAccessManager, final Metrics metrics) {
    this.maintenanceDbAccessManager = maintenanceDbAccessManager;
    this.metrics = metrics;
  }

  public ResultStatus handle() {
    final String classMethod = "AddCategoryCreatorToGroupHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus = ResultStatus
        .successful("Category Creators added to groups successfully.");

    try {
      final Iterator<Item> tableItems = this.maintenanceDbAccessManager.scanUsersTable();

      while (tableItems.hasNext()) {
        final Item groupItem = tableItems.next();

        try {
          final String groupId = groupItem.getString(Group.GROUP_ID);

          if (groupId.equals("1372b335-1d44-4d68-9d55-ccf7a954909d")) {
            continue; // I tested on this group, so no need to reprocess
          }

          final Map<String, Object> oldCategoriesMap = groupItem.getMap(Group.CATEGORIES);

          for (final String categoryId : oldCategoriesMap.keySet()) {
            try {
              final Item categoryItem = this.maintenanceDbAccessManager.getCategoryItem(categoryId);

              if (categoryItem != null) {
                //The category exists, create a GroupCategory out of it and let it fly

                final GroupCategory groupCategory = new GroupCategory(categoryItem.asMap());

                final String updateExpression =
                    "Set " + Group.CATEGORIES + ".#categoryId = :catMap";
                final NameMap nameMap = new NameMap().with("#categoryId", categoryId);
                final ValueMap valueMap = new ValueMap()
                    .withMap(":catMap", groupCategory.asMap());

                final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withUpdateExpression(updateExpression)
                    .withNameMap(nameMap)
                    .withValueMap(valueMap);

                this.maintenanceDbAccessManager.updateGroup(groupId, updateItemSpec);
              } else {
                //The category couldn't be found. Assume it was deleted and delete the entry.
                //NOTE: This shouldn't be possible in our current schema
                final String updateExpression = "remove " + Group.CATEGORIES + ".#categoryId";
                final NameMap nameMap = new NameMap().with("#categoryId", categoryId);

                final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withUpdateExpression(updateExpression)
                    .withNameMap(nameMap);

                this.maintenanceDbAccessManager.updateGroup(groupId, updateItemSpec);
              }
            } catch (final Exception e) {
              this.metrics.log(new ErrorDescriptor<>(groupId + " " + categoryId, classMethod, e));
              resultStatus = ResultStatus.failure("Exception in " + classMethod);
            }
          }
        } catch (final Exception e) {
          this.metrics.log(new ErrorDescriptor<>(groupItem.get(Group.GROUP_ID), classMethod, e));
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
