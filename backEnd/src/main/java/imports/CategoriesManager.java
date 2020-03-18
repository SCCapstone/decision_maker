package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import models.Category;
import models.User;
import utilities.ErrorDescriptor;
import utilities.JsonEncoders;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class CategoriesManager extends DatabaseAccessManager {

  public static final String CATEGORY_ID = "CategoryId";
  public static final String CATEGORY_NAME = "CategoryName";
  public static final String CHOICES = "Choices";
  public static final String GROUPS = "Groups";
  public static final String NEXT_CHOICE_NO = "NextChoiceNo";
  public static final String OWNER = "Owner";

  public static final Integer MAX_NUMBER_OF_CATEGORIES = 25;

  public CategoriesManager() {
    super("categories", "CategoryId", Regions.US_EAST_2);
  }

  public CategoriesManager(final DynamoDB dynamoDB) {
    super("categories", "CategoryId", Regions.US_EAST_2, dynamoDB);
  }

  public ResultStatus addNewCategory(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "CategoriesManager.addNewCategory";
    metrics.commonSetup(classMethod);

    //validate data, log results as there should be some validation already on the front end
    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, RequestFields.USER_RATINGS, CATEGORY_NAME, CHOICES);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String nextCategoryIndex = UUID.randomUUID().toString();
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        final Category newCategory = new Category(jsonMap);
        newCategory.updateNextChoiceNo();
        newCategory.setOwner(activeUser);
        newCategory.setCategoryId(nextCategoryIndex);
        newCategory.setGroups(Collections.emptyMap());

        Optional<String> errorMessage = this.newCategoryIsValid(newCategory, metrics);
        if (!errorMessage.isPresent()) {
          this.putItem(newCategory);

          //put the entered ratings in the users table
          jsonMap.putIfAbsent(CATEGORY_ID, newCategory.getCategoryId()); // add required key
          ResultStatus updatedUsersTableResult = DatabaseManagers.USERS_MANAGER
              .updateUserChoiceRatings(jsonMap, true, metrics);

          //TODO wrap this operation into a transaction with the above
          if (updatedUsersTableResult.success) {
            resultStatus = new ResultStatus(true,
                JsonEncoders.convertObjectToJson(newCategory.asMap()));
          } else {
            resultStatus.resultMessage = "Error: Unable to add this category to the users table. "
                + updatedUsersTableResult.resultMessage;
          }
        } else {
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
      final User user = new User(
          DatabaseManagers.USERS_MANAGER.getItemByPrimaryKey(newCategory.getOwner()).asMap());

      if (user.getOwnedCategories().size() >= MAX_NUMBER_OF_CATEGORIES) {
        errorMessage = this.getUpdatedInvalidMessage(errorMessage,
            "Error: user already has maximum allowed number of categories.");
      }

      for (String categoryName : user.getOwnedCategories().values()) {
        if (categoryName.equals(newCategory.getCategoryName())) {
          errorMessage = this.getUpdatedInvalidMessage(errorMessage,
              "Error: user can not own two categories with the same name.");
          break;
        }
      }

      if (newCategory.getChoices().size() < 1) {
        errorMessage = this.getUpdatedInvalidMessage(errorMessage,
                "Error: category must have at least one choice.");
      }

      for (String choiceLabel : newCategory.getChoices().values()) {
        if (choiceLabel.trim().length() < 1) {
          errorMessage = this
              .getUpdatedInvalidMessage(errorMessage, "Error: choice labels cannot be empty.");
          break;
        }
      }

      if (newCategory.getCategoryName().trim().length() < 1) {
        errorMessage = this
            .getUpdatedInvalidMessage(errorMessage, "Error: category name can not be empty.");
      }
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>(newCategory.asMap(), classMethod, e));
      errorMessage = this.getUpdatedInvalidMessage(errorMessage, "Exception");
    }

    metrics.commonClose(errorMessage == null); // we should get pinged by invalid calls
    return Optional.ofNullable(errorMessage);
  }

  private String getUpdatedInvalidMessage(final String current, final String update) {
    String invalidString;
    if (current == null) {
      invalidString = update;
    } else {
      invalidString = current + "\n" + update;
    }

    return invalidString;
  }

  public ResultStatus editCategory(final Map<String, Object> jsonMap, final Metrics metrics) {
    String classMethod = "CategoriesManager.editCategory";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, RequestFields.USER_RATINGS, CATEGORY_ID, CATEGORY_NAME,
            CHOICES);

    //validate data, log results as there should be some validation already on the front end
    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        final Category newCategory = new Category(jsonMap);
        final Category oldCategory = new Category(
            this.getItemByPrimaryKey(newCategory.getCategoryId()).asMap());

        if (activeUser.equals(oldCategory.getOwner())) {
          newCategory.updateNextChoiceNo();

          String updateExpression =
              "set " + CATEGORY_NAME + " = :name, " + CHOICES + " = :map, " + NEXT_CHOICE_NO
                  + " = :next";
          ValueMap valueMap = new ValueMap()
              .withString(":name", newCategory.getCategoryName())
              .withMap(":map", newCategory.getChoices())
              .withInt(":next", newCategory.getNextChoiceNo());

          UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withPrimaryKey(this.getPrimaryKeyIndex(), newCategory.getCategoryId())
              .withUpdateExpression(updateExpression)
              .withValueMap(valueMap);

          this.updateItem(updateItemSpec);

          //put the entered ratings in the users table
          ResultStatus updatedUsersTableResult = DatabaseManagers.USERS_MANAGER
              .updateUserChoiceRatings(jsonMap, true, metrics);

          if (updatedUsersTableResult.success) {
            oldCategory.setCategoryName(newCategory.getCategoryName());
            oldCategory.setChoices(newCategory.getChoices());
            oldCategory.setNextChoiceNo(newCategory.getNextChoiceNo());
            resultStatus = new ResultStatus(true,
                JsonEncoders.convertObjectToJson(oldCategory.asMap()));
          } else {
            resultStatus.resultMessage =
                "Error: Unable to update this category's ratings in the users table. "
                    + updatedUsersTableResult.resultMessage;
          }
        } else {
          resultStatus.resultMessage = "Error: editing user does not own this category";
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

  public ResultStatus getCategories(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "CategoriesManager.getCategories";
    metrics.commonSetup(classMethod);

    boolean success = true;
    String resultMessage = "";
    List<String> categoryIds = new ArrayList<>();

    //notice, due to how the ActiveUser key is set for every call, it's check must be last!
    if (jsonMap.containsKey(RequestFields.CATEGORY_IDS)) {
      categoryIds = (List<String>) jsonMap.get(RequestFields.CATEGORY_IDS);
    } else if (jsonMap.containsKey(GroupsManager.GROUP_ID)) {
      String groupId = (String) jsonMap.get(DatabaseManagers.GROUPS_MANAGER.getPrimaryKeyIndex());
      categoryIds = DatabaseManagers.GROUPS_MANAGER.getAllCategoryIds(groupId, metrics);
    } else if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      String username = (String) jsonMap.get(RequestFields.ACTIVE_USER);
      categoryIds = DatabaseManagers.USERS_MANAGER.getAllOwnedCategoryIds(username, metrics);
      List<String> groupIds = DatabaseManagers.USERS_MANAGER.getAllGroupIds(username, metrics);

      for (String groupId : groupIds) {
        List<String> groupCategoryIds = DatabaseManagers.GROUPS_MANAGER
            .getAllCategoryIds(groupId, metrics);
        categoryIds.addAll(groupCategoryIds);
      }
    } else {
      success = false;
      resultMessage = "Error: query key not defined.";
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod,
          "lookup key not in request payload/active user not set"));
    }

    if (success) {
      //remove duplicates from categoryIds
      Set<String> uniqueCategoryIds = new LinkedHashSet<>(categoryIds);

      List<Map> categories = new ArrayList<>();
      for (String id : uniqueCategoryIds) {
        try {
          Item dbData = this.getItemByPrimaryKey(id);
          categories.add(dbData.asMap());
        } catch (Exception e) {
          metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        }
      }

      resultMessage = JsonEncoders.convertListToJson(categories);
    }

    metrics.commonClose(success);

    return new ResultStatus(success, resultMessage);
  }

  public ResultStatus deleteCategory(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "CategoriesManager.deleteCategory";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays.asList(RequestFields.ACTIVE_USER, CATEGORY_ID);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        // Confirm that the username matches with the owner of the category before deleting it
        String username = (String) jsonMap.get((RequestFields.ACTIVE_USER));
        String categoryId = (String) jsonMap.get(CATEGORY_ID);

        Item item = this.getItemByPrimaryKey(categoryId);
        if (username.equals(item.getString(OWNER))) {
          List<String> groupIds = new ArrayList<>(item.getMap(GROUPS).keySet());

          if (!groupIds.isEmpty()) {
            DatabaseManagers.GROUPS_MANAGER.removeCategoryFromGroups(groupIds, categoryId, metrics);
          }

          //TODO These last two should probably be put into a ~transaction~
          DatabaseManagers.USERS_MANAGER.removeOwnedCategory(username, categoryId, metrics);

          DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
              .withPrimaryKey(this.getPrimaryKeyIndex(), categoryId);

          this.deleteItem(deleteItemSpec);

          resultStatus = new ResultStatus(true, "Category deleted successfully!");
        } else {
          metrics.log(
              new ErrorDescriptor<>(jsonMap, classMethod, "User is not the owner of the category"));
          resultStatus.resultMessage = "Error: User is not the owner of the category.";
        }
      } catch (Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus.resultMessage = "Error: Unable to parse request.";
      }
    } else {
      metrics.log(
          new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found"));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);

    return resultStatus;
  }

  public ResultStatus removeGroupFromCategories(Set<String> categoryIds, String groupId,
      Metrics metrics) {
    final String className = "CategoriesManager.removeGroupFromCategories";
    metrics.commonSetup(className);

    ResultStatus resultStatus = new ResultStatus();

    try {
      if (categoryIds.isEmpty()) {
        resultStatus = new ResultStatus(true,
            "Success: Group does not need to be removed from categories table.");
      } else {
        final String updateExpression = "remove " + GROUPS + ".#groupId";
        final NameMap nameMap = new NameMap().with("#groupId", groupId);

        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap);

        for (String categoryId : categoryIds) {
          updateItemSpec.withPrimaryKey(this.getPrimaryKeyIndex(), categoryId);
          this.updateItem(updateItemSpec);
        }
        resultStatus = new ResultStatus(true, "Group successfully removed from categories table.");
      }
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>(groupId, className, e));
      resultStatus.resultMessage = "Exception inside of: " + className;
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}