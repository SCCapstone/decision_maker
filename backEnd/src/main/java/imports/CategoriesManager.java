package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import models.Category;
import models.User;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class CategoriesManager extends DatabaseAccessManager {

  public static final String CATEGORY_ID = "CategoryId";
  public static final String CATEGORY_NAME = "CategoryName";
  public static final String CHOICES = "Choices";
  public static final String GROUPS = "Groups";
  public static final String NEXT_CHOICE_NO = "NextChoiceNo";
  public static final String VERSION = "Version";
  public static final String OWNER = "Owner";

  public static final Integer MAX_NUMBER_OF_CATEGORIES = 25;

  public CategoriesManager() {
    super("categories", "CategoryId", Regions.US_EAST_2);
  }

  public CategoriesManager(final DynamoDB dynamoDB) {
    super("categories", "CategoryId", Regions.US_EAST_2, dynamoDB);
  }


  /**
   * This method removes a given group from each category that is currently in the group.
   *
   * @param categoryIds A set containing all of the categories currently in the group. Note that
   *                    this can be an empty set, as groups are not required to have categories.
   * @param groupId     The GroupId for the group to be removed from the categories table.
   * @param metrics     Standard metrics object for profiling and logging
   */
  public ResultStatus removeGroupFromCategories(final Set<String> categoryIds, final String groupId,
      final Metrics metrics) {
    final String classMethod = "CategoriesManager.removeGroupFromCategories";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      final String updateExpression = "remove " + GROUPS + ".#groupId";
      final NameMap nameMap = new NameMap().with("#groupId", groupId);

      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withNameMap(nameMap);

      for (String categoryId : categoryIds) {
        updateItemSpec.withPrimaryKey(this.getPrimaryKeyIndex(), categoryId);
        this.updateItem(updateItemSpec);
      }
      resultStatus = new ResultStatus(true, "Group successfully removed from categories table.");
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>(groupId, classMethod, e));
      resultStatus.resultMessage = "Exception inside of: " + classMethod;
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}