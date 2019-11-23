import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:http/http.dart' as http;
import 'package:frontEnd/models/group.dart';
import 'globals.dart';

class GroupsManager {
  static final String apiEndpoint =
      "https://9zh1udqup3.execute-api.us-east-2.amazonaws.com/beta/groupsendpoint";
  static final String dummyPic =
      "https://fsmedia.imgix.net/d9/01/91/f4/d5cb/420b/9036/9718a7f6609d/obiwan1jesusjpg.jpeg?rect=158%2C0%2C866%2C433&auto=format%2Ccompress&dpr=2&w=650";

  static Future<List<Group>> getAllGroupsList() async {
    // TODO get actual groups, dummy data for now (https://github.com/SCCapstone/decision_maker/issues/113)
    List<Group> allGroups = new List<Group>();
    for (int i = 0; i < 15; i++) {
      Map<String, String> usersMap = new Map<String, String>();
      for (int i = 0; i < 4; i++) {
        usersMap.putIfAbsent(i.toString(), () => i.toString());
      }
      usersMap.putIfAbsent(Globals.username, () => Globals.username);
      Group group = new Group.debug("123", "The Council", dummyPic, "testing",
          usersMap, new Map<String, dynamic>(), 0, 10);
      allGroups.add(group);
    }
    return allGroups;
  }

  static Future<List<Group>> getGroups(String username, bool getAll) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "getGroups";
    jsonRequestBody["payload"]
        .putIfAbsent("ActiveUser", () => Globals.username);
    http.Response response = await http.post(apiEndpoint, body: jsonRequestBody);

    if (response.statusCode == 200) {
      Map<String, dynamic> fullResponseJson = jsonDecode(response.body);
      List<dynamic> responseJson =
          json.decode(fullResponseJson['resultMessage']);
      return responseJson.map((m) => new Group.fromJson(m)).toList();
    } else {
      //TODO add logging (https://github.com/SCCapstone/decision_maker/issues/79)
      throw Exception("Failed to load categories from the database.");
    }
  }

  static Future<bool> deleteGroup(String groupId, BuildContext context) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "deleteGroup";
    jsonRequestBody["payload"]
        .putIfAbsent("ActiveUser", () => Globals.username);
    jsonRequestBody["payload"].putIfAbsent("GroupId", () => groupId);

    http.Response response =
        await http.post(apiEndpoint, body: json.encode(jsonRequestBody));

    if (response.statusCode == 200) {
      try {
        Map<String, dynamic> body = jsonDecode(response.body);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          showPopupMessage(responseItem.resultMessage, context);
          return true;
        } else {
          showPopupMessage("Error deleting the group (1).", context);
          return false;
        }
      } catch (e) {
        showPopupMessage("Error deleting the group (2).", context);
        return false;
      }
    } else {
      showPopupMessage("Unable to deleting group.", context);
      return false;
    }
  }
}
