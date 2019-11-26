import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/models/event.dart';
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
      Group group = new Group.debug(
          "123",
          "The Council",
          dummyPic,
          "testing",
          usersMap,
          new Map<String, dynamic>(),
          new Map<String, dynamic>(),
          0,
          10,
          0);
      allGroups.add(group);
    }
    return allGroups;
  }

  static Future<List<Group>> getGroups(String username, bool getAll) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "getGroups";
    jsonRequestBody["payload"]
        .putIfAbsent("ActiveUser", () => Globals.username);
    print(jsonRequestBody);
    http.Response response =
        await http.post(apiEndpoint, body: jsonRequestBody);

    if (response.statusCode == 200) {
      print(response.body);
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

  static List<Event> getGroupEvents(Group group) {
    List<Event> events = new List<Event>();
    List<String> optedIn = new List<String>();
    for (int i = 0; i < 5; i++) {
      optedIn.add(i.toString());
    }
    Event event = new Event.debug("1234", "Opt In Example", "2019-11-27 09:00:00",
        "2019-11-27 09:20:00", 1, 10, 10, optedIn);
    Event event1 = new Event.debug("12345", "Voting Example", "2019-11-27 09:10:00",
        "2019-11-27 09:25:00", 1, 10, 10, optedIn);
    Event event2 = new Event.debug("12346", "Finished Example", "2019-11-27 09:00:00",
        "2019-11-27 09:25:00", 1, 10, 10, optedIn);
    events.add(event);
    events.add(event1);
    events.add(event2);
//    for (int i = 0; i < 10; i++) {
//      Event event = new Event.debug("1234", "Event $i", "2019-11-27 0$i:16:53",
//          "2019-11-27 0$i:16:53", 1, 10, 10, optedIn);
//      events.add(event);
//    }
    events.sort((a, b) => DateTime.parse(b.eventStartDateTime).compareTo(DateTime.parse(a.eventStartDateTime)));
    return events;
  }
}
