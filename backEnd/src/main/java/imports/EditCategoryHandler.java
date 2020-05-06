package imports;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import models.Category;
import models.User;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

@AllArgsConstructor
public class EditCategoryHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;

  public ResultStatus handle(final Map<String, Object> jsonMap, final Metrics metrics) {
    String classMethod = "EditCategoryHandler.handle";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, RequestFields.USER_RATINGS, Category.CATEGORY_ID, Category.CATEGORY_NAME,
            Category.CHOICES);

    //validate data, log results as there should be some validation already on the front end
    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        final Category newCategory = new Category(jsonMap);
        final Category oldCategory = this.dbAccessManager.getCategory(newCategory.getCategoryId());

        Optional<String> errorMessage = this
            .editCategoryIsValid(newCategory, oldCategory, activeUser, metrics);
        if (!errorMessage.isPresent()) {
          //make sure the new category has everything set on it for the encoding in the api response
          newCategory.updateNextChoiceNo();
          newCategory.setVersion(this.determineVersionNumber(newCategory, oldCategory));
          newCategory.setGroups(oldCategory.getGroups());
          newCategory.setOwner(oldCategory.getOwner());

          // only edit the category definition if something has changed
          if (!newCategory.getVersion().equals(oldCategory.getVersion())) {
            String updateExpression =
                "set " + Category.CATEGORY_NAME + " = :name, " + Category.CHOICES + " = :map, " + Category.NEXT_CHOICE_NO
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

          //put the entered ratings in the users table
          ResultStatus updatedUsersTableResult = DatabaseManagers.USERS_MANAGER
              .updateUserChoiceRatings(jsonMap, metrics);

          if (updatedUsersTableResult.success) {
            resultStatus = new ResultStatus(true,
                JsonUtils.convertObjectToJson(newCategory.asMap()));
          } else {
            resultStatus.resultMessage = "Error in call to users manager.";
            resultStatus.applyResultStatus(updatedUsersTableResult);
          }
        } else {
          metrics.log(new WarningDescriptor<>(jsonMap, classMethod, errorMessage.get()));
          resultStatus.resultMessage = errorMessage.get();
        }
      } catch (Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus.resultMessage = "Error: Unable to parse request.";
      }
    } else {
      metrics.log(
          new ErrorDescriptor<>(jsonMap, classMethod, "Error: Required request keys not found."));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);
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
      final Category oldCategory, final String activeUser, final Metrics metrics) {
    final String classMethod = "CategoryManager.editCategoryIsValid";
    metrics.commonSetup(classMethod);

    String errorMessage = null;

    try {
      if (oldCategory.getOwner().equals(activeUser)) {
        final User user = new User(DatabaseManagers.USERS_MANAGER.getMapByPrimaryKey(activeUser));

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
      metrics.log(new ErrorDescriptor<>(editCategory.asMap(), classMethod, e));
      errorMessage = this.getUpdatedErrorMessage(errorMessage, "Exception");
    }

    metrics.commonClose(errorMessage == null); // we should get pinged by invalid calls
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
