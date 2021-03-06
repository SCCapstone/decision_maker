//TODO add unit testing https://github.com/SCCapstone/decision_maker/issues/80
import 'dart:convert';
import 'dart:io';

import 'package:front_end_pocket_poll/imports/categories_manager.dart';
import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/imports/response_item.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/models/app_settings.dart';
import 'package:front_end_pocket_poll/models/favorite.dart';
import 'package:front_end_pocket_poll/models/user.dart';
import 'package:front_end_pocket_poll/utilities/request_fields.dart';

import 'api_manager.dart';
import 'globals.dart';

class UsersManager {
  //breaking style guide for consistency with backend vars
  static final String USERNAME = "Username";
  static final String DISPLAY_NAME = "DisplayName";
  static final String APP_SETTINGS = "AppSettings";
  static final String APP_SETTINGS_DARK_THEME = "DarkTheme";
  static final String APP_SETTINGS_MUTED = "Muted";
  static final String APP_SETTINGS_GROUP_SORT = "GroupSort";
  static final String APP_SETTINGS_CATEGORY_SORT = "CategorySort";
  static final String GROUPS = "Groups";
  static final String CATEGORY_RATINGS = "CategoryRatings";
  static final String OWNED_CATEGORIES = "OwnedCategories";
  static final String OWNED_GROUPS_COUNT = "OwnedGroupsCount";
  static final String FAVORITES = "Favorites";
  static final String ICON = "Icon";
  static final String GROUPS_LEFT = "GroupsLeft";
  static final String FIRST_LOGIN = "FirstLogin";
  static final String DEFAULT_VOTING_DURATION = "DefaultVotingDuration";
  static final String DEFAULT_CONSIDER_DURATION = "DefaultRsvpDuration";
  static final String REPORT_MESSAGE = "ReportMessage";

  static final String getUserDataAction = "getUserData";
  static final String updateSettingsAction = "updateUserSettings";
  static final String updateRatingsAction = "updateUserChoiceRatings";
  static final String updateSortSettingAction = "updateSortSetting";
  static final String registerPushEndpointAction = "registerPushEndpoint";
  static final String unregisterPushEndpointAction = "unregisterPushEndpoint";
  static final String markEventAsSeenAction = "markEventAsSeen";
  static final String markAllEventsSeenAction = "markAllEventsSeen";
  static final String setUserGroupMuteAction = "setUserGroupMute";
  static final String addFavoriteAction = "addFavorite";
  static final String reportUserAction = "reportUser";
  static final String giveAppFeedbackAction = "giveAppFeedback";

  static Future<ResultStatus<User>> getUserData({String username}) async {
    ResultStatus<User> retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = getUserDataAction;
    if (username != null) {
      jsonRequestBody[RequestFields.PAYLOAD]
          .putIfAbsent(USERNAME, () => username);
    }

    ResultStatus<String> response = await makeApiRequest(jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);
        if (responseItem.success) {
          if (responseItem.resultMessage == "User not found.") {
            // have a specific error message for if the user is not found in the DB
            retVal.errorMessage = "User ($username) not found.";
          } else {
            retVal.data =
                User.fromJson(json.decode(responseItem.resultMessage));
            retVal.success = true;
          }
        } else {
          retVal.errorMessage = "Unable to load user data.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to load user data.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to load user data. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to load user data.";
    }
    return retVal;
  }

  static Future<ResultStatus> updateUserSettings(
      String displayName,
      bool darkTheme,
      bool muted,
      int considerDuration,
      int votingDuration,
      List<String> favorites,
      File image) async {
    ResultStatus retVal = new ResultStatus(success: false);
    AppSettings settings = new AppSettings(
        muted: muted,
        darkTheme: darkTheme,
        groupSort: Globals.user.appSettings.groupSort,
        defaultConsiderDuration: considerDuration,
        defaultVotingDuration: votingDuration,
        categorySort: Globals.user.appSettings.categorySort);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = updateSettingsAction;
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(DISPLAY_NAME, () => displayName);
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(FAVORITES, () => favorites);
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(APP_SETTINGS, () => settings.asMap());
    if (image != null) {
      jsonRequestBody[RequestFields.PAYLOAD]
          .putIfAbsent(ICON, () => image.readAsBytesSync());
    }

    ResultStatus<String> response = await makeApiRequest(jsonRequestBody);

    if (response.success) {
      Map<String, dynamic> body = jsonDecode(response.data);

      try {
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal.success = true;
          Globals.user = User.fromJson(json.decode(responseItem.resultMessage));
        } else {
          retVal.errorMessage = "Unable to update user data.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to update user data.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to update user data. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to update user data.";
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

    ResultStatus<String> response = await makeApiRequest(jsonRequestBody);

    if (response.success) {
      Map<String, dynamic> body = jsonDecode(response.data);

      try {
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal.success = true;
        } else {
          retVal.errorMessage = "Unable to update ratings.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to update ratings.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to update ratings. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to update ratings.";
    }
    return retVal;
  }

  static Future updateSortSetting(String sortSetting, int sortValue) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = updateSortSettingAction;
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(sortSetting, () => sortValue);

    //blind send here, not critical for app or user if it fails
    makeApiRequest(jsonRequestBody);
  }

  static Future registerPushEndpoint(Future<String> token) async {
    String tokenAfter = await token;

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = registerPushEndpointAction;
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(RequestFields.DEVICE_TOKEN, () => tokenAfter);

    //blind send here, not critical for app or user if it fails
    makeApiRequest(jsonRequestBody);
  }

  static Future unregisterPushEndpoint() async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = unregisterPushEndpointAction;

    //blind send here, not critical for app or user if it fails
    makeApiRequest(jsonRequestBody);
  }

  static Future markEventAsSeen(
      final String groupId, final String eventId) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = markEventAsSeenAction;
    jsonRequestBody[RequestFields.PAYLOAD][GroupsManager.GROUP_ID] = groupId;
    jsonRequestBody[RequestFields.PAYLOAD][RequestFields.EVENT_ID] = eventId;

    //blind send here, not critical for app or user if it fails
    makeApiRequest(jsonRequestBody);
  }

  static Future markAllEventsAsSeen(final String groupId) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = markAllEventsSeenAction;
    jsonRequestBody[RequestFields.PAYLOAD][GroupsManager.GROUP_ID] = groupId;

    //blind send here, not critical for app or user if it fails
    makeApiRequest(jsonRequestBody);
  }

  static Future setUserGroupMute(
      final String groupId, final bool muteValue) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = setUserGroupMuteAction;
    jsonRequestBody[RequestFields.PAYLOAD][GroupsManager.GROUP_ID] = groupId;
    jsonRequestBody[RequestFields.PAYLOAD][APP_SETTINGS_MUTED] = muteValue;

    //blind send here, not critical for app or user if it fails
    makeApiRequest(jsonRequestBody);
  }

  static Future reportUser(String username, String reportMessage) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = reportUserAction;
    jsonRequestBody[RequestFields.PAYLOAD][USERNAME] = username;
    jsonRequestBody[RequestFields.PAYLOAD][REPORT_MESSAGE] = reportMessage;

    //blind send here, not critical for app or user if it fails
    makeApiRequest(jsonRequestBody);
  }

  static Future giveAppFeedback(String username, String reportMessage) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = giveAppFeedbackAction;
    jsonRequestBody[RequestFields.PAYLOAD][USERNAME] = username;
    jsonRequestBody[RequestFields.PAYLOAD][REPORT_MESSAGE] = reportMessage;

    //blind send here, not critical for app or user if it fails
    makeApiRequest(jsonRequestBody);
  }

  static Future<ResultStatus> addFavorite(final Favorite favorite) async {
    final ResultStatus retVal = new ResultStatus(success: false);

    final Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = addFavoriteAction;
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(USERNAME, () => favorite.username);

    final ResultStatus<String> response = await makeApiRequest(jsonRequestBody);

    if (response.success) {
      try {
        final Map<String, dynamic> body = jsonDecode(response.data);
        final ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal.success = true;
        } else {
          retVal.errorMessage = "Unable to add favorite.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to add favorite.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to add favorite. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to add favorite.";
    }

    return retVal;
  }
}
