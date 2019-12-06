//TODO add unit testing https://github.com/SCCapstone/decision_maker/issues/80
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:frontEnd/models/app_settings.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/utilities/request_fields.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:http/http.dart' as http;

import 'globals.dart';

class UsersManager {
  static final String apiEndpoint =
      "https://9zh1udqup3.execute-api.us-east-2.amazonaws.com/beta/usersendpoint";

  //breaking style guide for consistency with backend vars
  static final String USERNAME = "Username";
  static final String FIRST_NAME = "FirstName";
  static final String LAST_NAME = "LastName";
  static final String APP_SETTINGS = "AppSettings";
  static final String APP_SETTINGS_DARK_THEME = "DarkTheme";
  static final String APP_SETTINGS_MUTED = "Muted";
  static final String APP_SETTINGS_GROUP_SORT = "GroupSort";
  static final String GROUPS = "Groups";
  static final String CATEGORIES = "Categories";

  static void insertNewUser(String username, BuildContext context) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "newUser";
    jsonRequestBody["payload"].putIfAbsent(USERNAME, () => username);

    http.Response response =
        await http.post(apiEndpoint, body: json.encode(jsonRequestBody));

    if (response.statusCode == 200) {
      Map<String, dynamic> body = jsonDecode(response.body);
      try {
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          showPopupMessage(responseItem.resultMessage, context);
        } else {
          showPopupMessage("Error adding the new user (1).", context);
        }
      } catch (e) {
        showPopupMessage("Error adding the new user (2).", context);
      }
    } else {
      showPopupMessage("Unable to add the new user.", context);
    }
  }

  static void updateUserAppSettings(String settingToUpdate, int updateVal,
      BuildContext context) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "updateUserAppSettings";
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.ACTIVE_USER, () => Globals.username);
    jsonRequestBody["payload"]
        .putIfAbsent(settingToUpdate, () => updateVal);
    http.Response response =
    await http.post(apiEndpoint, body: json.encode(jsonRequestBody));

    if (response.statusCode == 200) {
      Map<String, dynamic> body = jsonDecode(response.body);

      try {
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          showPopupMessage(responseItem.resultMessage, context);
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
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.ACTIVE_USER, () => Globals.username);

    http.Response response =
        await http.post(apiEndpoint, body: json.encode(jsonRequestBody));

    if (response.statusCode == 200) {
      Map<String, dynamic> body = jsonDecode(response.body);

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
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.ACTIVE_USER, () => Globals.username);

    http.Response response =
        await http.post(apiEndpoint, body: json.encode(jsonRequestBody));

    if (response.statusCode == 200) {
      Map<String, dynamic> body = jsonDecode(response.body);

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

  static Future<AppSettings> getUserAppSettings(
      BuildContext context) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "getUserAppSettings";
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.ACTIVE_USER, () => Globals.username);

    http.Response response =
    await http.post(apiEndpoint, body: json.encode(jsonRequestBody));

    if (response.statusCode == 200) {
      Map<String, dynamic> body = jsonDecode(response.body);

      try {
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          Map<String, dynamic> responseJson = json.decode(responseItem.resultMessage);
          return new AppSettings.fromJson(responseJson);
        } else {
          showPopupMessage("Error getting user app settings (1).", context);
        }
      } catch (e) {
        showPopupMessage("Error getting user app settings (2).", context);
      }
    } else {
      showPopupMessage("Unable to get user app settings.", context);
    }

    return null;
  }
}
