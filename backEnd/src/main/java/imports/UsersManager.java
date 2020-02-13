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
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import utilities.ErrorDescriptor;
import utilities.JsonEncoders;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class UsersManager extends DatabaseAccessManager {

  public static final String USERNAME = "Username";
  public static final String DISPLAY_NAME = "DisplayName";
  public static final String ICON = "Icon";
  public static final String APP_SETTINGS = "AppSettings";
  public static final String APP_SETTINGS_DARK_THEME = "DarkTheme";
  public static final String APP_SETTINGS_MUTED = "Muted";
  public static final String APP_SETTINGS_GROUP_SORT = "GroupSort";
  public static final String GROUPS = "Groups";
  public static final String CATEGORIES = "Categories";
  public static final String FAVORITES = "Favorites";
  public static final String FAVORITE_OF = "FavoriteOf";

  public static final String DEFAULT_DISPLAY_NAME = "New User";
  public static final int DEFAULT_DARK_THEME = 0;
  public static final int DEFAULT_MUTED = 0;
  public static final int DEFAULT_GROUP_SORT = 0;

  public static final Map<String, Object> EMPTY_MAP = new HashMap<>();

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

  public ResultStatus getUserData(final Map<String, Object> jsonMap, Metrics metrics,
      LambdaLogger lambdaLogger) {
    final String classMethod = "UsersManager.getUserData";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();
    if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        Item user = this.getItemByPrimaryKey(activeUser);

        if (user == null) {
          user = new Item()
              .withString(USERNAME, activeUser)
              .withString(DISPLAY_NAME, DEFAULT_DISPLAY_NAME)
              .withMap(APP_SETTINGS, this.getDefaultAppSettings())
              .withMap(CATEGORIES, EMPTY_MAP)
              .withMap(GROUPS, EMPTY_MAP)
              .withMap(FAVORITES, EMPTY_MAP)
              .withMap(FAVORITE_OF, EMPTY_MAP);

          PutItemSpec putItemSpec = new PutItemSpec()
              .withItem(user);

          this.putItem(putItemSpec);
        }

        resultStatus = new ResultStatus(true, JsonEncoders.convertObjectToJson(user.asMap()));
      } catch (Exception e) {
        lambdaLogger
            .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(), e).toString());
        resultStatus.resultMessage = "Error: Unable to parse request. Exception message: ";
      }
    } else {
      lambdaLogger.log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
          "Required request keys not found").toString());
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus updateUserChoiceRatings(Map<String, Object> jsonMap, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    final String classMethod = "UsersManager.updateUserChoiceRatings";
    metrics.commonSetup(classMethod);

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
        lambdaLogger
            .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(), e).toString());
        resultStatus.resultMessage = "Error: Unable to parse request.";
      }
    } else {
      lambdaLogger.log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
          "Error: Required request keys not found.").toString());
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus updateUserSettings(Map<String, Object> jsonMap, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    final String classMethod = "UsersManager.updateUserAppSettings";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    /*
     If the user's display name or the icon changes:
       loop through a user's groups and favorites of and update accordingly
     If a user's Favorites change
       need to reach out and pull new favorites data in
       need to go out and delete favorites of map from removed favorites
     Maybe just blind update the app settings -> the code will definitely be simpler
     */

    try {
      String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
      String newDisplayName = (String) jsonMap.get(DISPLAY_NAME);
      String newIcon = (String) jsonMap.get(ICON);
      Map<String, Object> newAppSettings = (Map<String, Object>) jsonMap.get(APP_SETTINGS);
      Set<String> newFavorites = new HashSet<>(
          (List<String>) jsonMap.get(FAVORITES)); // note this comes in as list, in db is map

      Item userDataRaw = this.getItemByPrimaryKey(activeUser);
      Map<String, Object> user = userDataRaw.asMap();

      String oldDisplayName = (String) user.get(DISPLAY_NAME);
      String oldIcon = (String) user.get(ICON);
      Set<String> oldFavorites = new HashSet<>(((Map) user.get(FAVORITES)).keySet());

      //as long as this remains a small group of settings, I think it's okay to always overwrite
      //this does imply that the entire appSettings array is sent from the front end though
      String updateUserExpression = "set " + APP_SETTINGS + " = :appSettings";
      ValueMap userValueMap = new ValueMap().withMap(":appSettings", newAppSettings);

      String updateGroupsExpression = null;
      ValueMap groupsValueMap = new ValueMap();
      NameMap groupsNameMap = new NameMap();

      String updateFavoritesOfExpression = null;
      ValueMap favoritesOfValueMap = new ValueMap();
      NameMap favoritesOfNameMap = new NameMap();

      //determine if the display name/icon have changed (rn it's just display name)
      if (!oldDisplayName.equals(newDisplayName)) {
        updateUserExpression += ", " + DISPLAY_NAME + " = :name";
        userValueMap.withString(":name", newDisplayName);

        updateGroupsExpression = this.getUpdateString(updateGroupsExpression,
            GroupsManager.MEMBERS + ".#username." + DISPLAY_NAME, ":displayName");
        groupsValueMap.withString(":displayName", newDisplayName);
        groupsNameMap.with("#username", activeUser);

        updateFavoritesOfExpression = this
            .getUpdateString(updateFavoritesOfExpression, FAVORITES + ".#username." + DISPLAY_NAME,
                ":displayName");
        favoritesOfValueMap.withString(":displayName", newDisplayName);
        favoritesOfNameMap.with("#username", activeUser);
      }

      if (!oldIcon.equals(newIcon)) {
        updateUserExpression += ", " + ICON + " = :icon";
        userValueMap.withString(":icon", newIcon);

        updateGroupsExpression = this.getUpdateString(updateGroupsExpression,
            GroupsManager.MEMBERS + ".#username2." + ICON, ":icon");
        groupsValueMap.withString(":icon", newIcon);
        groupsNameMap.with("#username2", activeUser);

        updateFavoritesOfExpression = this
            .getUpdateString(updateFavoritesOfExpression, FAVORITES + ".#username2." + ICON,
                ":icon");
        favoritesOfValueMap.withString(":icon", newIcon);
        favoritesOfNameMap.with("#username2", activeUser);
      }

      UpdateItemSpec updateUserItemSpec = new UpdateItemSpec()
          .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser)
          .withUpdateExpression(updateUserExpression)
          .withValueMap(userValueMap);

      this.updateItem(updateUserItemSpec);

      if (updateGroupsExpression != null) {
        UpdateItemSpec updateGroupItemSpec;

        List<String> groupIds = new ArrayList<>(((Map) user.get(GROUPS)).keySet());
        for (String groupId : groupIds) {
          try {
            updateGroupItemSpec = new UpdateItemSpec()
                .withPrimaryKey(DatabaseManagers.GROUPS_MANAGER.getPrimaryKeyIndex(), groupId)
                .withUpdateExpression(updateGroupsExpression)
                .withValueMap(groupsValueMap)
                .withNameMap(groupsNameMap);

            DatabaseManagers.GROUPS_MANAGER.updateItem(updateGroupItemSpec);
          } catch (Exception e) {
            lambdaLogger.log(
                new ErrorDescriptor<>(groupId, classMethod, metrics.getRequestId(), e).toString());
          }
        }
      }

      if (updateFavoritesOfExpression != null) {
        UpdateItemSpec updateFavoritesOfItemSpec;

        List<String> usernamesToUpdate = new ArrayList<>(((Map) user.get(FAVORITE_OF)).keySet());
        for (String username : usernamesToUpdate) {
          try {
            updateFavoritesOfItemSpec = new UpdateItemSpec()
                .withPrimaryKey(this.getPrimaryKeyIndex(), username)
                .withUpdateExpression(updateFavoritesOfExpression)
                .withValueMap(favoritesOfValueMap)
                .withNameMap(favoritesOfNameMap);

            this.updateItem(updateFavoritesOfItemSpec);
          } catch (Exception e) {
            lambdaLogger.log(
                new ErrorDescriptor<>(username, classMethod, metrics.getRequestId(), e).toString());
          }
        }
      }

      this.updateActiveUsersFavorites(newFavorites, oldFavorites, activeUser, metrics,
          lambdaLogger);

      Item updatedUser = this.getItemByPrimaryKey(activeUser);

      resultStatus = new ResultStatus(true, JsonEncoders.convertObjectToJson(updatedUser.asMap()));
    } catch (Exception e) {
      lambdaLogger
          .log(new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(), e).toString());
      resultStatus.resultMessage = "Error: Unable to parse request.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private boolean updateActiveUsersFavorites(final Set<String> newFavorites,
      final Set<String> oldFavorites, final String activeUser, final Metrics metrics,
      final LambdaLogger lambdaLogger) {
    final String classMethod = "UsersManager.updateActiveUsersFavorites";
    metrics.commonSetup(classMethod);

    boolean hadError = false;

    if (!newFavorites.containsAll(oldFavorites)) {
      final Set<String> removedUsernames = new HashSet<>(oldFavorites);
      removedUsernames.removeAll(newFavorites);

      UpdateItemSpec updateDeletedFavoritesItemSpec;
      final String deleteFavoriteOfExpression = "remove " + FAVORITE_OF + ".#activeUser";
      final NameMap deleteFavoriteOfNameMap = new NameMap().with("#activeUser", activeUser);

      UpdateItemSpec updateFavoritesItemSpec;
      final String updateFavoriteExpression =
          "remove " + FAVORITES + ".#oldFavoriteUser";

      for (String username : removedUsernames) {
        try {
          updateDeletedFavoritesItemSpec = new UpdateItemSpec()
              .withPrimaryKey(this.getPrimaryKeyIndex(), username)
              .withUpdateExpression(deleteFavoriteOfExpression)
              .withNameMap(deleteFavoriteOfNameMap);

          this.updateItem(updateDeletedFavoritesItemSpec);

          updateFavoritesItemSpec = new UpdateItemSpec()
              .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser)
              .withUpdateExpression(updateFavoriteExpression)
              .withNameMap(new NameMap().with("#oldFavoriteUser", username));

          this.updateItem(updateFavoritesItemSpec);
        } catch (Exception e) {
          hadError = true;
          lambdaLogger.log(
              new ErrorDescriptor<>(username, classMethod, metrics.getRequestId(), e).toString());
        }
      }
    }

    if (!oldFavorites.containsAll(newFavorites)) {
      final Set<String> addedUsernames = new HashSet<>(newFavorites);
      addedUsernames.removeAll(oldFavorites);

      //first we add all of the favorites of mapping to the new user items
      UpdateItemSpec updateFavoritesOfItemSpec;
      final String updateFavoriteOfExpression = "set " + FAVORITE_OF + ".#activeUser = :true";
      final NameMap updateFavoriteOfNameMap = new NameMap().with("#activeUser", activeUser);
      final ValueMap updateFavoriteOfValueMap = new ValueMap().with(":true", true);

      UpdateItemSpec updateFavoritesItemSpec;
      final String updateFavoriteExpression =
          "set " + FAVORITES + ".#newFavoriteUser = :newFavorite";

      for (String username : addedUsernames) {
        try {
          //TODO put these two updates into a executeWriteTransaction statement

          //add the 'favorites of' mapping to the other user's item
          updateFavoritesOfItemSpec = new UpdateItemSpec()
              .withPrimaryKey(this.getPrimaryKeyIndex(), username)
              .withUpdateExpression(updateFavoriteOfExpression)
              .withNameMap(updateFavoriteOfNameMap)
              .withValueMap(updateFavoriteOfValueMap);

          this.updateItem(updateFavoritesOfItemSpec);

          //add the other user's data to the active user's 'favorites' map
          Item newFavoriteUser = this.getItemByPrimaryKey(username);
          Map<String, Object> newFavoriteUserMapped = newFavoriteUser.asMap();

          updateFavoritesItemSpec = new UpdateItemSpec()
              .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser)
              .withUpdateExpression(updateFavoriteExpression)
              .withNameMap(new NameMap().with("#newFavoriteUser", username))
              .withValueMap(new ValueMap().withMap(":newFavorite", ImmutableMap.of(
                  DISPLAY_NAME, (String) newFavoriteUserMapped.get(DISPLAY_NAME),
                  ICON, (String) newFavoriteUserMapped.get(ICON)
              )));

          this.updateItem(updateFavoritesItemSpec);
        } catch (Exception e) {
          hadError = true;
          lambdaLogger.log(
              new ErrorDescriptor<>(username, classMethod, metrics.getRequestId(), e).toString());
        }
      }
    }

    metrics.commonClose(!hadError);
    return (!hadError);
  }

  private String getUpdateString(String current, String key, String valueName) {
    if (current != null) {
      return current + ", " + key + " = " + valueName;
    } else {
      return "set " + key + " = " + valueName;
    }
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

  private Map<String, Object> getDefaultAppSettings() {
    Map<String, Object> retMap = new HashMap<>();
    retMap.put(APP_SETTINGS_DARK_THEME, DEFAULT_DARK_THEME);
    retMap.put(APP_SETTINGS_MUTED, DEFAULT_MUTED);
    retMap.put(APP_SETTINGS_GROUP_SORT, DEFAULT_GROUP_SORT);
    return retMap;
  }
}