import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:http/http.dart' as http;
import 'package:frontEnd/models/group.dart';
import 'globals.dart';

class GroupsManager {
  static final String apiEndpoint = "https://9zh1udqup3.execute-api.us-east-2.amazonaws.com/beta/groupsendpoint";

  static Future<List<Group>> getAllGroupsList() async {
    List<Group> allCategories = new List<Group>();
    for(int i=0;i<15;i++){
      Group group = new Group.debug("123", "Honor's Squad","hey","hey",
          new Map<String, dynamic>(), new Map<String, dynamic>(), 0, 0);
      allCategories.add(group);
    }
    return allCategories;
  }

  static Future<List<Group>> getGroups(
      String username, bool getAll) async {
    String jsonBody = "{\"action\":\"getCategories\", ";
    jsonBody += "\"payload\": {\"Username\" : \"$username\",";
    jsonBody += "\"GetAll\" : \"$getAll\"}}";
    http.Response response = await http.post(apiEndpoint, body: jsonBody);

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
}