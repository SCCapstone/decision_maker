package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
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

  private final UsersManager usersManager = new UsersManager();
  public static final String USERNAME_FIELD = "Username";
  public static final String GET_ALL_FIELD = "GetAll";

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
            jsonMap.containsKey(CATEGORY_FIELD_OWNER)
    ) {
      this.uuid = UUID.randomUUID();

      try {
        String nextCategoryIndex = this.uuid.toString();
        String categoryName = (String) jsonMap.get(CATEGORY_FIELD_CATEGORY_NAME);
        Map<String, Object> choices = (Map<String, Object>) jsonMap.get(CATEGORY_FIELD_CHOICES);
        Map<String, Object> groups = new HashMap<String, Object>();
        int nextChoiceNo = choices.size();
        String owner = (String) jsonMap.get(CATEGORY_FIELD_OWNER);

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
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    return resultStatus;
  }

  public ResultStatus editCategory(Map<String, Object> jsonMap) {
    //validate data, log results as there should be some validation already on the front end
    return new ResultStatus();
  }

  public ResultStatus getCategories(Map<String, Object> jsonMap) {
    boolean success = true;
    String resultMessage = "";
    List<String> categoryIds = new ArrayList<String>();

    if (jsonMap.containsKey("Username")) {
      String username = (String) jsonMap.get(USERNAME_FIELD);
      categoryIds = this.usersManager.getAllCategoryIds(username);
    } else if (jsonMap.containsKey("CategoryIds")) {
      categoryIds = (List<String>) jsonMap.get("CategoryIds");
    } else {
      success = false;
      resultMessage = "Error: query key not defined.";
    }

    // this will be a json string representing an array of objects
    StringBuilder outputString = new StringBuilder("[");
    for (String id : categoryIds) {
      Item dbData = super.getItem(new GetItemSpec().withPrimaryKey(super.getPrimaryKeyIndex(), id));
      Map<String, Object> dbDataMap = dbData.asMap();
      outputString.append("{");
      for (String s : dbDataMap.keySet()) {
        Object value = dbDataMap.get(s);
        if (value instanceof Map) {
          // found a map in the object, so now loop through each key/value in said map and format appropriately
          outputString.append(String.format("\\\"%s\\\":", s));
          Map<Object, Object> map = (Map<Object, Object>) value;
          outputString.append("{");
          for (Object key : map.keySet()) {
            outputString.append(String.format("\\\"%s\\\":", key.toString()));
            outputString.append(String.format("\\\"%s\\\",", map.get(key).toString()));
          }
          outputString
              .deleteCharAt(outputString.toString().lastIndexOf(",")); // remove the last comma
          outputString.append("},");
        } else if(value instanceof String) {
          // no map found, so normal key value pair
          outputString.append(String.format("\\\"%s\\\":", s));
          outputString.append(String.format("\\\"%s\\\",", dbDataMap.get(s).toString()));
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
