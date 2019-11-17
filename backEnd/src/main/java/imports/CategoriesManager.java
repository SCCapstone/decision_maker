package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import utilities.ExceptionHelper;
import utilities.IOStreamsHelper;
import utilities.ResultStatus;

import java.util.*;

public class CategoriesManager extends DatabaseAccessManager {

  public static final String CATEGORY_FIELD_CATEGORY_ID = "CategoryId";
  public static final String CATEGORY_FIELD_CATEGORY_NAME = "CategoryName";
  public static final String CATEGORY_FIELD_CHOICES = "Choices";
  public static final String CATEGORY_FIELD_GROUPS = "Groups";
  public static final String CATEGORY_FIELD_NEXT_CHOICE = "NextChoiceNo";
  public static final String CATEGORY_FIELD_OWNER = "Owner";

  public static final String REQUEST_FIELD_ACTIVE_USER = "ActiveUser";
  public static final String REQUEST_FIELD_CATEGORY_IDS = "CategoryIds";
  public static final String REQUEST_FIELD_USER_RATINGS = "UserRatings";

  private final UsersManager usersManager = new UsersManager();

  private UUID uuid;

  public CategoriesManager() {
    super("categories", "CategoryId", Regions.US_EAST_2);
  }

  public ResultStatus addNewCategory(Map<String, Object> jsonMap) {
    //validate data, log results as there should be some validation already on the front end
    ResultStatus resultStatus = new ResultStatus();
    if (
        jsonMap.containsKey(CATEGORY_FIELD_CATEGORY_NAME) &&
            jsonMap.containsKey(CATEGORY_FIELD_CHOICES) &&
            jsonMap.containsKey(REQUEST_FIELD_USER_RATINGS) &&
            jsonMap.containsKey(REQUEST_FIELD_ACTIVE_USER)
    ) {
      this.uuid = UUID.randomUUID();

      try {
        String nextCategoryIndex = this.uuid.toString();
        String categoryName = (String) jsonMap.get(CATEGORY_FIELD_CATEGORY_NAME);
        Map<String, Object> choices = (Map<String, Object>) jsonMap.get(CATEGORY_FIELD_CHOICES);
        Map<String, Object> groups = new HashMap<String, Object>();
        int nextChoiceNo = choices.size();
        String owner = (String) jsonMap.get(REQUEST_FIELD_ACTIVE_USER);

        Item newCategory = new Item()
            .withPrimaryKey(CATEGORY_FIELD_CATEGORY_ID, nextCategoryIndex)
            .withString(CATEGORY_FIELD_CATEGORY_NAME, categoryName)
            .withMap(CATEGORY_FIELD_CHOICES, choices)
            .withMap(CATEGORY_FIELD_GROUPS, groups)
            .withInt(CATEGORY_FIELD_NEXT_CHOICE, nextChoiceNo)
            .withString(CATEGORY_FIELD_OWNER, owner);

        PutItemSpec putItemSpec = new PutItemSpec()
            .withItem(newCategory);

        super.putItem(putItemSpec);

        resultStatus = new ResultStatus(true, "Category created successfully!");
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
        jsonMap.containsKey(CATEGORY_FIELD_CATEGORY_ID) &&
            jsonMap.containsKey(CATEGORY_FIELD_CHOICES) &&
            jsonMap.containsKey(REQUEST_FIELD_USER_RATINGS) &&
            jsonMap.containsKey(REQUEST_FIELD_ACTIVE_USER)
    ) {
      try {
        String categoryId = (String) jsonMap.get(CATEGORY_FIELD_CATEGORY_ID);
        Map<String, Object> choices = (Map<String, Object>) jsonMap.get(CATEGORY_FIELD_CHOICES);
        String activeUser = (String) jsonMap.get(REQUEST_FIELD_ACTIVE_USER);

        int nextChoiceNo = -1;

        //get the max current choiceNo
        for (String choiceNo : choices.keySet()) {
          if (Integer.parseInt(choiceNo) > nextChoiceNo) {
            nextChoiceNo = Integer.parseInt(choiceNo);
          }
        }

        //move the next choice to be the next value up from the max
        nextChoiceNo++;

        String updateExpression = "set " + CATEGORY_FIELD_CHOICES + "." + categoryId + " = :map";
        ValueMap valueMap = new ValueMap().withMap(":map", choices);
        
        UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(super.getPrimaryKeyIndex(), activeUser)
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);

        super.updateItem(updateItemSpec);

        resultStatus = new ResultStatus(true, "Category updated successfully!");
      } catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request." + '\n' + ExceptionHelper.getStackTrace(e);
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

    if (jsonMap.containsKey(REQUEST_FIELD_ACTIVE_USER)) {
      String username = (String) jsonMap.get(REQUEST_FIELD_ACTIVE_USER);
      categoryIds = this.usersManager.getAllCategoryIds(username);
    } else if (jsonMap.containsKey(REQUEST_FIELD_CATEGORY_IDS)) {
      categoryIds = (List<String>) jsonMap.get(REQUEST_FIELD_CATEGORY_IDS);
    } else {
      success = false;
      resultMessage = "Error: query key not defined.";
    }

    // this will be a json string representing an array of objects
    StringBuilder outputString = new StringBuilder("[");
    for (String id : categoryIds) {
      Item dbData = super.getItem(new GetItemSpec().withPrimaryKey(super.getPrimaryKeyIndex(), id));
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
              outputString.append(String.format("\\\"%s\\\",", mapAttribute.get(key).toString()));
            }
            IOStreamsHelper.removeLastInstanceOf(outputString, ','); // remove the last comma
          }
          outputString.append("},");
        } else {
          // no map found, so normal key value pair
          outputString.append(String.format("\\\"%s\\\":", categoryAttribute));
          outputString.append(String.format("\\\"%s\\\",", categoryData.get(categoryAttribute).toString()));
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
}
