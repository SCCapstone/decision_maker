package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.util.HashMap;
import java.util.List;
import utilities.ErrorDescriptor;
import utilities.JsonEncoders;
import utilities.Metrics;
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

  public UsersManager() {
    super("users", "Username", Regions.US_EAST_2);
  }

  public UsersManager(final DynamoDB dynamoDB) {
    super("users", "Username", Regions.US_EAST_2, dynamoDB);
  }

  public List<String> getAllCategoryIds(final String username, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    final String classMethod = "UsersManager.getAllCategoryIds";
    metrics.commonSetup(classMethod);

    List<String> categoryIds = new ArrayList<>();
    boolean success = false;

    try {
      final Item user = this.getItemByPrimaryKey(username);

      if (user != null) {
        Map<String, Object> userMapped = user.asMap(); // specific user record as a map
        Map<String, String> categoryMap = (Map<String, String>) userMapped.get(CATEGORIES);

        categoryIds = new ArrayList<>(categoryMap.keySet());
        success = true;
      } else {
        lambdaLogger.log(new ErrorDescriptor<>(username, classMethod, metrics.getRequestId(),
            "user lookup returned null").toString());
      }
    } catch (Exception e) {
      lambdaLogger.log(
          new ErrorDescriptor<>(username, classMethod, metrics.getRequestId(), e).toString());
    }

    metrics.commonClose(success);
    return categoryIds;
  }

  public List<String> getAllGroupIds(final String username, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    final String classMethod = "UsersManager.getAllGroupIds";
    metrics.commonSetup(classMethod);

    List<String> groupIds = new ArrayList<>();
    boolean success = false;

    try {
      Item user = this.getItemByPrimaryKey(username);

      if (user != null) {
        Map<String, Object> userMapped = user.asMap(); // specific user record as a map
        Map<String, String> groupMap = (Map<String, String>) userMapped.get(GROUPS);

        groupIds = new ArrayList<>(groupMap.keySet());
        success = true;
      } else {
        lambdaLogger.log(new ErrorDescriptor<>(username, classMethod, metrics.getRequestId(),
            "user lookup returned null").toString());
      }
    } catch (Exception e) {
      lambdaLogger.log(
          new ErrorDescriptor<>(username, classMethod, metrics.getRequestId(), e).toString());
    }

    metrics.commonClose(success);

    return groupIds;
  }

  public ResultStatus addNewUser(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();
    if (jsonMap.containsKey(USERNAME)) {
      try {
        String username = (String) jsonMap.get(USERNAME);

        Item user = this.getItemByPrimaryKey(username);
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

          this.putItem(putItemSpec);

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
            .withPrimaryKey(this.getPrimaryKeyIndex(), user)
            .withNameMap(nameMap)
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);

        this.updateItem(updateItemSpec);

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

  public ResultStatus updateUserAppSettings(Map<String, Object> jsonMap, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    ResultStatus resultStatus = new ResultStatus();
    final String classMethod = "UsersManager.updateUserAppSettings";
    metrics.commonSetup(classMethod);

    if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      try {
        String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        String settingToChange = null;
        if (jsonMap.containsKey(APP_SETTINGS_DARK_THEME)) {
          settingToChange = APP_SETTINGS_DARK_THEME;
        } else if (jsonMap.containsKey(APP_SETTINGS_MUTED)) {
          settingToChange = APP_SETTINGS_MUTED;
        } else if (jsonMap.containsKey(APP_SETTINGS_GROUP_SORT)) {
          settingToChange = APP_SETTINGS_GROUP_SORT;
        } else {
          lambdaLogger.log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
              "Invalid values for setting or user").toString());
          resultStatus.resultMessage = "Error: Invalid values for setting or user";
        }

        if (settingToChange != null) {
          Integer settingVal = (Integer) jsonMap.get(settingToChange);
          ValueMap valueMap = new ValueMap().withInt(":value", settingVal);
          if (checkAppSettingsVals(settingVal)) {
            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser)
                .withUpdateExpression("set " + APP_SETTINGS + "." + settingToChange + " = :value")
                .withValueMap(valueMap);

            this.updateItem(updateItemSpec);
            resultStatus = new ResultStatus(true, "User settings updated successfully!");
          } else {
            lambdaLogger
                .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
                    "Invalid values for app setting")
                    .toString());
            resultStatus.resultMessage = "Error: Invalid values for settings";
          }
        }
      } catch (Exception e) {
        lambdaLogger
            .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(), e).toString());
        resultStatus.resultMessage = "Error: Unable to parse request." + e;
      }
    } else {
      lambdaLogger.log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
          "Required request keys not found").toString());
      resultStatus.resultMessage = "Error: required request keys not found";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus getUserRatings(Map<String, Object> jsonMap, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    ResultStatus resultStatus = new ResultStatus();
    final String classMethod = "UsersManager.getUserRatings";
    metrics.commonSetup(classMethod);

    if (
        jsonMap.containsKey(RequestFields.ACTIVE_USER) &&
            jsonMap.containsKey(CategoriesManager.CATEGORY_ID)
    ) {
      try {
        String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        String categoryId = (String) jsonMap.get(CategoriesManager.CATEGORY_ID);

        GetItemSpec getItemSpec = new GetItemSpec()
            .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser);
        Item userDataRaw = this.getItem(getItemSpec);

        Map<String, Object> userCategories = (Map<String, Object>) userDataRaw.asMap()
            .get(UsersManager.CATEGORIES);

        Map<String, Object> userRatings = (Map<String, Object>) userCategories.get(categoryId);
        if (userRatings != null) {
          resultStatus = new ResultStatus(
              true,
              JsonEncoders.convertObjectToJson(userRatings));
        } else {
          lambdaLogger.log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
              "CategoryId produced a null value returned from DB.").toString());
          resultStatus.resultMessage = "Error with given categoryId: " + categoryId;
        }
      } catch (Exception e) {
        lambdaLogger
            .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(), e).toString());
        resultStatus.resultMessage = "Error: Unable to parse request. Exception message: " + e;
      }
    } else {
      lambdaLogger.log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
          "Required request keys not found").toString());
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus getUserAppSettings(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();

    if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      try {
        String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        GetItemSpec getItemSpec = new GetItemSpec()
            .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser);
        Item userDataRaw = this.getItem(getItemSpec);

        Map<String, Object> userSettings = (Map<String, Object>) userDataRaw.asMap()
            .get(UsersManager.APP_SETTINGS);

        resultStatus = new ResultStatus(true, JsonEncoders.convertObjectToJson(userSettings));
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

  private Map<String, Object> getDefaultAppSettings() {
    Map<String, Object> retMap = new HashMap<>();
    retMap.put(APP_SETTINGS_DARK_THEME, DEFAULT_DARK_THEME);
    retMap.put(APP_SETTINGS_MUTED, DEFAULT_MUTED);
    retMap.put(APP_SETTINGS_GROUP_SORT, DEFAULT_GROUP_SORT);
    return retMap;
  }

  private boolean checkAppSettingsVals(int settingVal) {
    return (settingVal == 0 || settingVal == 1);
  }
}