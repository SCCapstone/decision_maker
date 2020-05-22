package dbMaintenance.handlers;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import dbMaintenance.managers.MaintenanceDbAccessManager;
import handlers.ApiRequestHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import models.Category;
import models.Group;
import models.GroupCategory;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class KeyChoicesByLabelHandler implements ApiRequestHandler {

  private final MaintenanceDbAccessManager maintenanceDbAccessManager;
  private final Metrics metrics;

  @Inject
  public KeyChoicesByLabelHandler(final MaintenanceDbAccessManager maintenanceDbAccessManager,
      final Metrics metrics) {
    this.maintenanceDbAccessManager = maintenanceDbAccessManager;
    this.metrics = metrics;
  }

  public ResultStatus handle() {
    final String classMethod = "KeyChoicesByLabelHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus = ResultStatus
        .successful("Category choices keyed by label successfully.");

    try {
      final Iterator<Item> tableItems = this.maintenanceDbAccessManager.scanCategoriesTable();

      while (tableItems.hasNext()) {
        final Item categoryItem = tableItems.next();

        try {
          final String categoryId = categoryItem.getString(Category.CATEGORY_ID);

          if (categoryId.equals("038b60ea-838b-44d5-a5a0-2f000f16140c")) {
            continue; // I tested on this category, so no need to reprocess
          }

          final Map<String, String> oldChoicesRaw = categoryItem.getMap(Category.CHOICES);
          final Map<String, Integer> newChoices = new HashMap<>();

          for (final Map.Entry<String, String> oldChoiceEntry : oldChoicesRaw.entrySet()) {
            newChoices.put(oldChoiceEntry.getValue(), Integer.parseInt(oldChoiceEntry.getKey()));
          }

          final String updateExpression = "set " + Category.CHOICES + " = :map";
          final ValueMap valueMap = new ValueMap().withMap(":map", newChoices);

          final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withUpdateExpression(updateExpression)
              .withValueMap(valueMap);

          this.maintenanceDbAccessManager.updateCategory(categoryId, updateItemSpec);
        } catch (final Exception e) {
          this.metrics
              .log(new ErrorDescriptor<>(categoryItem.get(Category.CATEGORY_ID), classMethod, e));
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
