//TODO add unit testing https://github.com/SCCapstone/decision_maker/issues/80
import 'dart:convert';
import 'dart:io';

import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/models/user.dart';
import 'package:frontEnd/utilities/request_fields.dart';

import 'api_manager.dart';
import 'globals.dart';

class UsersManager {
  static final String apiEndpoint = "usersendpoint";

  //breaking style guide for consistency with backend vars
  static final String USERNAME = "Username";
  static final String DISPLAY_NAME = "DisplayName";
  static final String APP_SETTINGS = "AppSettings";
  static final String APP_SETTINGS_DARK_THEME = "DarkTheme";
  static final String APP_SETTINGS_MUTED = "Muted";
  static final String APP_SETTINGS_GROUP_SORT = "GroupSort";
  static final String GROUPS = "Groups";
  static final String CATEGORIES = "Categories";
  static final String OWNED_CATEGORIES = "OwnedCategories";
  static final String FAVORITES = "Favorites";
  static final String ICON = "Icon";

  static final String getUserDataAction = "getUserData";
  static final String updateSettingsAction = "updateUserSettings";
  static final String updateRatingsAction = "updateUserChoiceRatings";
  static final String getRatingsAction = "getUserRatings";

  static Future<ResultStatus<User>> getUserData({String username}) async {
    ResultStatus<User> retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = getUserDataAction;
    if (username != null) {
      jsonRequestBody[RequestFields.PAYLOAD]
          .putIfAbsent(USERNAME, () => username);
    }

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);
    User ret;

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);

        ResponseItem responseItem = new ResponseItem.fromJson(body);

        ret = User.fromJson(json.decode(responseItem.resultMessage));

        if (responseItem.success) {
          retVal.success = true;
          retVal.data = ret;
        } else {
          retVal.errorMessage = "Error getting the user (1).";
        }
      } catch (e) {
        retVal.errorMessage = "Error getting the user (2).";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to get user data. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to get user data.";
    }
    return retVal;
  }

  static Future<ResultStatus> updateUserSettings(
      String displayName,
      int darkTheme,
      int muted,
      int groupSort,
      List<String> favorites,
      File image) async {
    ResultStatus retVal = new ResultStatus(success: false);

    Map<String, dynamic> settings = new Map<String, dynamic>();
    settings.putIfAbsent(APP_SETTINGS_DARK_THEME, () => darkTheme);
    settings.putIfAbsent(APP_SETTINGS_MUTED, () => muted);
    settings.putIfAbsent(APP_SETTINGS_GROUP_SORT, () => groupSort);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = updateSettingsAction;
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(DISPLAY_NAME, () => displayName);
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(FAVORITES, () => favorites);
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(APP_SETTINGS, () => settings);
    if (image != null) {
      jsonRequestBody[RequestFields.PAYLOAD]
          .putIfAbsent(ICON, () => image.readAsBytesSync());
    }

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      Map<String, dynamic> body = jsonDecode(response.data);

      try {
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal.success = true;
          Globals.user = User.fromJson(json.decode(responseItem.resultMessage));
        } else {
          retVal.errorMessage = "Error updating user settings (1).";
        }
      } catch (e) {
        retVal.errorMessage = "Error updating user settings (2).";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to update user settings. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to update user settings.";
    }
    return retVal;
  }

  static Future<ResultStatus> updateUserChoiceRatings(
      String categoryId, Map<String, String> choiceRatings) async {
    ResultStatus retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = updateRatingsAction;
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(CategoriesManager.CATEGORY_ID, () => categoryId);
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(RequestFields.USER_RATINGS, () => choiceRatings);

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      Map<String, dynamic> body = jsonDecode(response.data);

      try {
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal.success = true;
        } else {
          retVal.errorMessage = "Error updating user ratings (1).";
        }
      } catch (e) {
        retVal.errorMessage = "Error updating user ratings (2).";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to update user rating. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to update user rating.";
    }
    return retVal;
  }

  static Future<ResultStatus<Map<String, dynamic>>> getUserRatings(
      String categoryId) async {
    ResultStatus<Map<String, dynamic>> retVal =
        new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = getRatingsAction;
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(CategoriesManager.CATEGORY_ID, () => categoryId);

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      Map<String, dynamic> body = jsonDecode(response.data);

      try {
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal.success = true;
          retVal.data = json.decode(responseItem.resultMessage);
        } else {
          retVal.errorMessage = "Error getting user ratings (1).";
        }
      } catch (e) {
        retVal.errorMessage = "Error getting user ratings (2).";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to get user ratings. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to get user ratings.";
    }
    return retVal;
  }

  static Future registerPushEndpoint(Future<String> token) async {
    String tokenAfter = await token;

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = "registerPushEndpoint";
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(RequestFields.DEVICE_TOKEN, () => tokenAfter);

    //blind send here, not critical for app or user if it fails
    makeApiRequest(apiEndpoint, jsonRequestBody);
  }

  static Future unregisterPushEndpoint() async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = "unregisterPushEndpoint";

    //blind send here, not critical for app or user if it fails
    makeApiRequest(apiEndpoint, jsonRequestBody);
  }
}
