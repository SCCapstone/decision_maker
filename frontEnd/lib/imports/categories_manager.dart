import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/utilities/request_fields.dart';
import 'package:frontEnd/utilities/utilities.dart';

import 'api_manager.dart';
import 'globals.dart';
import 'groups_manager.dart';

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

    String response = await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response != "") {
      try {
        Map<String, dynamic> body = jsonDecode(response);
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

  static Future<List<Category>> getAllCategoriesList() async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "getCategories";

    String response = await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response != "") {
      try {
        Map<String, dynamic> fullResponseJson = jsonDecode(response);
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

  static Future<List<Category>> getAllCategoriesFromGroup(String groupId) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "getCategories";
    jsonRequestBody["payload"]
        .putIfAbsent(GroupsManager.GROUP_ID, () => groupId);

    String response = await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response != "") {
      try {
        Map<String, dynamic> fullResponseJson = jsonDecode(response);
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
    jsonRequestBody["payload"].putIfAbsent(CATEGORY_ID, () => categoryId);

    String response = await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response != "") {
      try {
        Map<String, dynamic> body = jsonDecode(response);
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
