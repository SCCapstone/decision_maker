package handlers;

import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import managers.DbAccessManager;
import models.Category;
import models.User;
import utilities.AttributeValueUtils;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.UpdateItemData;
import utilities.WarningDescriptor;

public class AddNewCategoryHandler implements ApiRequestHandler {

  public static final Integer MAX_NUMBER_OF_CATEGORIES = 25;
  public static final Integer DEFAULT_CATEGORY_VERSION = 1;

  private final DbAccessManager dbAccessManager;
  private final UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler;
  private final Metrics metrics;

  @Inject
  public AddNewCategoryHandler(
      final DbAccessManager dbAccessManager,
      final UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler,
      final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.updateUserChoiceRatingsHandler = updateUserChoiceRatingsHandler;
    this.metrics = metrics;
  }

  public ResultStatus handle(final String activeUser, final String categoryName,
      final Map<String, Object> choices, final Map<String, Object> userRatings) {
    final String classMethod = "AddNewCategoryHandler.handle";
    this.metrics.commonSetup(classMethod);

    //validate data, log results as there should be some validation already on the front end
    ResultStatus resultStatus = new ResultStatus();

    try {
      final String nextCategoryIndex = UUID.randomUUID().toString();

      final Category newCategory = new Category();
      newCategory.setCategoryId(nextCategoryIndex);
      newCategory.setCategoryName(categoryName);
      newCategory.setVersion(DEFAULT_CATEGORY_VERSION);
      newCategory.setOwner(activeUser);
      newCategory.setGroups(Collections.emptyMap());
      newCategory.setChoicesRawMap(choices);
      newCategory.updateNextChoiceNo();

      Optional<String> errorMessage = this.newCategoryIsValid(newCategory);
      if (!errorMessage.isPresent()) {
        //get the update data for entering the user ratings into the users table
        final ResultStatus<UpdateItemData> updatedUsersTableResult = this.updateUserChoiceRatingsHandler
            .handle(activeUser, newCategory.getCategoryId(), userRatings, false, categoryName,
                true);

        if (updatedUsersTableResult.success) {
          final List<TransactWriteItem> actions = new ArrayList<>();

          actions.add(new TransactWriteItem().withUpdate(updatedUsersTableResult.data.asUpdate()));
          actions.add(new TransactWriteItem()
              .withPut(new Put().withTableName(DbAccessManager.CATEGORIES_TABLE_NAME).withItem(
                  AttributeValueUtils.convertMapToAttributeValueMap(newCategory.asMap()))));

          this.dbAccessManager.executeWriteTransaction(actions);

          resultStatus = new ResultStatus(true, JsonUtils.convertObjectToJson(newCategory.asMap()));
        } else {
          resultStatus.resultMessage = "Error: Unable to add this category to the users table. "
              + updatedUsersTableResult.resultMessage;
        }
      } else {
        this.metrics.logWithBody(new WarningDescriptor<>(classMethod, errorMessage.get()));
        resultStatus.resultMessage = errorMessage.get();
      }
    } catch (Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus.resultMessage = "Exception in " + classMethod;
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private Optional<String> newCategoryIsValid(final Category newCategory) {
    final String classMethod = "CategoryManager.newCategoryIsValid";
    this.metrics.commonSetup(classMethod);

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
      this.metrics.log(new ErrorDescriptor<>(newCategory.asMap(), classMethod, e));
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
