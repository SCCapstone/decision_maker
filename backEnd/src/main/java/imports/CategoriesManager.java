package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import utilities.ExceptionHelper;
import utilities.IOStreamsHelper;
import utilities.RequestFields;
import utilities.ResultStatus;

import java.util.*;

public class CategoriesManager extends DatabaseAccessManager {

  public static final String CATEGORY_ID = "CategoryId";
  public static final String CATEGORY_NAME = "CategoryName";
  public static final String CHOICES = "Choices";
  public static final String GROUPS = "Groups";
  public static final String NEXT_CHOICE_NO = "NextChoiceNo";
  public static final String OWNER = "Owner";

  private final UsersManager usersManager = new UsersManager();

  private UUID uuid;

  public CategoriesManager() {
    super("categories", "CategoryId", Regions.US_EAST_2);
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
      this.uuid = UUID.randomUUID();

      try {
        String nextCategoryIndex = this.uuid.toString();
        String categoryName = (String) jsonMap.get(CATEGORY_NAME);
        Map<String, Object> choices = (Map<String, Object>) jsonMap.get(CHOICES);
        Map<String, Object> ratings = (Map<String, Object>) jsonMap.get(RequestFields.USER_RATINGS);
        Map<String, Object> groups = new HashMap<String, Object>();
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

        super.putItem(putItemSpec);

        //put the entered ratings in the users table
        Map<String, Object> insertNewCatForOwner = new HashMap<String, Object>();
        insertNewCatForOwner.put(RequestFields.ACTIVE_USER, owner);
        insertNewCatForOwner.put(CATEGORY_ID, nextCategoryIndex);
        insertNewCatForOwner.put(RequestFields.USER_RATINGS, ratings);
        ResultStatus updatedUsersTableResult = this.usersManager
            .updateUserChoiceRatings(insertNewCatForOwner);

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
            jsonMap.containsKey(CHOICES) &&
            jsonMap.containsKey(RequestFields.USER_RATINGS) &&
            jsonMap.containsKey(RequestFields.ACTIVE_USER)
    ) {
      try {
        String categoryId = (String) jsonMap.get(CATEGORY_ID);
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

        String updateExpression = "set " + CHOICES + " = :map, " + NEXT_CHOICE_NO + " = :next";
        ValueMap valueMap = new ValueMap()
            .withMap(":map", choices)
            .withInt(":next", nextChoiceNo);

        UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(super.getPrimaryKeyIndex(), categoryId)
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);

        super.updateItem(updateItemSpec);

        //put the entered ratings in the users table
        Map<String, Object> insertNewCatForOwner = new HashMap<String, Object>();
        insertNewCatForOwner.put(UsersManager.USERNAME, activeUser);
        insertNewCatForOwner.put(CATEGORY_ID, categoryId);
        insertNewCatForOwner.put(RequestFields.USER_RATINGS, ratings);

        ResultStatus updatedUsersTableResult = this.usersManager
            .updateUserChoiceRatings(insertNewCatForOwner);

        if (updatedUsersTableResult.success) {
          resultStatus = new ResultStatus(true, "Category saved successfully!");
        } else {
          resultStatus.resultMessage =
              "Error: Unable to update this category's ratings in the users table. "
                  + updatedUsersTableResult.resultMessage;
        }

        resultStatus = new ResultStatus(true, "Category updated successfully!");
      } catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage =
            "Error: Unable to parse request." + '\n' + ExceptionHelper.getStackTrace(e);
      }
    } else {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    return resultStatus;
  }

  public ResultStatus getCategories(Map<String, Object> jsonMap) {
    boolean success = true;
    String resultMessage = "";
    List<String> categoryIds = new ArrayList<String>();

    if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      String username = (String) jsonMap.get(RequestFields.ACTIVE_USER);
      categoryIds = this.usersManager.getAllCategoryIds(username);
    } else if (jsonMap.containsKey(RequestFields.CATEGORY_IDS)) {
      categoryIds = (List<String>) jsonMap.get(RequestFields.CATEGORY_IDS);
    } else {
      success = false;
      resultMessage = "Error: query key not defined.";
    }

    // this will be a json string representing an array of objects
    StringBuilder outputString = new StringBuilder("[");
    for (String id : categoryIds) {
      Item dbData = super
          .getItem(new GetItemSpec().withPrimaryKey(super.getPrimaryKeyIndex(), id));
      Map<String, Object> categoryData = dbData.asMap();
      outputString.append("{");
      for (String categoryAttribute : categoryData.keySet()) {
        Object value = categoryData.get(categoryAttribute);
        if (value instanceof Map) {
          // found a map in the object, so now loop through each key/value in said map and format appropriately
          outputString.append(String.format("\\\"%s\\\":", categoryAttribute));
          Map<Object, Object> mapAttribute = (Map<Object, Object>) value;
          outputString.append("{");
          if (mapAttribute.size() > 0) {
            for (Object key : mapAttribute.keySet()) {
              outputString.append(String.format("\\\"%s\\\":", key.toString()));
              outputString
                  .append(String.format("\\\"%s\\\",", mapAttribute.get(key).toString()));
            }
            IOStreamsHelper.removeLastInstanceOf(outputString, ','); // remove the last comma
          }
          outputString.append("},");
        } else {
          // no map found, so normal key value pair
          outputString.append(String.format("\\\"%s\\\":", categoryAttribute));
          outputString
              .append(String
                  .format("\\\"%s\\\",", categoryData.get(categoryAttribute).toString()));
        }
      }
      IOStreamsHelper.removeLastInstanceOf(outputString, ','); // remove the last comma
      outputString.append("},");
    }
    IOStreamsHelper.removeLastInstanceOf(outputString, ','); // remove the last comma
    outputString.append("]");

    if (success) {
      resultMessage = outputString.toString();
    }

    return new ResultStatus(success, resultMessage);
  }

  public ResultStatus deleteCategory(Map<String, Object> jsonMap) {
    //TODO remove all mappings in the groups table and in users table (https://github.com/SCCapstone/decision_maker/issues/108)
    ResultStatus resultStatus = new ResultStatus();
    if (
        jsonMap.containsKey(CATEGORY_ID) &&
            jsonMap.containsKey(RequestFields.ACTIVE_USER)
    ) {
      try {
        // Confirm that the username matches with the owner of the category before deleting it
        String username = (String) jsonMap.get((RequestFields.ACTIVE_USER));
        String categoryId = (String) jsonMap.get(CATEGORY_ID);

        GetItemSpec getItemSpec = new GetItemSpec()
            .withPrimaryKey(super.getPrimaryKeyIndex(), categoryId);
        Item item = super.getItem(getItemSpec);
        if (username.equals(item.getString(OWNER))) {
          DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
              .withPrimaryKey(super.getPrimaryKeyIndex(), categoryId);

          super.deleteItem(deleteItemSpec);

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
