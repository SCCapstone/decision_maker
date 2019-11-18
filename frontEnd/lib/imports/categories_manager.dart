import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:http/http.dart' as http;

import 'globals.dart';

class CategoriesManager {
  static final String apiEndpoint = "https://9zh1udqup3.execute-api.us-east-2.amazonaws.com/beta/categoriesendpoint";

  static void addNewCategory(String categoryName, Map<String, String> choiceLabels, Map<String, String> choiceRatings, Category category, String user, BuildContext context) async {
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

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();

    if (category != null) {
      jsonRequestBody["action"] = "editCategory";
      jsonRequestBody["payload"].putIfAbsent("CategoryId", () => category.categoryId);
    } else {
      jsonRequestBody["action"] = "newCategory";
      jsonRequestBody["payload"].putIfAbsent("CategoryName", () => categoryName);
    }

    jsonRequestBody["payload"].putIfAbsent("Choices", () => choiceLabels);
    jsonRequestBody["payload"].putIfAbsent("UserRatings", () => choiceRatings);
    jsonRequestBody["payload"].putIfAbsent("ActiveUser", () => user);

    http.Response response = await http.post(
        apiEndpoint,
        body: json.encode(jsonRequestBody));

    if (response.statusCode == 200) {
      try {
        Map<String, dynamic> body = jsonDecode(response.body);
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
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "getCategories";
    jsonRequestBody["payload"].putIfAbsent("ActiveUser", () => username);

    http.Response response = await http.post(apiEndpoint, body: json.encode(jsonRequestBody));

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

  static void deleteCategory(String categoryId, BuildContext context) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "deleteCategory";
    jsonRequestBody["payload"].putIfAbsent("ActiveUser", () => Globals.username);
    jsonRequestBody["payload"].putIfAbsent("CategoryId", () => categoryId);

    http.Response response = await http.post(
        apiEndpoint,
        body: json.encode(jsonRequestBody));

    if (response.statusCode == 200) {
      try {
        Map<String, dynamic> body = jsonDecode(response.body);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          showPopupMessage(responseItem.resultMessage, context);
        } else {
          showPopupMessage("Error deleting the category (1).", context);
        }
      } catch (e) {
        showPopupMessage("Error deleting the category (2).", context);
      }
    } else {
      showPopupMessage("Unable to deleting category.",context);
    }
  }
}