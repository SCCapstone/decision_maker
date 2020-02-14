//TODO add unit testing https://github.com/SCCapstone/decision_maker/issues/80
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/models/user.dart';
import 'package:frontEnd/utilities/request_fields.dart';
import 'package:frontEnd/utilities/utilities.dart';

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
  static final String FAVORITES = "Favorites";
  static final String ICON = "Icon";

  static Future<User> getUserData({BuildContext context}) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "getUserData";

    String response = await makeApiRequest(apiEndpoint, jsonRequestBody);
    User ret;

    if (response != "") {
      try {
        Map<String, dynamic> body = jsonDecode(response);

        ResponseItem responseItem = new ResponseItem.fromJson(body);

        ret = User.fromJson(json.decode(responseItem.resultMessage));

        if (responseItem.success) {
          showPopupMessage(responseItem.resultMessage, context);
        } else {
          showPopupMessage("Error getting the user (1).", context);
        }
      } catch (e) {
        showPopupMessage("Error getting the user (2).", context);
      }
    } else {
      showPopupMessage("Unable to get the user.", context);
    }

    return ret;
  }

  static void updateUserSettings(
      String displayName,
      int darkTheme,
      int muted,
      int groupSort,
      List<String> favorites,
      BuildContext context) async {
    Map<String, dynamic> settings = new Map<String, dynamic>();
    settings.putIfAbsent(APP_SETTINGS_DARK_THEME, () => darkTheme);
    settings.putIfAbsent(APP_SETTINGS_MUTED, () => muted);
    settings.putIfAbsent(APP_SETTINGS_GROUP_SORT, () => groupSort);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "updateUserSettings";
    jsonRequestBody["payload"].putIfAbsent(DISPLAY_NAME, () => displayName);
    jsonRequestBody["payload"].putIfAbsent(FAVORITES, () => favorites);
    jsonRequestBody["payload"].putIfAbsent(APP_SETTINGS, () => settings);
    jsonRequestBody["payload"].putIfAbsent(ICON, () => "asdf");

    String response = await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response != "") {
      Map<String, dynamic> body = jsonDecode(response);

      try {
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          Globals.user = User.fromJson(json.decode(responseItem.resultMessage));
        } else {
          showPopupMessage("Error updating user settings (1).", context);
        }
      } catch (e) {
        showPopupMessage("Error updating user settings (2).", context);
      }
    } else {
      showPopupMessage("Unable to update user settings.", context);
    }
  }

  static void updateUserChoiceRatings(String categoryId,
      Map<String, String> choiceRatings, BuildContext context) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "updateUserChoiceRatings";
    jsonRequestBody["payload"]
        .putIfAbsent(CategoriesManager.CATEGORY_ID, () => categoryId);
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.USER_RATINGS, () => choiceRatings);

    String response = await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response != "") {
      Map<String, dynamic> body = jsonDecode(response);

      try {
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          showPopupMessage(responseItem.resultMessage, context);
        } else {
          showPopupMessage("Error updating user ratings (1).", context);
        }
      } catch (e) {
        showPopupMessage("Error updating user ratings (2).", context);
      }
    } else {
      showPopupMessage("Unable to update user ratings.", context);
    }
  }

  static Future<Map<String, dynamic>> getUserRatings(
      String categoryId, BuildContext context) async {
    if (categoryId == null) {
      return new Map<String, dynamic>();
    }

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "getUserRatings";
    jsonRequestBody["payload"]
        .putIfAbsent(CategoriesManager.CATEGORY_ID, () => categoryId);

    String response = await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response != "") {
      Map<String, dynamic> body = jsonDecode(response);

      try {
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          return json.decode(responseItem.resultMessage);
        } else {
          showPopupMessage("Error getting user ratings (1).", context);
        }
      } catch (e) {
        showPopupMessage("Error getting user ratings (2).", context);
      }
    } else {
      showPopupMessage("Unable to get user ratings.", context);
    }

    return null;
  }
}
