package imports;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import models.Category;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

@AllArgsConstructor
public class DeleteCategoryHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;

  public ResultStatus handle(final Map<String, Object> jsonMap, final Metrics metrics) {

    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, Category.CATEGORY_ID);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      final String activeUser = (String) jsonMap.get((RequestFields.ACTIVE_USER));
      final String categoryId = (String) jsonMap.get(Category.CATEGORY_ID);

      resultStatus = this.handle(activeUser, categoryId, metrics);
    } else {
      metrics.log(
          new ErrorDescriptor<>(jsonMap, "DeleteCategoryHandler.handle",
              "Required request keys not found"));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    return resultStatus;
  }

  private ResultStatus handle(final String activeUser, final String categoryId,
      final Metrics metrics) {
    final String classMethod = "DeleteCategoryHandler.handle";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      // Confirm that the username matches with the owner of the category before deleting it
      final Category category = this.dbAccessManager.getCategory(categoryId);
      if (activeUser.equals(category.getOwner())) {
        DatabaseManagers.GROUPS_MANAGER
            .removeCategoryFromGroups(category.getGroups().keySet(), categoryId, metrics);

        //TODO These last two should probably be put into a ~transaction~
        this.removeOwnedCategory(activeUser, categoryId, metrics);
        this.dbAccessManager.deleteCategory(categoryId);

        resultStatus = new ResultStatus(true, "Category deleted successfully!");
      } else {
        metrics.log(
            new ErrorDescriptor<>(
                ImmutableMap.of("activeUser", activeUser, "categoryId", categoryId),
                classMethod, "User is not the owner of the category"));
        resultStatus.resultMessage = "Error: User is not the owner of the category.";
      }
    } catch (Exception e) {
      metrics.log(
          new ErrorDescriptor<>(ImmutableMap.of("activeUser", activeUser, "categoryId", categoryId),
              classMethod, e));
      resultStatus.resultMessage = "Error: Unable to parse request.";
    }

    metrics.commonClose(resultStatus.success);

    return resultStatus;
  }

  /**
   * This method handles removing one of a user's owned categories.
   *
   * @param username   The username of the user that owns the category that is being removed.
   * @param categoryId The id of the owned category being removed.
   * @param metrics    Standard metrics object for profiling and logging.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  private ResultStatus removeOwnedCategory(final String username, final String categoryId,
      final Metrics metrics) {
    final String classMethod = "DeleteCategoryHandler.removeOwnedCategory";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      final String updateExpression = "remove " + User.OWNED_CATEGORIES + ".#categoryId";
      final NameMap nameMap = new NameMap().with("#categoryId", categoryId);

      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withNameMap(nameMap);

      this.dbAccessManager.updateUser(username, updateItemSpec);
      resultStatus = new ResultStatus(true, "Owned category removed successfully");
    } catch (Exception e) {
      metrics.log(
          new ErrorDescriptor<>(ImmutableMap.of("username", username, "categoryId", categoryId),
              classMethod, e));
      resultStatus.resultMessage = "Exception in manager";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
