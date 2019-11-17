import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:http/http.dart' as http;

class CategoriesManager {
  static final String apiEndpoint = "https://9zh1udqup3.execute-api.us-east-2.amazonaws.com/beta/categoriesendpoint";

  static void addNewCategory(String categoryName, Map<String, String> choiceLabels, Map<String, String> choiceRatings, Category category, String user, BuildContext context) async {
    //TODO make call to save choice rating in the users table

    if (categoryName == "") {
      showPopupMessage("Please enter a name for this category.", context);
      return;
    }

    if (choiceLabels.length <= 0) {
      showPopupMessage("There must be at least one choice.", context);
      return;
    }

    if (choiceLabels.length != choiceRatings.length) {
      showPopupMessage("You must enter a rating for all choices", context);
      return;
    }

    if (user == "") {
      //not really sure how this would happen, either no active user or no owner
      showPopupMessage("Internal error.", context);
      return;
    }

    //Validation successful

    String jsonBody = "";

    if (category != null) {
      jsonBody += "{\"action\":\"editCategory\",";
      jsonBody += "\"payload\": {\"CategoryId\" : \"" + category.categoryId + "\",";
      jsonBody += "\"Choices\" : {";

      for (String i in choiceLabels.keys) {
        jsonBody += "\"$i\" : \"" + choiceLabels[i] + "\", ";
      }

      jsonBody = jsonBody.substring(0, jsonBody.length - 2);

      jsonBody += "}, ";
    } else {
      jsonBody += "{\"action\":\"newCategory\",";
      jsonBody += "\"payload\": {\"CategoryName\" : \"$categoryName\",";
      jsonBody += "\"Choices\" : {";

      for (String i in choiceLabels.keys) {
        jsonBody += "\"$i\" : \"" + choiceLabels[i] + "\", ";
      }

      //remove trailing space and comma
      jsonBody = jsonBody.substring(0, jsonBody.length - 2);

      jsonBody += "}, ";
    }

    jsonBody += "\"UserRatings\" : {";

    for (String i in choiceRatings.keys) {
      jsonBody += "\"$i\" : \"" + choiceRatings[i] + "\", ";
    }

    //remove trailing space and comma
    jsonBody = jsonBody.substring(0, jsonBody.length - 2);

    jsonBody += "}, ";

    jsonBody += "\"ActiveUser\" : \"$user\"";
    jsonBody += "}}";

    print(jsonBody);

    http.Response response = await http.post(
        apiEndpoint,
        //TODO create or find something that will create the json for me
        body: jsonBody);

    if (response.statusCode == 200) {
      Map<String, dynamic> body = jsonDecode(response.body);

      try {
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          showPopupMessage(responseItem.resultMessage, context);
        } else {
          showPopupMessage("Error saving the category (1).", context);
        }
      } catch (e) {
        showPopupMessage("Error saving the category (2).", context);
      }
    } else {
      showPopupMessage("Unable to edit/create category.",context);
    }
  }

  static Future<List<Category>> getAllCategoriesList(String username) async {
    String jsonBody = "{\"action\":\"getCategories\", ";
    jsonBody += "\"payload\": {\"ActiveUser\" : \"$username\"}}";

    http.Response response = await http.post(apiEndpoint, body: jsonBody);

    if (response.statusCode == 200) {
      try {
        Map<String, dynamic> fullResponseJson = jsonDecode(response.body);
        List<dynamic> responseJson = json.decode(
            fullResponseJson['resultMessage']);

        return responseJson.map((m) => new Category.fromJson(m))
            .toList();
      } catch (e) {
        //TODO add logging (https://github.com/SCCapstone/decision_maker/issues/79)
        return new List<Category>(); // returns empty list
      }
    } else {
      //TODO add logging (https://github.com/SCCapstone/decision_maker/issues/79)
      throw Exception("Failed to load categories from the database.");
    }
  }
}