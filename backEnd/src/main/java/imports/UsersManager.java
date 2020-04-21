package imports;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import exceptions.InvalidAttributeValueException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import models.AppSettings;
import models.User;
import utilities.Config;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class UsersManager extends DatabaseAccessManager {

  public static final String USERNAME = "Username";
  public static final String DISPLAY_NAME = "DisplayName";
  public static final String ICON = "Icon";
  public static final String APP_SETTINGS = "AppSettings";
  public static final String APP_SETTINGS_DARK_THEME = "DarkTheme";
  public static final String APP_SETTINGS_MUTED = "Muted";
  public static final String APP_SETTINGS_GROUP_SORT = "GroupSort";
  public static final String APP_SETTINGS_CATEGORY_SORT = "CategorySort";
  public static final String GROUPS = "Groups";
  public static final String GROUPS_LEFT = "GroupsLeft";
  public static final String CATEGORY_RATINGS = "CategoryRatings";
  public static final String OWNED_CATEGORIES = "OwnedCategories";
  public static final String FAVORITES = "Favorites";
  public static final String FAVORITE_OF = "FavoriteOf";
  public static final String PUSH_ENDPOINT_ARN = "PushEndpointArn";
  public static final String EVENTS_UNSEEN = "EventsUnseen";
  public static final String FIRST_LOGIN = "FirstLogin";

  public static final String DEFAULT_DISPLAY_NAME = "New User";
  public static final boolean DEFAULT_DARK_THEME = true;
  public static final boolean DEFAULT_MUTED = false;
  public static final int DEFAULT_GROUP_SORT = 0;
  public static final int DEFAULT_CATEGORY_SORT = 1;

  public static final int MAX_DISPLAY_NAME_LENGTH = 40;

  public UsersManager() {
    super("users", "Username", Regions.US_EAST_2);
  }

  public UsersManager(final DynamoDB dynamoDB) {
    super("users", "Username", Regions.US_EAST_2, dynamoDB);
  }

  public List<String> getAllOwnedCategoryIds(final String username, final Metrics metrics) {
    final String classMethod = "UsersManager.getAllOwnedCategoryIds";
    metrics.commonSetup(classMethod);

    List<String> categoryIds = new ArrayList<>();
    boolean success = false;

    try {
      final User user = new User(this.getItemByPrimaryKey(username).asMap());

      categoryIds = new ArrayList<>(user.getOwnedCategories().keySet());
      success = true;
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>(username, classMethod, e));
    }

    metrics.commonClose(success);
    return categoryIds;
  }

  public List<String> getAllGroupIds(final String username, final Metrics metrics) {
    final String classMethod = "UsersManager.getAllGroupIds";
    metrics.commonSetup(classMethod);

    List<String> groupIds = new ArrayList<>();
    boolean success = false;

    try {
      final User user = new User(this.getItemByPrimaryKey(username).asMap());

      groupIds = new ArrayList<>(user.getGroups().keySet());
      success = true;
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>(username, classMethod, e));
    }

    metrics.commonClose(success);

    return groupIds;
  }

  /**
   * This method does one of two things: 1) It gets the active user's data. If the active user's
   * data does not exist, we assume this is their first login and we enter a new user object in the
   * db. 2) It gets another user's data based on the passed in USERNAME key. If the requested user's
   * data does not exist, the magic string of 'User not found.' is returned.
   *
   * @param jsonMap Common request map from endpoint handler containing api input
   * @param metrics Standard metrics object for profiling and logging
   * @return Standard result status object giving insight on whether the request was successful
   */
  public ResultStatus getUserData(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "UsersManager.getUserData";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    if (jsonMap.containsKey(UsersManager.USERNAME)) {
      final String otherUser = (String) jsonMap.get(UsersManager.USERNAME);
      Item user = this.getItemByPrimaryKey(otherUser);
      if (user != null) {
        resultStatus = new ResultStatus(true, JsonUtils.convertObjectToJson(user.asMap()));
      } else {
        resultStatus = new ResultStatus(true, "User not found.");
      }
    } else if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        Item user = this.getItemByPrimaryKey(activeUser);

        if (user == null) {
          user = new Item()
              .withString(USERNAME, activeUser)
              .withString(DISPLAY_NAME, DEFAULT_DISPLAY_NAME)
              .withNull(ICON)
              .withMap(APP_SETTINGS, AppSettings.defaultSettings().asMap())
              .withMap(CATEGORY_RATINGS, Collections.emptyMap())
              .withMap(OWNED_CATEGORIES, Collections.emptyMap())
              .withMap(GROUPS, Collections.emptyMap())
              .withMap(GROUPS_LEFT, Collections.emptyMap())
              .withMap(FAVORITES, Collections.emptyMap())
              .withMap(FAVORITE_OF, Collections.emptyMap());

          this.putItem(user);

          //note: this needs to come after the put item as we don't need to store this info in the db
          user.withBoolean(FIRST_LOGIN, true);
        } else {
          user.withBoolean(FIRST_LOGIN, false);
        }

        resultStatus = new ResultStatus(true, JsonUtils.convertObjectToJson(user.asMap()));
      } catch (Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus.resultMessage = "Error: Unable to parse request. Exception message: ";
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found"));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This method takes in the active user, a category id, and a map of choice rating. Using this
   * information if updates a users CategoryRatings attribute map to contain the appropriate ratings
   * for the categoryId. It merges old choice ratings with new ones to preserve historical data.
   *
   * @param jsonMap Common request map from endpoint handler containing api input
   * @param metrics Standard metrics object for profiling and logging
   * @return Standard result status object giving insight on whether the request was successful
   */
  public ResultStatus updateUserChoiceRatings(final Map<String, Object> jsonMap,
      final Metrics metrics) {
    return this.updateUserChoiceRatings(jsonMap, false, metrics);
  }

  //Same doc as above with the addition of the 'isNewCategory' param. This param tell the function
  //that the category has just been created and therefore the active user is the owner
  public ResultStatus updateUserChoiceRatings(final Map<String, Object> jsonMap,
      final boolean isNewCategory, final Metrics metrics) {
    final String classMethod = "UsersManager.updateUserChoiceRatings";
    metrics.commonSetup(classMethod);

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, CategoriesManager.CATEGORY_ID,
            RequestFields.USER_RATINGS);

    ResultStatus resultStatus = new ResultStatus();
    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String categoryId = (String) jsonMap.get(CategoriesManager.CATEGORY_ID);
        final Map<String, Object> ratings = (Map<String, Object>) jsonMap
            .get(RequestFields.USER_RATINGS);

        final Optional<String> errorMessage = this.userRatingsIsValid(ratings);
        if (!errorMessage.isPresent()) {
          final Map<String, Integer> ratingsMapConverted = ratings.entrySet().stream()
              .collect(collectingAndThen(
                  toMap(Entry::getKey, (Map.Entry e) -> Integer.parseInt(e.getValue().toString())),
                  HashMap::new));

          final User user = new User(this.getMapByPrimaryKey(activeUser));
          if (user.getCategoryRatings().containsKey(categoryId)) {
            //we need to apply the existing ratings to the updated ratings
            final Map<String, Integer> categoryRatings = user.getCategoryRatings().get(categoryId);
            for (final String choiceId : categoryRatings.keySet()) {
              if (!ratingsMapConverted.containsKey(choiceId)) {
                //we only put it if it ins't there; if it is there, they're overwriting it
                ratingsMapConverted.put(choiceId, categoryRatings.get(choiceId));
              }
            }
          }

          String updateExpression = "set " + CATEGORY_RATINGS + ".#categoryId = :map";
          NameMap nameMap = new NameMap().with("#categoryId", categoryId);
          ValueMap valueMap = new ValueMap().withMap(":map", ratingsMapConverted);

          if ((isNewCategory || user.getOwnedCategories().containsKey(categoryId)) && jsonMap
              .containsKey(CategoriesManager.CATEGORY_NAME)) {
            final String categoryName = (String) jsonMap.get(CategoriesManager.CATEGORY_NAME);
            updateExpression += ", " + OWNED_CATEGORIES + ".#categoryId = :categoryName";
            valueMap.withString(":categoryName", categoryName);
          }

          UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser)
              .withNameMap(nameMap)
              .withUpdateExpression(updateExpression)
              .withValueMap(valueMap);

          this.updateItem(updateItemSpec);

          resultStatus = new ResultStatus(true, "User ratings updated successfully!");
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

  private Optional<String> userRatingsIsValid(final Map<String, Object> ratings) {
    String errorMessage = null;

    try {
      for (final String choiceId : ratings.keySet()) {
        final Integer rating = Integer.parseInt(ratings.get(choiceId).toString());

        if (rating < 0 || rating > 5) {
          errorMessage = "Error: invalid rating value.";
          break;
        }
      }
    } catch (final Exception e) {
      errorMessage = "Error: invalid ratings map.";
    }

    return Optional.ofNullable(errorMessage);
  }

  /**
   * This method handles the api request to update a users settings such as display name, icon,
   * favorites, and other app settings.
   *
   * @param jsonMap Common request map from endpoint handler containing api input
   * @param metrics Standard metrics object for profiling and logging
   * @return Standard result status object giving insight on whether the request was successful
   */
  public ResultStatus updateUserSettings(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "UsersManager.updateUserSettings";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    /*
     If the user's display name or the icon changes:
       loop through a user's groups and favorites-of and update accordingly
     If a user's Favorites change
       need to reach out and pull new favorites data in
       need to go out and delete favorites of map from removed favorites
     Blind update the app settings to simplify the code
     */

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, DISPLAY_NAME, APP_SETTINGS, FAVORITES);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        String newDisplayName = (String) jsonMap.get(DISPLAY_NAME);
        AppSettings newAppSettings = new AppSettings(
            (Map<String, Object>) jsonMap.get(APP_SETTINGS));
        Set<String> newFavorites = new HashSet<>(
            (List<String>) jsonMap.get(FAVORITES)); // note this comes in as list, in db is map

        final Optional<String> errorMessage = this.userSettingsIsValid(newDisplayName);
        if (!errorMessage.isPresent()) {
          final User oldUser = new User(this.getItemByPrimaryKey(activeUser).asMap());

          //as long as this remains a small group of settings, I think it's okay to always overwrite
          //this does imply that the entire appSettings array is sent from the front end though
          String updateUserExpression = "set " + APP_SETTINGS + " = :appSettings";
          ValueMap userValueMap = new ValueMap().withMap(":appSettings", newAppSettings.asMap());

          String updateGroupsExpression = null;
          ValueMap groupsValueMap = new ValueMap();
          NameMap groupsNameMap = new NameMap();

          String updateFavoritesOfExpression = null;
          ValueMap favoritesOfValueMap = new ValueMap();
          NameMap favoritesOfNameMap = new NameMap();

          //determine if the display name/icon have changed
          if (!oldUser.getDisplayName().equals(newDisplayName)) {
            updateUserExpression += ", " + DISPLAY_NAME + " = :name";
            userValueMap.withString(":name", newDisplayName);

            updateGroupsExpression = this.getUpdateString(updateGroupsExpression,
                GroupsManager.MEMBERS + ".#username." + DISPLAY_NAME, ":displayName");
            groupsValueMap.withString(":displayName", newDisplayName);
            groupsNameMap.with("#username", activeUser);

            updateFavoritesOfExpression = this
                .getUpdateString(updateFavoritesOfExpression,
                    FAVORITES + ".#username." + DISPLAY_NAME,
                    ":displayName");
            favoritesOfValueMap.withString(":displayName", newDisplayName);
            favoritesOfNameMap.with("#username", activeUser);
          }

          //ICON is an optional api payload key, if present it's assumed it has the contents of a new file for upload
          if (jsonMap.containsKey(ICON)) {
            List<Integer> newIcon = (List<Integer>) jsonMap.get(ICON);

            //try to create the file in s3, if no filename returned, throw exception
            String newIconFileName = DatabaseManagers.S3_ACCESS_MANAGER
                .uploadImage(newIcon, metrics)
                .orElseThrow(Exception::new);

            updateUserExpression += ", " + ICON + " = :icon";
            userValueMap.withString(":icon", newIconFileName);

            updateGroupsExpression = this.getUpdateString(updateGroupsExpression,
                GroupsManager.MEMBERS + ".#username2." + ICON, ":icon");
            groupsValueMap.withString(":icon", newIconFileName);
            groupsNameMap.with("#username2", activeUser);

            updateFavoritesOfExpression = this
                .getUpdateString(updateFavoritesOfExpression, FAVORITES + ".#username2." + ICON,
                    ":icon");
            favoritesOfValueMap.withString(":icon", newIconFileName);
            favoritesOfNameMap.with("#username2", activeUser);
          }

          UpdateItemSpec updateUserItemSpec = new UpdateItemSpec()
              .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser)
              .withUpdateExpression(updateUserExpression)
              .withValueMap(userValueMap);

          this.updateItem(updateUserItemSpec);

          if (updateGroupsExpression != null) {
            UpdateItemSpec updateGroupItemSpec;

            for (String groupId : oldUser.getGroups().keySet()) {
              try {
                updateGroupItemSpec = new UpdateItemSpec()
                    .withPrimaryKey(DatabaseManagers.GROUPS_MANAGER.getPrimaryKeyIndex(), groupId)
                    .withUpdateExpression(updateGroupsExpression)
                    .withValueMap(groupsValueMap)
                    .withNameMap(groupsNameMap);

                DatabaseManagers.GROUPS_MANAGER.updateItem(updateGroupItemSpec);
              } catch (Exception e) {
                metrics.log(new ErrorDescriptor<>(groupId, classMethod, e));
              }
            }
          }

          if (updateFavoritesOfExpression != null) {
            UpdateItemSpec updateFavoritesOfItemSpec;

            //all of the users that this user is a favorite of need to be updated
            for (String username : oldUser.getFavoriteOf().keySet()) {
              try {
                updateFavoritesOfItemSpec = new UpdateItemSpec()
                    .withPrimaryKey(this.getPrimaryKeyIndex(), username)
                    .withUpdateExpression(updateFavoritesOfExpression)
                    .withValueMap(favoritesOfValueMap)
                    .withNameMap(favoritesOfNameMap);

                this.updateItem(updateFavoritesOfItemSpec);
              } catch (Exception e) {
                metrics.log(new ErrorDescriptor<>(username, classMethod, e));
              }
            }
          }

          this.updateActiveUsersFavorites(newFavorites, oldUser.getFavorites().keySet(), activeUser,
              metrics);

          final Item updatedUser = this.getItemByPrimaryKey(activeUser);
          //anytime we return the active user we need to add this
          updatedUser.withBoolean(FIRST_LOGIN, false);

          resultStatus = new ResultStatus(true,
              JsonUtils.convertObjectToJson(updatedUser.asMap()));
        } else {
          metrics.log(new WarningDescriptor<>(jsonMap, classMethod, errorMessage.get()));
          resultStatus.resultMessage = errorMessage.get();
        }
      } catch (final InvalidAttributeValueException iae) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, iae));
        resultStatus.resultMessage = iae.getMessage();
      } catch (final Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus.resultMessage = "Error: Unable to parse request.";
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found."));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private Optional<String> userSettingsIsValid(final String displayName) {
    String errorMessage = null;

    if (displayName.length() <= 0) {
      errorMessage = "Error: Display name cannot be empty.";
    } else if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
      errorMessage =
          "Error: Display name cannot be longer than " + MAX_DISPLAY_NAME_LENGTH + "characters.";
    }

    return Optional.ofNullable(errorMessage);
  }

  private boolean updateActiveUsersFavorites(final Set<String> newFavorites,
      final Set<String> oldFavorites, final String activeUser, final Metrics metrics) {
    final String classMethod = "UsersManager.updateActiveUsersFavorites";
    metrics.commonSetup(classMethod);

    boolean hadError = false;

    //If there are missing favorites, go through and remove the favoritesOf for each of these users
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
          metrics.log(new ErrorDescriptor<>(username, classMethod, e));
        }
      }
    }

    //If there are new favorites, go through and update the favoritesOf for each of these users
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
          final User newFavoriteUser = new User(this.getItemByPrimaryKey(username).asMap());

          updateFavoritesItemSpec = new UpdateItemSpec()
              .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser)
              .withUpdateExpression(updateFavoriteExpression)
              .withNameMap(new NameMap().with("#newFavoriteUser", username))
              .withValueMap(new ValueMap().withMap(":newFavorite", new HashMap<String, Object>() {{
                put(DISPLAY_NAME, newFavoriteUser.getDisplayName());
                put(ICON, newFavoriteUser.getIcon());
              }}));

          this.updateItem(updateFavoritesItemSpec);
        } catch (Exception e) {
          hadError = true;
          metrics.log(new ErrorDescriptor<>(username, classMethod, e));
        }
      }
    }

    metrics.commonClose(!hadError);
    return (!hadError);
  }

  /**
   * This method is used to update one of the sort settings associated with a specific user (either
   * CategorySort or GroupSort).
   *
   * @param jsonMap The map containing the json request sent from the front end. This must contain a
   *                value for one of the sort settings (CategorySort or GroupSort).
   * @param metrics Standard metrics object for profiling and logging
   */
  public ResultStatus updateSortSetting(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "UsersManager.updateSortSetting";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();
    if (jsonMap.containsKey(RequestFields.ACTIVE_USER) && (
        jsonMap.containsKey(APP_SETTINGS_CATEGORY_SORT) ||
            jsonMap.containsKey(APP_SETTINGS_GROUP_SORT))) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        User user = new User(this.getItemByPrimaryKey(activeUser).asMap());
        AppSettings appSettings;
        Map<String, Object> appSettingsMap = user.getAppSettings().asMap();
        String updateExpression = "";
        ValueMap valueMap = new ValueMap();

        if (jsonMap.containsKey(APP_SETTINGS_GROUP_SORT)) {
          Integer groupSort = (Integer) jsonMap.get(APP_SETTINGS_GROUP_SORT);
          appSettingsMap.put(APP_SETTINGS_GROUP_SORT, groupSort);
          // if the sort value is invalid, an exception will be thrown by AppSettings.SetGroupSort
          appSettings = new AppSettings(appSettingsMap);
          updateExpression +=
              "set " + APP_SETTINGS + "." + APP_SETTINGS_GROUP_SORT + " = :groupSort";
          valueMap.withInt(":groupSort", groupSort);
        } else if (jsonMap.containsKey(APP_SETTINGS_CATEGORY_SORT)) {
          Integer categorySort = (Integer) jsonMap.get(APP_SETTINGS_CATEGORY_SORT);
          appSettingsMap.put(APP_SETTINGS_CATEGORY_SORT, categorySort);
          // if the sort value is invalid, an exception will be thrown by AppSettings.SetCategorySort
          appSettings = new AppSettings(appSettingsMap);
          updateExpression +=
              "set " + APP_SETTINGS + "." + APP_SETTINGS_CATEGORY_SORT + " = :categorySort";
          valueMap.withInt(":categorySort", categorySort);
        }

        UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser)
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);

        this.updateItem(updateItemSpec);
      } catch (Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus.resultMessage = "Exception inside of manager.";
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found."));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }
    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private String getUpdateString(String current, String key, String valueName) {
    if (current != null) {
      return current + ", " + key + " = " + valueName;
    } else {
      return "set " + key + " = " + valueName;
    }
  }

  public ResultStatus createPlatformEndpointAndStoreArn(final Map<String, Object> jsonMap,
      final Metrics metrics) {
    final String classMethod = "UsersManager.createPlatformEndpointAndStoreArn";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, RequestFields.DEVICE_TOKEN);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String deviceToken = (String) jsonMap.get(RequestFields.DEVICE_TOKEN);
        final CreatePlatformEndpointRequest createPlatformEndpointRequest =
            new CreatePlatformEndpointRequest()
                .withPlatformApplicationArn(Config.PUSH_SNS_PLATFORM_ARN)
                .withToken(deviceToken)
                .withCustomUserData(activeUser);
        final CreatePlatformEndpointResult createPlatformEndpointResult = DatabaseManagers.SNS_ACCESS_MANAGER
            .registerPlatformEndpoint(createPlatformEndpointRequest, metrics);

        final String userEndpointArn = createPlatformEndpointResult.getEndpointArn();

        final String updateExpression = "set " + PUSH_ENDPOINT_ARN + " = :userEndpointArn";
        final ValueMap valueMap = new ValueMap().withString(":userEndpointArn", userEndpointArn);
        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser)
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);

        this.updateItem(updateItemSpec);
        resultStatus = new ResultStatus(true, "user post arn set successfully");
      } catch (Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus.resultMessage = "Exception inside of manager.";
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found."));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus unregisterPushEndpoint(final Map<String, Object> jsonMap,
      final Metrics metrics) {
    final String classMethod = "UsersManager.unregisterPushEndpoint";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays.asList(RequestFields.ACTIVE_USER);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final User user = new User(this.getItemByPrimaryKey(activeUser).asMap());

        if (user.pushEndpointArnIsSet()) {
          final String updateExpression = "remove " + PUSH_ENDPOINT_ARN;
          final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser)
              .withUpdateExpression(updateExpression);

          this.updateItem(updateItemSpec);

          //we've made it here without exception, now we try to actually delete the arn
          //If the following fails we're still safe as there's no reference to the arn in the db anymore
          final DeleteEndpointRequest deleteEndpointRequest = new DeleteEndpointRequest()
              .withEndpointArn(user.getPushEndpointArn());
          DatabaseManagers.SNS_ACCESS_MANAGER.unregisterPlatformEndpoint(deleteEndpointRequest);

          resultStatus = new ResultStatus(true, "endpoint unregistered");
        }
      } catch (Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus.resultMessage = "Exception inside of manager.";
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found."));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus removeOwnedCategory(final String username, final String categoryId,
      final Metrics metrics) {
    final String classMethod = "UsersManager.removeOwnedCategory";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      final String updateExpression = "remove " + OWNED_CATEGORIES + ".#categoryId";
      final NameMap nameMap = new NameMap().with("#categoryId", categoryId);

      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withPrimaryKey(this.getPrimaryKeyIndex(), username)
          .withUpdateExpression(updateExpression)
          .withNameMap(nameMap);

      this.updateItem(updateItemSpec);
      resultStatus = new ResultStatus(true, "Owned category removed successfully");
    } catch (Exception e) {
      metrics.log(
          new ErrorDescriptor<>(String.format("Username: %s, categoryId: %s", username, categoryId),
              classMethod, e));
      resultStatus.resultMessage = "Exception in manager";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This method removes a given group from each user that is or was in the group.
   *
   * @param membersLeft A set containing all of the members who have left the group.
   * @param groupId     The GroupId for the group to be removed from the users table.
   * @param metrics     Standard metrics object for profiling and logging
   */
  public ResultStatus removeGroupsLeftFromUsers(final Set<String> membersLeft, final String groupId,
      final Metrics metrics) {
    final String classMethod = "UsersManager.removeGroupsLeftFromUsers";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      final String updateExpressionGroupsLeft = "remove " + GROUPS_LEFT + ".#groupId";
      final NameMap nameMap = new NameMap().with("#groupId", groupId);

      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpressionGroupsLeft)
          .withNameMap(nameMap);

      for (String member : membersLeft) {
        updateItemSpec.withPrimaryKey(this.getPrimaryKeyIndex(), member);
        this.updateItem(updateItemSpec);
      }
      resultStatus = new ResultStatus(true, "Group successfully removed from users table.");
    } catch (Exception e) {
      metrics.log(
          new ErrorDescriptor<>(groupId, classMethod, e));
      resultStatus.resultMessage = "Exception inside of: " + classMethod;
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus markEventAsSeen(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "UsersManager.markEventAsSeen";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, GroupsManager.GROUP_ID, RequestFields.EVENT_ID);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(GroupsManager.GROUP_ID);
        final String eventId = (String) jsonMap.get(RequestFields.EVENT_ID);

        final String updateExpression =
            "remove " + GROUPS + ".#groupId." + EVENTS_UNSEEN + ".#eventId";
        final NameMap nameMap = new NameMap().with("#groupId", groupId).with("#eventId", eventId);

        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser)
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap);

        this.updateItem(updateItemSpec);

        resultStatus = new ResultStatus(true, "Event marked as seen successfully.");
      } catch (final Exception e) {
        resultStatus.resultMessage = "Exception inside of: " + classMethod;
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found."));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus setUserGroupMute(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "UsersManager.setUserGroupMute";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, GroupsManager.GROUP_ID, APP_SETTINGS_MUTED);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(GroupsManager.GROUP_ID);
        final Boolean muteValue = (Boolean) jsonMap.get(APP_SETTINGS_MUTED);

        final String updateExpression =
            "set " + GROUPS + ".#groupId." + APP_SETTINGS_MUTED + " = :mute";
        final NameMap nameMap = new NameMap().with("#groupId", groupId);
        final ValueMap valueMap = new ValueMap().withBoolean(":mute", muteValue);

        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser)
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap)
            .withValueMap(valueMap);

        this.updateItem(updateItemSpec);

        resultStatus = new ResultStatus(true, "User group mute set successfully.");
      } catch (final Exception e) {
        resultStatus.resultMessage = "Exception inside of: " + classMethod;
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found."));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public ResultStatus markAllEventsSeen(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "UsersManager.markAllEventsSeen";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, GroupsManager.GROUP_ID);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(GroupsManager.GROUP_ID);

        //assume the user has this group mapping, otherwise this call shouldn't have been made

        final String updateExpression =
            "set " + GROUPS + ".#groupId." + EVENTS_UNSEEN + " = :empty";
        final ValueMap valueMap = new ValueMap().withMap(":empty", Collections.emptyMap());
        final NameMap nameMap = new NameMap().with("#groupId", groupId);

        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser)
            .withUpdateExpression(updateExpression)
            .withNameMap(nameMap)
            .withValueMap(valueMap);

        this.updateItem(updateItemSpec);

        resultStatus = new ResultStatus(true, "All events marked seen successfully.");
      } catch (final Exception e) {
        resultStatus.resultMessage = "Exception inside of: " + classMethod;
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found."));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}