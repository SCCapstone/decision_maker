package imports;

import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import exceptions.MissingApiRequestKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import models.Category;
import models.User;
import utilities.AttributeValueUtils;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;
import utilities.UpdateItemData;
import utilities.WarningDescriptor;

public class AddNewCategoryHandler extends ApiRequestHandler {

  public static final Integer MAX_NUMBER_OF_CATEGORIES = 25;
  public static final Integer DEFAULT_CATEGORY_VERSION = 1;

  public AddNewCategoryHandler(final DbAccessManager dbAccessManager,
      final Map<String, Object> requestBody, final Metrics metrics) {
    super(dbAccessManager, requestBody, metrics);
  }

  @Override
  public ResultStatus handle() throws MissingApiRequestKeyException {
    final String classMethod = "AddNewCategoryHandler.handle";

    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, RequestFields.USER_RATINGS, Category.CATEGORY_NAME,
            Category.CHOICES);

    if (this.requestBody.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) this.requestBody.get((RequestFields.ACTIVE_USER));
        final String categoryName = (String) this.requestBody.get(Category.CATEGORY_NAME);
        final Map<String, Object> choices = (Map<String, Object>) this.requestBody
            .get(Category.CHOICES);
        final Map<String, Object> userRatings = (Map<String, Object>) this.requestBody
            .get(RequestFields.USER_RATINGS);

        resultStatus = this.handle(activeUser, categoryName, choices, userRatings);
      } catch (final Exception e) {
        //something couldn't get parsed
        this.metrics.log(new ErrorDescriptor<>(this.requestBody, classMethod, e));
        resultStatus.resultMessage = "Error: Invalid request.";
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
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
        final ResultStatus<UpdateItemData> updatedUsersTableResult = new UpdateUserChoiceRatingsHandler(
            this.dbAccessManager, this.requestBody, this.metrics)
            .handle(activeUser, newCategory.getCategoryId(), userRatings, false, true);

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
        this.metrics
            .log(new WarningDescriptor<>(this.requestBody, classMethod, errorMessage.get()));
        resultStatus.resultMessage = errorMessage.get();
      }
    } catch (Exception e) {
      this.metrics.log(new ErrorDescriptor<>(this.requestBody, classMethod, e));
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
