package handlers;

import static utilities.Config.MAX_NUMBER_OF_CHOICES;

import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
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

public class NewCategoryHandler implements ApiRequestHandler {

  public static final Integer MAX_NUMBER_OF_CATEGORIES = 25;

  private final DbAccessManager dbAccessManager;
  private final UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler;
  private final Metrics metrics;

  @Inject
  public NewCategoryHandler(
      final DbAccessManager dbAccessManager,
      final UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler,
      final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.updateUserChoiceRatingsHandler = updateUserChoiceRatingsHandler;
    this.metrics = metrics;
  }

  public ResultStatus handle(final String activeUser, final String categoryName,
      final Map<String, Object> choices, final Map<String, Object> userRatings) {
    final String classMethod = "NewCategoryHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final String categoryId = UUID.randomUUID().toString();

      final Category newCategory = new Category();
      newCategory.setCategoryId(categoryId);
      newCategory.setCategoryName(categoryName);
      newCategory.setOwner(activeUser);
      newCategory.setGroups(Collections.emptyMap());
      newCategory.setChoicesRawMap(choices); // duplicate keys will get filtered out here

      final Optional<String> errorMessage = this.newCategoryIsValid(newCategory);
      if (!errorMessage.isPresent()) {
        //get the update data for entering the user ratings into the users table
        final ResultStatus<UpdateItemData> updatedUsersTableResult = this.updateUserChoiceRatingsHandler
            .handle(activeUser, categoryId, userRatings, false,
                categoryName, true);

        if (updatedUsersTableResult.success) {
          final List<TransactWriteItem> actions = new ArrayList<>();

          actions.add(new TransactWriteItem().withUpdate(updatedUsersTableResult.data.asUpdate()));
          actions.add(new TransactWriteItem()
              .withPut(new Put().withTableName(DbAccessManager.CATEGORIES_TABLE_NAME).withItem(
                  AttributeValueUtils.convertMapToAttributeValueMap(newCategory.asMap()))));

          this.dbAccessManager.executeWriteTransaction(actions);

          resultStatus = ResultStatus
              .successful(JsonUtils.convertObjectToJson(newCategory.asMap()));
        } else {
          resultStatus = ResultStatus
              .failure("Error: Unable to add this category to the users table.");
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

  private Optional<String> newCategoryIsValid(final Category newCategory) {
    final String classMethod = "NewCategoryHandler.newCategoryIsValid";
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

      if (newCategory.getChoices().size() > MAX_NUMBER_OF_CHOICES) {
        errorMessage = this.getUpdatedErrorMessage(errorMessage,
            "Error: category cannot have more than " + MAX_NUMBER_OF_CHOICES + " choices.");
      }

      //choice ids are the labels
      for (final String choiceId : newCategory.getChoices().keySet()) {
        if (choiceId.trim().length() < 1) {
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
