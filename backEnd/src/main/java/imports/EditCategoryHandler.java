package imports;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import models.Category;
import models.User;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class EditCategoryHandler extends ApiRequestHandler {

  public EditCategoryHandler(final DbAccessManager dbAccessManager,
      final Map<String, Object> requestBody, final Metrics metrics) {
    super(dbAccessManager, requestBody, metrics);
  }

  @Override
  public ResultStatus handle() {
    final String classMethod = "EditCategoryHandler.handle";

    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, RequestFields.USER_RATINGS, Category.CATEGORY_ID,
            Category.CATEGORY_NAME, Category.CHOICES);

    if (this.requestBody.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) this.requestBody.get((RequestFields.ACTIVE_USER));
        final String categoryId = (String) this.requestBody.get(Category.CATEGORY_ID);
        final String categoryName = (String) this.requestBody.get(Category.CATEGORY_NAME);
        final Map<String, Object> choices = (Map<String, Object>) this.requestBody
            .get(Category.CHOICES);
        final Map<String, Object> userRatings = (Map<String, Object>) this.requestBody
            .get(RequestFields.USER_RATINGS);

        resultStatus = this.handle(activeUser, categoryId, categoryName, choices, userRatings);
      } catch (final Exception e) {
        //something couldn't get parsed
        this.metrics.log(new ErrorDescriptor<>(this.requestBody, classMethod, e));
        resultStatus.resultMessage = "Error: Invalid request.";
      }
    } else {
      this.metrics.log(new ErrorDescriptor<>(this.requestBody, classMethod,
          "Required request keys not found"));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    return resultStatus;
  }

  public ResultStatus handle(final String activeUser, final String categoryId,
      final String categoryName, final Map<String, Object> choices,
      final Map<String, Object> userRatings) {
    final String classMethod = "EditCategoryHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

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

        // only edit the category definition if something has changed
        if (!newCategory.getVersion().equals(oldCategory.getVersion())) {
          String updateExpression =
              "set " + Category.CATEGORY_NAME + " = :name, " + Category.CHOICES + " = :map, "
                  + Category.NEXT_CHOICE_NO
                  + " = :next, " + Category.VERSION + " = :version";
          ValueMap valueMap = new ValueMap()
              .withString(":name", newCategory.getCategoryName())
              .withMap(":map", newCategory.getChoices())
              .withInt(":next", newCategory.getNextChoiceNo())
              .withInt(":version", newCategory.getVersion());

          UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withUpdateExpression(updateExpression)
              .withValueMap(valueMap);

          this.dbAccessManager.updateCategory(newCategory.getCategoryId(), updateItemSpec);
        }

        //TODO maybe try to build a transaction out of this?

        //put the entered ratings in the users table
        final ResultStatus updatedUsersTableResult =
            new UpdateUserChoiceRatingsHandler(this.dbAccessManager, this.requestBody, this.metrics)
            .handle(activeUser, categoryId, userRatings);

        if (updatedUsersTableResult.success) {
          resultStatus = new ResultStatus(true,
              JsonUtils.convertObjectToJson(newCategory.asMap()));
        } else {
          resultStatus.resultMessage = "Error in call to users manager.";
          resultStatus.applyResultStatus(updatedUsersTableResult);
        }
      } else {
        this.metrics
            .log(new WarningDescriptor<>(this.requestBody, classMethod, errorMessage.get()));
        resultStatus.resultMessage = errorMessage.get();
      }
    } catch (Exception e) {
      this.metrics.log(new ErrorDescriptor<>(this.requestBody, classMethod, e));
      resultStatus.resultMessage = "Error: Unable to parse request.";
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
    final String classMethod = "CategoryManager.editCategoryIsValid";
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
