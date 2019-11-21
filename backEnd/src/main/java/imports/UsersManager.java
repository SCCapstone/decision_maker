package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import java.util.HashMap;
import utilities.RequestFields;
import utilities.ResultStatus;

import java.util.ArrayList;
import java.util.Map;

public class UsersManager extends DatabaseAccessManager {

  public static final String USERNAME = "Username";
  public static final String FIRST_NAME = "FirstName";
  public static final String LAST_NAME = "LastName";
  public static final String APP_SETTING_DARK_THEME = "AppSetting_DarkTheme";
  public static final String APP_SETTING_MUTED = "AppSetting_Muted";
  public static final String GROUPS = "Groups";
  public static final String CATEGORIES = "Categories";

  public static final String DEFAULT_FIRSTNAME = "DefFirstName";
  public static final String DEFAULT_LASTNAME = "DefLastName";
  public static final boolean DEFAULT_DARK_THEME = false;
  public static final boolean DEFAULT_MUTED = false;

  public static final Map EMPTY_MAP = new HashMap();

  public UsersManager() {
    super("users", "Username", Regions.US_EAST_2);
  }

  public ArrayList<String> getAllCategoryIds(String username) {
    Item dbData = super
        .getItem(new GetItemSpec().withPrimaryKey(super.getPrimaryKeyIndex(), username));

    Map<String, Object> dbDataMap = dbData.asMap(); // specific user record as a map
    Map<String, String> categoryMap = (Map<String, String>) dbDataMap.get(CATEGORIES);

    return new ArrayList<String>(categoryMap.keySet());
  }

  public ArrayList<String> getAllGroupIds(String username) {
    Item dbData = super
        .getItem(new GetItemSpec().withPrimaryKey(super.getPrimaryKeyIndex(), username));

    Map<String, Object> dbDataMap = dbData.asMap(); // specific user record as a map
    Map<String, String> categoryMap = (Map<String, String>) dbDataMap.get(GROUPS);

    return new ArrayList<String>(categoryMap.keySet());
  }

  public boolean checkUser(String userName) {
    Item newItem;
    try {
      newItem = super
          .getItem(new GetItemSpec().withPrimaryKey(super.getPrimaryKeyIndex(), userName));
      if (newItem == null) {
        return true;
      }
      return false;
    } catch (ResourceNotFoundException e) {
      return false;
    }
  }

  public ResultStatus addNewUser(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();
    if (jsonMap.containsKey(USERNAME)) {
      try {
        String userName = (String) jsonMap.get(USERNAME);

        if (this.checkUser(userName)) {
          Item newUser = new Item()
              .withString(USERNAME, userName)
              .withString(FIRST_NAME, DEFAULT_FIRSTNAME)
              .withString(LAST_NAME, DEFAULT_LASTNAME)
              .withBoolean(APP_SETTING_DARK_THEME, DEFAULT_DARK_THEME)
              .withBoolean(APP_SETTING_MUTED, DEFAULT_MUTED)
              .withMap(CATEGORIES, EMPTY_MAP)
              .withMap(GROUPS, EMPTY_MAP);

          PutItemSpec putItemSpec = new PutItemSpec()
              .withItem(newUser);

          super.putItem(putItemSpec);

          resultStatus = new ResultStatus(true, "User added successfully!");
        } else {
          resultStatus.resultMessage = "Error: Username already exists in database";
        }
      } catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request. Exception message: " + e;
      }
    } else {
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    return resultStatus;
  }

  public ResultStatus updateUserChoiceRatings(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();

    if (
        jsonMap.containsKey(RequestFields.ACTIVE_USER) &&
            jsonMap.containsKey(CategoriesManager.CATEGORY_ID) &&
            jsonMap.containsKey(RequestFields.USER_RATINGS)
    ) {
      try {
        String user = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        String categoryId = (String) jsonMap.get(CategoriesManager.CATEGORY_ID);
        Map<String, Object> ratings = (Map<String, Object>) jsonMap.get(RequestFields.USER_RATINGS);

        String updateExpression = "set " + CATEGORIES + ".#categoryId = :map";
        NameMap nameMap = new NameMap().with("#categoryId", categoryId);
        ValueMap valueMap = new ValueMap().withMap(":map", ratings);

        UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(super.getPrimaryKeyIndex(), user)
            .withNameMap(nameMap)
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);

        super.updateItem(updateItemSpec);

        resultStatus = new ResultStatus(true, "User ratings updated successfully!");
      } catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request. Exception message: " + e;
      }
    } else {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    return resultStatus;
  }
}
