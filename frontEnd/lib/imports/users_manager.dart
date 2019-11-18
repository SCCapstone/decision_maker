//TODO add unit testing https://github.com/SCCapstone/decision_maker/issues/80
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:http/http.dart' as http;

class UsersManager {
  static final String apiEndpoint = "https://9zh1udqup3.execute-api.us-east-2.amazonaws.com/beta/usersendpoint";

  static void insertNewUser(String userName, BuildContext context) async {
    String jsonBody = "{\"action\":\"newUser\",";
    jsonBody += "\"payload\": {\"Username\" : \"" + userName + "\"";
    jsonBody += "}}";
    http.Response response = await http.post(
        apiEndpoint,
        body: jsonBody);

    print(response.body);

    if (response.statusCode == 200) {
      Map<String, dynamic> body = jsonDecode(response.body);
      print(body);
      try {
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          showPopupMessage(responseItem.resultMessage, context);
        } else {
          showPopupMessage("Error adding the new user (1).", context);
        }
      } catch (e) {
        print(e);
        showPopupMessage("Error adding the new user (2).", context);
      }
    } else {
      showPopupMessage(
          "Unable to add the new user.",
          context);
    }
  }
  static void updateUserChoiceRatings(int categoryId, List<String> choiceLabels, List<String>choiceRatings, String user, BuildContext context) async {
    if(choiceLabels.length != choiceRatings.length){
      showPopupMessage("Error, choice labels and choice ratings list nonequal", context);
      return;
    }

    String jsonBody = "{\"action\":\"updateUserChoiceRatings\",";
    jsonBody += "\"payload\": {\"CategoryId" + categoryId.toString() + "\", ";
    jsonBody += "\"Ratings\": {";
    for (int i = 0; i < choiceLabels.length; i++) {
      jsonBody += "\"" +choiceLabels.elementAt(i) + "\": \"" + choiceRatings.elementAt(i)  + "\", ";
    }

    jsonBody = jsonBody.substring(0,jsonBody.length-2);

    jsonBody += "}, ";
    jsonBody += "\"Username\" : \"" + user + "\"";
    jsonBody += "}}";

    http.Response response = await http.post(
        apiEndpoint,
        body: jsonBody);

    print(response.body);

    if (response.statusCode == 200) {
      Map<String, dynamic> body = jsonDecode(response.body);
      print(body);
      try {
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          showPopupMessage(responseItem.resultMessage, context);
        } else {
          showPopupMessage("Error updating user ratings(1).", context);
        }
      } catch (e) {
        print(e);
        showPopupMessage("Error updating user preferences (2).", context);
      }
    } else {
      showPopupMessage(
          "Unable to update user preferences.",
          context);
    }
  }
}