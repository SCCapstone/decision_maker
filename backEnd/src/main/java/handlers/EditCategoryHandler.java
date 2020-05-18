package handlers;

import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.Category;
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
        newCategory.updateNextChoiceNo();
        newCategory.setVersion(this.determineVersionNumber(newCategory, oldCategory));
        newCategory.setGroups(oldCategory.getGroups());
        newCategory.setOwner(oldCategory.getOwner());

        final List<TransactWriteItem> actions = new ArrayList<>();

        // only edit the category definition if something has changed
        if (!newCategory.getVersion().equals(oldCategory.getVersion())) {
          final String updateExpression =
              "set " + Category.CATEGORY_NAME + " = :name, " + Category.CHOICES + " = :map, "
                  + Category.NEXT_CHOICE_NO
                  + " = :next, " + Category.VERSION + " = :version";
          final ValueMap valueMap = new ValueMap()
              .withString(":name", newCategory.getCategoryName())
              .withMap(":map", newCategory.getChoices())
              .withInt(":next", newCategory.getNextChoiceNo())
              .withInt(":version", newCategory.getVersion());

          final UpdateItemData updateItemData = new UpdateItemData(categoryId,
              DbAccessManager.CATEGORIES_TABLE_NAME)
              .withUpdateExpression(updateExpression)
              .withValueMap(valueMap);

          actions.add(new TransactWriteItem().withUpdate(updateItemData.asUpdate()));
        }

        //get the update data to entered the ratings into the users table
        final ResultStatus<UpdateItemData> updatedUsersTableResult =
            this.updateUserChoiceRatingsHandler
                .handle(activeUser, categoryId, newCategory.getVersion(), userRatings, false,
                    categoryName);

        if (updatedUsersTableResult.success) {
          actions.add(new TransactWriteItem().withUpdate(updatedUsersTableResult.data.asUpdate()));

          this.dbAccessManager.executeWriteTransaction(actions);

          resultStatus = ResultStatus
              .successful(JsonUtils.convertObjectToJson(newCategory.asMap()));
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

  private Integer determineVersionNumber(final Category editCategory, final Category oldCategory) {
    Integer versionNumber = oldCategory.getVersion();

    if (!editCategory.getCategoryName().equals(oldCategory.getCategoryName())) {
      // if the category name changed then update the version
      versionNumber++;
    } else if (editCategory.getChoices().size() != oldCategory.getChoices().size()) {
      // if there are a different number of choices then we know the version is different
      versionNumber++;
    } else if (!editCategory.getChoices().keySet().containsAll(oldCategory.getChoices().keySet())) {
      // there are the same number of choices but they aren't the same choices
      versionNumber++;
    } else {
      // check each label, if any differ then it's a new version
      for (final String choiceId : oldCategory.getChoices().keySet()) {
        if (!oldCategory.getChoices().get(choiceId)
            .equals(editCategory.getChoices().get(choiceId))) {
          versionNumber++;
          break;
        }
      }
    }

    return versionNumber;
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

      for (String choiceLabel : editCategory.getChoices().values()) {
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
}
