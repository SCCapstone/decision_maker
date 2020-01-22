package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

  public CategoriesManager() {
    super("categories", "CategoryId", Regions.US_EAST_2);
  }

  public CategoriesManager(final DynamoDB dynamoDB) {
    super("categories", "CategoryId", Regions.US_EAST_2, dynamoDB);
  }

  public ResultStatus addNewCategory(Map<String, Object> jsonMap) {
    //validate data, log results as there should be some validation already on the front end
    ResultStatus resultStatus = new ResultStatus();
    if (
        jsonMap.containsKey(CATEGORY_NAME) &&
            jsonMap.containsKey(CHOICES) &&
            jsonMap.containsKey(RequestFields.USER_RATINGS) &&
            jsonMap.containsKey(RequestFields.ACTIVE_USER)
    ) {
      final UUID uuid = UUID.randomUUID();

      try {
        String nextCategoryIndex = uuid.toString();
        String categoryName = (String) jsonMap.get(CATEGORY_NAME);
        Map<String, Object> choices = (Map<String, Object>) jsonMap.get(CHOICES);
        Map<String, Object> ratings = (Map<String, Object>) jsonMap.get(RequestFields.USER_RATINGS);
        Map<String, Object> groups = new HashMap<>();
        int nextChoiceNo = choices.size();
        String owner = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        Item newCategory = new Item()
            .withPrimaryKey(CATEGORY_ID, nextCategoryIndex)
            .withString(CATEGORY_NAME, categoryName)
            .withMap(CHOICES, choices)
            .withMap(GROUPS, groups)
            .withInt(NEXT_CHOICE_NO, nextChoiceNo)
            .withString(OWNER, owner);

        PutItemSpec putItemSpec = new PutItemSpec()
            .withItem(newCategory);

        this.putItem(putItemSpec);

        //put the entered ratings in the users table
        Map<String, Object> insertNewCatForOwner = new HashMap<>();
        insertNewCatForOwner.put(RequestFields.ACTIVE_USER, owner);
        insertNewCatForOwner.put(CATEGORY_ID, nextCategoryIndex);
        insertNewCatForOwner.put(RequestFields.USER_RATINGS, ratings);

        ResultStatus updatedUsersTableResult =
            DatabaseManagers.USERS_MANAGER.updateUserChoiceRatings(insertNewCatForOwner);

        if (updatedUsersTableResult.success) {
          resultStatus = new ResultStatus(true, "Category created successfully!");
        } else {
          resultStatus.resultMessage = "Error: Unable to add this category to the users table. "
              + updatedUsersTableResult.resultMessage;
        }
      } catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request";
      }
    } else {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    return resultStatus;
  }

  public ResultStatus editCategory(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();
    //validate data, log results as there should be some validation already on the front end
    if (
        jsonMap.containsKey(CATEGORY_ID) &&
            jsonMap.containsKey(CATEGORY_NAME) &&
            jsonMap.containsKey(CHOICES) &&
            jsonMap.containsKey(RequestFields.USER_RATINGS) &&
            jsonMap.containsKey(RequestFields.ACTIVE_USER)
    ) {
      try {
        String categoryId = (String) jsonMap.get(CATEGORY_ID);
        String categoryName = (String) jsonMap.get(CATEGORY_NAME);
        Map<String, Object> choices = (Map<String, Object>) jsonMap.get(CHOICES);
        Map<String, Object> ratings = (Map<String, Object>) jsonMap.get(RequestFields.USER_RATINGS);
        String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        //TODO check to see if the choices match what are there and if not, user needs to be owner (https://github.com/SCCapstone/decision_maker/issues/107)

        int nextChoiceNo = -1;

        //get the max current choiceNo
        for (String choiceNo : choices.keySet()) {
          if (Integer.parseInt(choiceNo) > nextChoiceNo) {
            nextChoiceNo = Integer.parseInt(choiceNo);
          }
        }

        //move the next choice to be the next value up from the max
        nextChoiceNo++;

        String updateExpression =
            "set " + CATEGORY_NAME + " = :name, " + CHOICES + " = :map, " + NEXT_CHOICE_NO
                + " = :next";
        ValueMap valueMap = new ValueMap()
            .withString(":name", categoryName)
            .withMap(":map", choices)
            .withInt(":next", nextChoiceNo);

        UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(this.getPrimaryKeyIndex(), categoryId)
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);

        this.updateItem(updateItemSpec);

        //put the entered ratings in the users table
        Map<String, Object> insertNewCatForOwner = new HashMap<>();
        insertNewCatForOwner.put(RequestFields.ACTIVE_USER, activeUser);
        insertNewCatForOwner.put(CATEGORY_ID, categoryId);
        insertNewCatForOwner.put(RequestFields.USER_RATINGS, ratings);

        ResultStatus updatedUsersTableResult = DatabaseManagers.USERS_MANAGER
            .updateUserChoiceRatings(insertNewCatForOwner);

        if (updatedUsersTableResult.success) {
          resultStatus = new ResultStatus(true, "Category saved successfully!");
        } else {
          resultStatus.resultMessage =
              "Error: Unable to update this category's ratings in the users table. "
                  + updatedUsersTableResult.resultMessage;
        }
      } catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request.";
      }
    } else {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    return resultStatus;
  }

  public ResultStatus getCategories(final Map<String, Object> jsonMap, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
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
      categoryIds = DatabaseManagers.GROUPS_MANAGER.getAllCategoryIds(groupId);
    } else if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      String username = (String) jsonMap.get(RequestFields.ACTIVE_USER);
      categoryIds = DatabaseManagers.USERS_MANAGER
          .getAllCategoryIds(username, metrics, lambdaLogger);
    } else {
      success = false;
      resultMessage = "Error: query key not defined.";
      lambdaLogger.log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
          "lookup key not in request payload/active user not set").toString());
    }

    List<Map> categories = new ArrayList<>();
    for (String id : categoryIds) {
      try {
        Item dbData = this.getItemByPrimaryKey(id);
        if (dbData != null) {
          categories.add(dbData.asMap());
        } else {
          //maybe log this idk, we probably shouldn't have ids that don't point to cats in the db?
          lambdaLogger.log(new ErrorDescriptor<>(id, classMethod, metrics.getRequestId(),
              "CategoryId lookup returned null").toString());
        }
      } catch (Exception e) {
        //definitely need to log this, most likely a db down error
        lambdaLogger.log(
            new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(), e).toString());
      }
    }

    if (success) {
      resultMessage = JsonEncoders.convertListToJson(categories);
    }

    metrics.commonClose(success);

    return new ResultStatus(success, resultMessage);
  }

  public ResultStatus deleteCategory(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();
    if (
        jsonMap.containsKey(CATEGORY_ID) &&
            jsonMap.containsKey(RequestFields.ACTIVE_USER)
    ) {
      try {
        // Confirm that the username matches with the owner of the category before deleting it
        String username = (String) jsonMap.get((RequestFields.ACTIVE_USER));
        String categoryId = (String) jsonMap.get(CATEGORY_ID);

        Item item = this.getItemByPrimaryKey(categoryId);
        if (username.equals(item.getString(OWNER))) {
          List<String> groupIds = new ArrayList<>(item.getMap(GROUPS).keySet());
          DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
              .withPrimaryKey(this.getPrimaryKeyIndex(), categoryId);

          this.deleteItem(deleteItemSpec);

          if (!groupIds.isEmpty()) {
            DatabaseManagers.GROUPS_MANAGER.removeCategoryFromGroups(groupIds, categoryId);
          }

          resultStatus = new ResultStatus(true, "Category deleted successfully!");
        } else {
          resultStatus.resultMessage = "Error: User is not the owner of the category.";
        }
      } catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request.";
      }
    } else {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }
    return resultStatus;
  }
}