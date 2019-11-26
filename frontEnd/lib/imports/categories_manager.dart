import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/utilities/request_fields.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:http/http.dart' as http;

import 'globals.dart';

class CategoriesManager {
  static final String apiEndpoint =
      "https://9zh1udqup3.execute-api.us-east-2.amazonaws.com/beta/categoriesendpoint";

  //breaking style guide for consistency with backend vars
  static final String CATEGORY_ID = "CategoryId";
  static final String CATEGORY_NAME = "CategoryName";
  static final String CHOICES = "Choices";
  static final String GROUPS = "Groups";
  static final String NEXT_CHOICE_NO = "NextChoiceNo";
  static final String OWNER = "Owner";

  static void addOrEditCategory(
      String categoryName,
      Map<String, String> choiceLabels,
      Map<String, String> choiceRatings,
      Category category,
      BuildContext context) async {
    if (categoryName == "") {
      showPopupMessage("Please enter a name for this category.", context);
      return;
    }

    if (choiceLabels.length <= 0) {
      showPopupMessage("There must be at least one choice.", context);
      return;
    }

    if (choiceLabels.length != choiceRatings.length) {
      showPopupMessage("You must enter a rating for all choices.", context);
      return;
    }

    //Validation successful

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();

    if (category != null) {
      jsonRequestBody["action"] = "editCategory";
      jsonRequestBody["payload"]
          .putIfAbsent(CATEGORY_ID, () => category.categoryId);
    } else {
      jsonRequestBody["action"] = "newCategory";
    }

    jsonRequestBody["payload"].putIfAbsent(CATEGORY_NAME, () => categoryName);
    jsonRequestBody["payload"].putIfAbsent(CHOICES, () => choiceLabels);
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.USER_RATINGS, () => choiceRatings);
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.ACTIVE_USER, () => Globals.username);

    http.Response response =
        await http.post(apiEndpoint, body: json.encode(jsonRequestBody));

    if (response.statusCode == 200) {
      try {
        Map<String, dynamic> body = jsonDecode(response.body);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          if (category != null) {
            showPopupMessage(responseItem.resultMessage, context);
          } else {
            showPopupMessage(responseItem.resultMessage, context,
                callback: (_) => Navigator.pop(context));
          }
        } else {
          showPopupMessage("Error saving the category (1).", context);
        }
      } catch (e) {
        showPopupMessage("Error saving the category (2).", context);
      }
    } else {
      showPopupMessage("Unable to edit/create category.", context);
    }
  }

  static Future<List<Category>> getAllCategoriesList(String username) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "getCategories";
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.ACTIVE_USER, () => username);

    http.Response response =
        await http.post(apiEndpoint, body: json.encode(jsonRequestBody));

    if (response.statusCode == 200) {
      try {
        Map<String, dynamic> fullResponseJson = jsonDecode(response.body);
        List<dynamic> responseJson =
            json.decode(fullResponseJson['resultMessage']);

        return responseJson.map((m) => new Category.fromJson(m)).toList();
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
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.ACTIVE_USER, () => Globals.username);
    jsonRequestBody["payload"].putIfAbsent(CATEGORY_ID, () => categoryId);

    http.Response response =
        await http.post(apiEndpoint, body: json.encode(jsonRequestBody));

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
      showPopupMessage("Unable to delete category.", context);
    }
  }
}
