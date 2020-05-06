package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import models.Category;
import models.User;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class CategoriesManager extends DatabaseAccessManager {

  public static final String CATEGORY_ID = "CategoryId";
  public static final String CATEGORY_NAME = "CategoryName";
  public static final String CHOICES = "Choices";
  public static final String GROUPS = "Groups";
  public static final String NEXT_CHOICE_NO = "NextChoiceNo";
  public static final String VERSION = "Version";
  public static final String OWNER = "Owner";

  public static final Integer MAX_NUMBER_OF_CATEGORIES = 25;

  public CategoriesManager() {
    super("categories", "CategoryId", Regions.US_EAST_2);
  }

  public CategoriesManager(final DynamoDB dynamoDB) {
    super("categories", "CategoryId", Regions.US_EAST_2, dynamoDB);
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
            this.getMapByPrimaryKey(newCategory.getCategoryId()));

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
                "set " + CATEGORY_NAME + " = :name, " + CHOICES + " = :map, " + NEXT_CHOICE_NO
                    + " = :next, " + VERSION + " = :version";
            ValueMap valueMap = new ValueMap()
                .withString(":name", newCategory.getCategoryName())
                .withMap(":map", newCategory.getChoices())
                .withInt(":next", newCategory.getNextChoiceNo())
                .withInt(":version", newCategory.getVersion());

            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(this.getPrimaryKeyIndex(), newCategory.getCategoryId())
                .withUpdateExpression(updateExpression)
                .withValueMap(valueMap);

            this.updateItem(updateItemSpec);
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

  public Integer determineVersionNumber(final Category editCategory, final Category oldCategory) {
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
            errorMessage = this.getUpdatedInvalidMessage(errorMessage,
                "Error: user can not own two categories with the same name.");
            break;
          }
        }
      } else {
        errorMessage = this
            .getUpdatedInvalidMessage(errorMessage, "Error: user does not own this category.");
      }

      if (editCategory.getChoices().size() < 1) {
        errorMessage = this.getUpdatedInvalidMessage(errorMessage,
            "Error: category must have at least one choice.");
      }

      for (String choiceLabel : editCategory.getChoices().values()) {
        if (choiceLabel.trim().length() < 1) {
          errorMessage = this
              .getUpdatedInvalidMessage(errorMessage, "Error: choice labels cannot be empty.");
          break;
        }
      }

      if (editCategory.getCategoryName().trim().length() < 1) {
        errorMessage = this
            .getUpdatedInvalidMessage(errorMessage, "Error: category name can not be empty.");
      }
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>(editCategory.asMap(), classMethod, e));
      errorMessage = this.getUpdatedInvalidMessage(errorMessage, "Exception");
    }

    metrics.commonClose(errorMessage == null); // we should get pinged by invalid calls
    return Optional.ofNullable(errorMessage);
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

      resultMessage = JsonUtils.convertIterableToJson(categories);
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

        final Category category = new Category(this.getMapByPrimaryKey(categoryId));
        if (username.equals(category.getOwner())) {
          DatabaseManagers.GROUPS_MANAGER
              .removeCategoryFromGroups(category.getGroups().keySet(), categoryId, metrics);

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

  /**
   * This method removes a given group from each category that is currently in the group.
   *
   * @param categoryIds A set containing all of the categories currently in the group. Note that
   *                    this can be an empty set, as groups are not required to have categories.
   * @param groupId     The GroupId for the group to be removed from the categories table.
   * @param metrics     Standard metrics object for profiling and logging
   */
  public ResultStatus removeGroupFromCategories(final Set<String> categoryIds, final String groupId,
      final Metrics metrics) {
    final String classMethod = "CategoriesManager.removeGroupFromCategories";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
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
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>(groupId, classMethod, e));
      resultStatus.resultMessage = "Exception inside of: " + classMethod;
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}