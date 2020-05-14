package handlers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.Category;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.UpdateItemData;

public class DeleteCategoryHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;
  private Metrics metrics;

  @Inject
  public DeleteCategoryHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  public ResultStatus handle(final String activeUser, final String categoryId) {
    final String classMethod = "DeleteCategoryHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      // Confirm that the activeUser is the owner of the category before deleting it
      final Category category = this.dbAccessManager.getCategory(categoryId);
      if (activeUser.equals(category.getOwner())) {
        if (this.removeCategoryFromGroups(category.getGroups().keySet(), categoryId).success) {

          //remove the category from the category table
          final UpdateItemData deleteFromCategories = new UpdateItemData(categoryId,
              DbAccessManager.CATEGORIES_TABLE_NAME);

          //remove the owned category entry in the user item
          final UpdateItemData removeFromOwnedCategories = new UpdateItemData(activeUser,
              DbAccessManager.USERS_TABLE_NAME)
              .withUpdateExpression("remove " + User.OWNED_CATEGORIES + ".#categoryId")
              .withNameMap(new NameMap().with("#categoryId", categoryId));

          final List<TransactWriteItem> actions = new ArrayList<>();
          actions.add(new TransactWriteItem().withDelete(deleteFromCategories.asDelete()));
          actions.add(new TransactWriteItem().withUpdate(removeFromOwnedCategories.asUpdate()));

          this.dbAccessManager.executeWriteTransaction(actions);

          resultStatus = ResultStatus.successful("Category deleted successfully!");
        } else {
          resultStatus = ResultStatus.failure("Unable to remove category from groups");
        }
      } else {
        this.metrics.logWithBody(
            new ErrorDescriptor<>(classMethod, "User is not the owner of the category"));
        resultStatus = ResultStatus.failure("Error: User is not the owner of the category.");
      }
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This function is called when a category is deleted and updates each item in the groups table
   * that was linked to the category accordingly.
   *
   * @param groupIds   A set of group ids that need to have the category id removed from them.
   * @param categoryId The catgory id to be removed.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  private ResultStatus removeCategoryFromGroups(final Set<String> groupIds,
      final String categoryId) {
    final String classMethod = "DeleteCategoryHandler.removeCategoryFromGroups";
    this.metrics.commonSetup(classMethod);

    //assume true and set to false if anything errors
    ResultStatus resultStatus = ResultStatus.successful("Category removed from groups.");

    final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
        .withUpdateExpression("remove Categories.#categoryId")
        .withNameMap(new NameMap().with("#categoryId", categoryId));

    for (final String groupId : groupIds) {
      try {
        this.dbAccessManager.updateGroup(groupId, updateItemSpec);
      } catch (final Exception e) {
        this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
        resultStatus = ResultStatus.failure("Error: Unable to delete category from all groups.");
      }
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
