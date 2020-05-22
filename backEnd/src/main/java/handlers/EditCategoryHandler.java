package handlers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.Category;
import models.Group;
import models.User;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.UpdateItemData;
import utilities.WarningDescriptor;

public class EditCategoryHandler implements ApiRequestHandler {

  private final DbAccessManager dbAccessManager;
  private final UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler;
  private final Metrics metrics;

  @Inject
  public EditCategoryHandler(
      final DbAccessManager dbAccessManager,
      final UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler,
      final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.updateUserChoiceRatingsHandler = updateUserChoiceRatingsHandler;
    this.metrics = metrics;
  }

  public ResultStatus handle(final String activeUser, final String categoryId,
      final String categoryName, final Map<String, Object> choices,
      final Map<String, Object> userRatings) {
    final String classMethod = "EditCategoryHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Category newCategory = new Category();
      newCategory.setCategoryId(categoryId);
      newCategory.setCategoryName(categoryName);
      newCategory.setChoicesRawMap(choices);

      final Category oldCategory = this.dbAccessManager.getCategory(categoryId);

      Optional<String> errorMessage = this
          .editCategoryIsValid(newCategory, oldCategory, activeUser);
      if (!errorMessage.isPresent()) {
        //make sure the new category has everything set on it for the encoding in the api response
        newCategory.setGroups(oldCategory.getGroups());
        newCategory.setOwner(oldCategory.getOwner());

        final List<TransactWriteItem> actions = new ArrayList<>();

        final String updateExpression =
            "set " + Category.CATEGORY_NAME + " = :name, " + Category.CHOICES + " = :map";
        final ValueMap valueMap = new ValueMap()
            .withString(":name", newCategory.getCategoryName())
            .withMap(":map", newCategory.getChoices());

        final UpdateItemData updateItemData = new UpdateItemData(categoryId,
            DbAccessManager.CATEGORIES_TABLE_NAME)
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);

        actions.add(new TransactWriteItem().withUpdate(updateItemData.asUpdate()));

        //get the update data to entered the ratings into the users table
        final ResultStatus<UpdateItemData> updatedUsersTableResult =
            this.updateUserChoiceRatingsHandler
                .handle(activeUser, categoryId, userRatings, false, categoryName);

        if (updatedUsersTableResult.success) {
          actions.add(new TransactWriteItem().withUpdate(updatedUsersTableResult.data.asUpdate()));

          this.dbAccessManager.executeWriteTransaction(actions);

          resultStatus = ResultStatus
              .successful(JsonUtils.convertObjectToJson(newCategory.asMap()));

          //blind send to try an update the associated groups
          this.updateGroupsTable(oldCategory, newCategory);
        } else {
          resultStatus = ResultStatus.failure("Error in dependency.");
          resultStatus.applyResultStatus(updatedUsersTableResult);
        }
      } else {
        this.metrics.logWithBody(new WarningDescriptor<>(classMethod, errorMessage.get()));
        resultStatus = ResultStatus.failure(errorMessage.get());
      }
    } catch (Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private Optional<String> editCategoryIsValid(final Category editCategory,
      final Category oldCategory, final String activeUser) {
    final String classMethod = "EditCategoryHandler.editCategoryIsValid";
    this.metrics.commonSetup(classMethod);

    String errorMessage = null;

    try {
      if (oldCategory.getOwner().equals(activeUser)) {
        final User user = this.dbAccessManager.getUser(activeUser);

        for (String categoryId : user.getOwnedCategories().keySet()) {
          //this is an update and the name might not have changed so we have to see if a different
          //category has this same name
          final String ownedCategoryName = user.getOwnedCategories().get(categoryId);
          if (ownedCategoryName.equals(editCategory.getCategoryName())
              && !categoryId.equals(editCategory.getCategoryId())) {
            errorMessage = this.getUpdatedErrorMessage(errorMessage,
                "Error: user can not own two categories with the same name.");
            break;
          }
        }
      } else {
        errorMessage = this
            .getUpdatedErrorMessage(errorMessage, "Error: user does not own this category.");
      }

      if (editCategory.getChoices().size() < 1) {
        errorMessage = this.getUpdatedErrorMessage(errorMessage,
            "Error: category must have at least one choice.");
      }

      for (final String choiceLabel : editCategory.getChoices().keySet()) {
        if (choiceLabel.trim().length() < 1) {
          errorMessage = this
              .getUpdatedErrorMessage(errorMessage, "Error: choice labels cannot be empty.");
          break;
        }
      }

      if (editCategory.getCategoryName().trim().length() < 1) {
        errorMessage = this
            .getUpdatedErrorMessage(errorMessage, "Error: category name can not be empty.");
      }
    } catch (Exception e) {
      this.metrics.log(new ErrorDescriptor<>(editCategory.asMap(), classMethod, e));
      errorMessage = this.getUpdatedErrorMessage(errorMessage, "Exception");
    }

    this.metrics.commonClose(errorMessage == null);
    return Optional.ofNullable(errorMessage);
  }

  private String getUpdatedErrorMessage(final String current, final String update) {
    String invalidString;
    if (current == null) {
      invalidString = update;
    } else {
      invalidString = current + "\n" + update;
    }

    return invalidString;
  }

  private void updateGroupsTable(final Category oldCategory, final Category newCategory) {
    if (!oldCategory.getCategoryName().equals(newCategory.getCategoryName())) {
      //The name is a denormalized field to the group item. It has changed so we need to update all
      // of the associated groups

      final String classMethod = "EditCategoryHandler.updateGroupsTable";

      final String updateExpression =
          "set " + Group.CATEGORIES + ".#categoryId." + Category.CATEGORY_NAME + " = :categoryName";
      final NameMap nameMap = new NameMap().with("#categoryId", oldCategory.getCategoryId());
      final ValueMap valueMap = new ValueMap()
          .withString(":categoryName", newCategory.getCategoryName());

      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withNameMap(nameMap)
          .withValueMap(valueMap);

      for (final String groupId : oldCategory.getGroups().keySet()) {
        try {
          this.dbAccessManager.updateGroup(groupId, updateItemSpec);
        } catch (final Exception e) {
          this.metrics.log(new ErrorDescriptor<>(
              "groupId: " + groupId + " categoryId: " + oldCategory.getCategoryId(), classMethod,
              e));
        }
      }
    }
  }
}
