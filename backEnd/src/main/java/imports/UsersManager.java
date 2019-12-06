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
import java.util.List;
import utilities.JsonEncoders;
import utilities.RequestFields;
import utilities.ResultStatus;

import java.util.ArrayList;
import java.util.Map;

public class UsersManager extends DatabaseAccessManager {

  public static final String USERNAME = "Username";
  public static final String FIRST_NAME = "FirstName";
  public static final String LAST_NAME = "LastName";
  public static final String APP_SETTINGS = "AppSettings";
  public static final String APP_SETTINGS_DARK_THEME = "DarkTheme";
  public static final String APP_SETTINGS_MUTED = "Muted";
  public static final String APP_SETTINGS_GROUP_SORT = "GroupSort";
  public static final String GROUPS = "Groups";
  public static final String CATEGORIES = "Categories";

  public static final String DEFAULT_FIRSTNAME = "DefFirstName";
  public static final String DEFAULT_LASTNAME = "DefLastName";
  public static final int DEFAULT_DARK_THEME = 0;
  public static final int DEFAULT_MUTED = 0;
  public static final int DEFAULT_GROUP_SORT = 0;

  public static final Map EMPTY_MAP = new HashMap();
  public static final UsersManager USERS_MANAGER = new UsersManager();

  public UsersManager() {
    super("users", "Username", Regions.US_EAST_2);
  }

  public static List<String> getAllCategoryIds(String username) {
    Item dbData = USERS_MANAGER
        .getItem(new GetItemSpec().withPrimaryKey(USERS_MANAGER.getPrimaryKeyIndex(), username));

    Map<String, Object> dbDataMap = dbData.asMap(); // specific user record as a map
    Map<String, String> categoryMap = (Map<String, String>) dbDataMap.get(CATEGORIES);

    return new ArrayList<>(categoryMap.keySet());
  }

  public static List<String> getAllGroupIds(String username) {
    Item dbData = USERS_MANAGER
        .getItem(new GetItemSpec().withPrimaryKey(USERS_MANAGER.getPrimaryKeyIndex(), username));

    Map<String, Object> dbDataMap = dbData.asMap(); // specific user record as a map
    Map<String, String> groupMap = (Map<String, String>) dbDataMap.get(GROUPS);

    return new ArrayList<>(groupMap.keySet());
  }

  public static Item getUser(String username) {
    return USERS_MANAGER.getItemByPrimaryKey(username);
  }

  public static ResultStatus addNewUser(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();
    if (jsonMap.containsKey(USERNAME)) {
      try {
        String username = (String) jsonMap.get(USERNAME);

        Item user = UsersManager.getUser(username);
        if (user == null) {
          Item newUser = new Item()
              .withString(USERNAME, username)
              .withString(FIRST_NAME, DEFAULT_FIRSTNAME)
              .withString(LAST_NAME, DEFAULT_LASTNAME)
              .withMap(APP_SETTINGS, getDefaultAppSettings())
              .withMap(CATEGORIES, EMPTY_MAP)
              .withMap(GROUPS, EMPTY_MAP);

          PutItemSpec putItemSpec = new PutItemSpec()
              .withItem(newUser);

          USERS_MANAGER.putItem(putItemSpec);

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

  public static ResultStatus updateUserChoiceRatings(Map<String, Object> jsonMap) {
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
            .withPrimaryKey(USERS_MANAGER.getPrimaryKeyIndex(), user)
            .withNameMap(nameMap)
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);

        USERS_MANAGER.updateItem(updateItemSpec);

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
  
  public static ResultStatus updateUserAppSettings(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();
    
    if (jsonMap.containsKey(RequestFields.ACTIVE_USER) && ((jsonMap.containsKey(APP_SETTINGS_MUTED)) ||
           (jsonMap.containsKey(APP_SETTINGS_DARK_THEME)) || (jsonMap.containsKey(APP_SETTINGS_GROUP_SORT)))) {
      try {
        String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        String settingToChange = "";
        ValueMap valueMap = new ValueMap();
        if (jsonMap.containsKey(APP_SETTINGS_DARK_THEME)) {
          settingToChange = APP_SETTINGS_DARK_THEME;
        } else if (jsonMap.containsKey(APP_SETTINGS_MUTED)) {
          settingToChange = APP_SETTINGS_MUTED;
        } else if (jsonMap.containsKey(APP_SETTINGS_GROUP_SORT)) {
          settingToChange = APP_SETTINGS_GROUP_SORT;
        } else {
          //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
          resultStatus.resultMessage = "Error: Invalid values for setting or user";
          return resultStatus;
        }
        
        Integer settingVal = (Integer) jsonMap.get(settingToChange);
        
        if (checkAppSettingsVals(settingToChange, settingVal)) {
          valueMap.withInt(":value", settingVal);
        
          UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withPrimaryKey(USERS_MANAGER.getPrimaryKeyIndex(), activeUser)
              .withUpdateExpression("set " + APP_SETTINGS + "." + settingToChange + " = :value")
              .withValueMap(valueMap);
        
          USERS_MANAGER.updateItem(updateItemSpec);
          resultStatus = new ResultStatus(true, "User settings updated successfully!");
        } else {
          resultStatus.resultMessage = "Error: Invalid values for settings";
        }
      }
      catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
       resultStatus.resultMessage = "Error: Unable to parse request."; 
      }
    } else {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: required request keys not found";
    }
    return resultStatus;
  }

  public static ResultStatus getUserRatings(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();

    if (
        jsonMap.containsKey(RequestFields.ACTIVE_USER) &&
            jsonMap.containsKey(CategoriesManager.CATEGORY_ID)
    ) {
      try {
        String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        String categoryId = (String) jsonMap.get(CategoriesManager.CATEGORY_ID);

        GetItemSpec getItemSpec = new GetItemSpec()
            .withPrimaryKey(USERS_MANAGER.getPrimaryKeyIndex(), activeUser);
        Item userDataRaw = USERS_MANAGER.getItem(getItemSpec);

        Map<String, Object> userRatings = (Map<String, Object>) userDataRaw.asMap()
            .get(UsersManager.CATEGORIES);

        resultStatus = new ResultStatus(
            true,
            JsonEncoders.convertObjectToJson(userRatings.get(categoryId)));
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
  
  public static ResultStatus getUserAppSettings(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();
    
    if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      try {
        String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        
        GetItemSpec getItemSpec = new GetItemSpec()
            .withPrimaryKey(USERS_MANAGER.getPrimaryKeyIndex(), activeUser);
        Item userDataRaw = USERS_MANAGER.getItem(getItemSpec);
        
        Map<String, Object> userSettings = (Map<String, Object>) userDataRaw.asMap()
          .get(UsersManager.APP_SETTINGS);
        
        resultStatus = new ResultStatus(true, JsonEncoders.convertObjectToJson(userSettings));
      }
      catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request. Exception message: " + e;
      }
    }
    else {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }
    
    return resultStatus;
  }
  
  private static Map<String, Object> getDefaultAppSettings(){
    Map<String, Object> retMap = new HashMap<String, Object>();
    retMap.put(APP_SETTINGS_DARK_THEME, DEFAULT_DARK_THEME);
    retMap.put(APP_SETTINGS_MUTED, DEFAULT_MUTED);
    retMap.put(APP_SETTINGS_GROUP_SORT, DEFAULT_GROUP_SORT);
    return retMap;
  }
  
  private static boolean checkAppSettingsVals(String setting, int settingVal) {
    boolean retbool = false;
    
    if (setting.equals(APP_SETTINGS_DARK_THEME)) {
      if (settingVal == 0 || settingVal == 1) {
      retbool = true;
      } 
    } else if (setting.equals(APP_SETTINGS_MUTED)) {
      if (settingVal == 0 || settingVal == 1) {
      retbool = true;
      }
    } else if (setting.equals(APP_SETTINGS_GROUP_SORT)) {
      if (settingVal == 0 || settingVal == 1) {
      retbool = true;
      }
    }
    return retbool;
  }             
}