import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:http/http.dart' as http;

class CategoriesManager {
  static final String apiEndpoint = "https://9zh1udqup3.execute-api.us-east-2.amazonaws.com/beta/categoriesendpoint";

  static void addNewCategory(String categoryName, Map<String, String> choiceLabels, Map<String, String> choiceRates, String user, BuildContext context) async {
    //TODO make call to save choice rating in the users table
    //TODO validate input
    String jsonBody = "{\"action\":\"newCategory\",";
    jsonBody += "\"payload\": {\"CategoryName\" : \"" + categoryName + "\",";
    jsonBody += "\"Choices\" : {";

    for (String i in choiceLabels.keys) {
      jsonBody += "\"" + i + "\" : \"" + choiceLabels[i] + "\", ";
    }

    jsonBody = jsonBody.substring(0, jsonBody.length - 2);

    jsonBody += "}, ";
    jsonBody += "\"Owner\" : \"" + user + "\"";
    jsonBody += "}}";
    print(jsonBody);

    http.Response response = await http.post(
        apiEndpoint,
        //TODO create or find something that will create the json for me
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
          showPopupMessage("Error createing the new category (1).", context);
        }
      } catch (e) {
        print(e);
        showPopupMessage("Error createing the new category (2).", context);
      }
    } else {
      showPopupMessage(
        "Unable to create category.",
        context);
    }
  }
}