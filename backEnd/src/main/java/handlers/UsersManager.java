package handlers;

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



  public UsersManager() {
    super("users", "Username", Regions.US_EAST_2);
  }

  public UsersManager(final DynamoDB dynamoDB) {
    super("users", "Username", Regions.US_EAST_2, dynamoDB);
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
   * This method is used to update one of the sort settings associated with a specific user (either
   * CategorySort or GroupSort).
   *
   * @param jsonMap The map containing the json request sent from the front end. This must contain a
   *                value for one of the sort settings (CategorySort or GroupSort).
   * @param metrics Standard metrics object for profiling and logging.
   */
  public ResultStatus updateSortSetting(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "UsersManager.updateSortSetting";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = null;

    final List<String> requiredKeys = Arrays.asList(RequestFields.ACTIVE_USER);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final User user = new User(this.getItemByPrimaryKey(activeUser).asMap());

        if (jsonMap.containsKey(APP_SETTINGS_GROUP_SORT)) {
          user.getAppSettings().setGroupSort((Integer) jsonMap.get(APP_SETTINGS_GROUP_SORT));
        } else if (jsonMap.containsKey(APP_SETTINGS_CATEGORY_SORT)) {
          user.getAppSettings().setCategorySort((Integer) jsonMap.get(APP_SETTINGS_CATEGORY_SORT));
        } else {
          resultStatus = ResultStatus.failure("Error: settings key not defined.");
          metrics.log(new ErrorDescriptor<>(jsonMap, classMethod,
              "Settings key in request payload not set."));
        }

        //this will only be set by an error at this point
        if (resultStatus == null) {
          final String updateExpression = "set " + APP_SETTINGS + " = :settingsMap";
          final ValueMap valueMap = new ValueMap()
              .withMap(":settingsMap", user.getAppSettings().asMap());

          final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
              .withUpdateExpression(updateExpression)
              .withValueMap(valueMap);

          this.updateItem(activeUser, updateItemSpec);
          resultStatus = ResultStatus.successful("Sort value updated successfully.");
        }
      } catch (Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus = ResultStatus.failure("Exception inside of manager.");
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found."));
      resultStatus = ResultStatus.failure("Error: Required request keys not found.");
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }


  /**
   * This function takes in a device token registered in google cloud messaging and creates a SNS
   * endpoint for this token and then registers the ARN of the SNS endpoint on the user item.
   *
   * @param jsonMap The map containing the json request payload sent from the front end. This must
   *                contain the active user and the firebase messaging token.
   * @param metrics Standard metrics object for profiling and logging.
   * @return Standard result status object giving insight on whether the request was successful.
   */
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

        //first thing to do is register the device token with SNS
        final CreatePlatformEndpointRequest createPlatformEndpointRequest =
            new CreatePlatformEndpointRequest()
                .withPlatformApplicationArn(Config.PUSH_SNS_PLATFORM_ARN)
                .withToken(deviceToken)
                .withCustomUserData(activeUser);
        final CreatePlatformEndpointResult createPlatformEndpointResult = DatabaseManagers.SNS_ACCESS_MANAGER
            .registerPlatformEndpoint(createPlatformEndpointRequest, metrics);

        //this creation will give us a new ARN for the sns endpoint associated with the device token
        final String userEndpointArn = createPlatformEndpointResult.getEndpointArn();

        //we need to register the ARN for the user's device on the user item
        final String updateExpression = "set " + PUSH_ENDPOINT_ARN + " = :userEndpointArn";
        final ValueMap valueMap = new ValueMap().withString(":userEndpointArn", userEndpointArn);
        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(this.getPrimaryKeyIndex(), activeUser)
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);

        this.updateItem(updateItemSpec);
        resultStatus = ResultStatus.successful("user push arn set successfully");
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

  /**
   * This function takes in a username and if that user has a push notification associated with
   * their account it gets removed from their user item.
   *
   * @param jsonMap The map containing the json request payload sent from the front end.
   * @param metrics Standard metrics object for profiling and logging.
   * @return Standard result status object giving insight on whether the request was successful.
   */
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

          //we've made it here without exception which means the user doesn't have record of the
          //endpoint anymore, now we try to actually delete the arn. If the following fails we're
          //still safe as there's no reference to the arn in the db anymore
          final DeleteEndpointRequest deleteEndpointRequest = new DeleteEndpointRequest()
              .withEndpointArn(user.getPushEndpointArn());
          DatabaseManagers.SNS_ACCESS_MANAGER.unregisterPlatformEndpoint(deleteEndpointRequest);

          resultStatus = ResultStatus.successful("endpoint unregistered");
        } else {
          resultStatus = ResultStatus.successful("no endpoint to unregister");
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