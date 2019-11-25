import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/utilities/request_fields.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:http/http.dart' as http;
import 'package:frontEnd/models/group.dart';
import 'globals.dart';

class GroupsManager {
  static final String apiEndpoint =
      "https://9zh1udqup3.execute-api.us-east-2.amazonaws.com/beta/groupsendpoint";

  //breaking style guide for consistency with backend vars
  static final String GROUP_ID = "GroupId";
  static final String GROUP_NAME = "GroupName";
  static final String ICON = "Icon";
  static final String GROUP_CREATOR = "GroupCreator";
  static final String MEMBERS = "Members";
  static final String CATEGORIES = "Categories";
  static final String DEFAULT_POLL_PASS_PERCENT = "DefaultPollPassPercent";
  static final String DEFAULT_POLL_DURATION = "DefaultPollDuration";

  static Future<List<Group>> getGroups({List<String> groupIds}) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "getGroups";

    if (groupIds == null) {
      jsonRequestBody["payload"]
          .putIfAbsent(RequestFields.ACTIVE_USER, () => Globals.username);
    } else {
      jsonRequestBody["payload"]
          .putIfAbsent(RequestFields.GROUP_IDS, () => groupIds);
    }

    http.Response response =
        await http.post(apiEndpoint, body: json.encode(jsonRequestBody));

    if (response.statusCode == 200) {
      try {
        Map<String, dynamic> body = jsonDecode(response.body);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          List<dynamic> responseJson = json.decode(responseItem.resultMessage);

          return responseJson.map((m) => new Group.fromJson(m)).toList();
        } else {
          //TODO add logging (https://github.com/SCCapstone/decision_maker/issues/79)
        }
      } catch (e) {
        //TODO add logging (https://github.com/SCCapstone/decision_maker/issues/79)
      }

      return List<Group>(); // return empty list
    } else {
      //TODO add logging (https://github.com/SCCapstone/decision_maker/issues/79)
      throw Exception("Failed to load groups from the database.");
    }
  }

  static Future<bool> deleteGroup(String groupId, BuildContext context) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "deleteGroup";
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.ACTIVE_USER, () => Globals.username);
    jsonRequestBody["payload"].putIfAbsent(GROUP_ID, () => groupId);

    http.Response response =
        await http.post(apiEndpoint, body: json.encode(jsonRequestBody));

    if (response.statusCode == 200) {
      try {
        Map<String, dynamic> body = jsonDecode(response.body);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          showPopupMessage(responseItem.resultMessage, context);
        } else {
          showPopupMessage("Error deleting the group (1).", context);
        }

        return responseItem.success;
      } catch (e) {
        showPopupMessage("Error deleting the group (2).", context);
      }
    } else {
      showPopupMessage("Unable to delete group.", context);
    }

    return false;
  }

  static void editGroup(Group group, BuildContext context) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "editGroup";
    jsonRequestBody["payload"] = group.asMap();
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.ACTIVE_USER, () => Globals.username);

    http.Response response =
        await http.post(apiEndpoint, body: json.encode(jsonRequestBody));

    if (response.statusCode == 200) {
      try {
        Map<String, dynamic> body = jsonDecode(response.body);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          showPopupMessage(responseItem.resultMessage, context);
        } else {
          showPopupMessage("Error saving group data (1).", context);
        }
      } catch (e) {
        showPopupMessage("Error saving group data (2).", context);
      }
    } else {
      showPopupMessage("Unable to save group.", context);
    }
  }
}
