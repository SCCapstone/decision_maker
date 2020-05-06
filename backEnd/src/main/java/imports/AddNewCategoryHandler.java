package imports;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
public class AddNewCategoryHandler implements Handler {

  public static final Integer MAX_NUMBER_OF_CATEGORIES = 25;

  private DbAccessManager dbAccessManager;

  public ResultStatus handle(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "CategoriesManager.addNewCategory";
    metrics.commonSetup(classMethod);

    //validate data, log results as there should be some validation already on the front end
    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, RequestFields.USER_RATINGS, Category.CATEGORY_NAME,
            Category.CHOICES);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String nextCategoryIndex = UUID.randomUUID().toString();
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        final Category newCategory = new Category(jsonMap);
        newCategory.updateNextChoiceNo();
        newCategory.setVersion(1);
        newCategory.setOwner(activeUser);
        newCategory.setCategoryId(nextCategoryIndex);
        newCategory.setGroups(Collections.emptyMap());

        Optional<String> errorMessage = this.newCategoryIsValid(newCategory, metrics);
        if (!errorMessage.isPresent()) {
          this.dbAccessManager.putCategory(newCategory);

          //put the entered ratings in the users table
          jsonMap
              .putIfAbsent(Category.CATEGORY_ID, newCategory.getCategoryId()); // add required key
          //TODO create handler class an call handle on it for this action
          ResultStatus updatedUsersTableResult = DatabaseManagers.USERS_MANAGER
              .updateUserChoiceRatings(jsonMap, true, metrics);

          //TODO wrap this operation into a transaction with the above
          if (updatedUsersTableResult.success) {
            resultStatus = new ResultStatus(true,
                JsonUtils.convertObjectToJson(newCategory.asMap()));
          } else {
            resultStatus.resultMessage = "Error: Unable to add this category to the users table. "
                + updatedUsersTableResult.resultMessage;
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

  private Optional<String> newCategoryIsValid(final Category newCategory, final Metrics metrics) {
    final String classMethod = "CategoryManager.newCategoryIsValid";
    metrics.commonSetup(classMethod);

    String errorMessage = null;

    try {
      final User user = this.dbAccessManager.getUser(newCategory.getOwner());

      if (user.getOwnedCategories().size() >= MAX_NUMBER_OF_CATEGORIES) {
        errorMessage = this.getUpdatedErrorMessage(errorMessage,
            "Error: user already has maximum allowed number of categories.");
      }

      for (String categoryName : user.getOwnedCategories().values()) {
        if (categoryName.equals(newCategory.getCategoryName())) {
          errorMessage = this.getUpdatedErrorMessage(errorMessage,
              "Error: user can not own two categories with the same name.");
          break;
        }
      }

      if (newCategory.getChoices().size() < 1) {
        errorMessage = this.getUpdatedErrorMessage(errorMessage,
            "Error: category must have at least one choice.");
      }

      for (String choiceLabel : newCategory.getChoices().values()) {
        if (choiceLabel.trim().length() < 1) {
          errorMessage = this
              .getUpdatedErrorMessage(errorMessage, "Error: choice labels cannot be empty.");
          break;
        }
      }

      if (newCategory.getCategoryName().trim().length() < 1) {
        errorMessage = this
            .getUpdatedErrorMessage(errorMessage, "Error: category name can not be empty.");
      }
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>(newCategory.asMap(), classMethod, e));
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
