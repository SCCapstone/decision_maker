import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/utilities/request_fields.dart';
import 'package:frontEnd/utilities/utilities.dart';

import 'api_manager.dart';
import 'groups_manager.dart';

class CategoriesManager {
  static final String apiEndpoint = "categoriesendpoint";

  //breaking style guide for consistency with backend vars
  static final String CATEGORY_ID = "CategoryId";
  static final String CATEGORY_NAME = "CategoryName";
  static final String CHOICES = "Choices";
  static final String GROUPS = "Groups";
  static final String NEXT_CHOICE_NO = "NextChoiceNo";
  static final String OWNER = "Owner";
  static final String editAction = "editCategory";
  static final String createAction = "newCategory";
  static final String getAction = "getCategories";

  static void addOrEditCategory(
      String categoryName,
      Map<String, String> choiceLabels,
      Map<String, String> choiceRatings,
      Category category,
      BuildContext context) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();

    if (category != null) {
      jsonRequestBody[RequestFields.ACTION] = editAction;
      jsonRequestBody["payload"]
          .putIfAbsent(CATEGORY_ID, () => category.categoryId);
    } else {
      jsonRequestBody[RequestFields.ACTION] = createAction;
    }

    jsonRequestBody["payload"].putIfAbsent(CATEGORY_NAME, () => categoryName);
    jsonRequestBody["payload"].putIfAbsent(CHOICES, () => choiceLabels);
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.USER_RATINGS, () => choiceRatings);

    String response =
        await makeApiRequest(apiEndpoint, jsonRequestBody, context);

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

  static Future<List<Category>> getAllCategoriesList(
      BuildContext context) async {
    ResultStatus retVal = new ResultStatus(success: false);
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = getAction;

    String response =
        await makeApiRequest(apiEndpoint, jsonRequestBody, context);

    if (response != "") {
      try {
        Map<String, dynamic> fullResponseJson = jsonDecode(response);
        List<dynamic> responseJson =
            json.decode(fullResponseJson[RequestFields.RESULT_MESSAGE]);
        retVal.data =
            responseJson.map((m) => new Category.fromJson(m)).toList();
        retVal.success = true;
        return responseJson.map((m) => new Category.fromJson(m)).toList();
      } catch (e) {
        retVal.data = new List<Category>();
        retVal.errorMessage = "Error when parsing categories";
        return new List<Category>(); // returns empty list
      }
    } else {
      throw Exception("Failed to load categories from the database.");
    }
  }

  static Future<ResultStatus> getAllCategoriesListNew(
      BuildContext context) async {
    ResultStatus retVal = new ResultStatus(success: false);
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = getAction;

    String response =
        await makeApiRequest(apiEndpoint, jsonRequestBody, context);

    if (response != "") {
      try {
        Map<String, dynamic> fullResponseJson = jsonDecode(response);
        List<dynamic> responseJson =
            json.decode(fullResponseJson[RequestFields.RESULT_MESSAGE]);
        retVal.data =
            responseJson.map((m) => new Category.fromJson(m)).toList();
        retVal.success = true;
      } catch (e) {
        retVal.data = new List<Category>();
        retVal.errorMessage = "Error when parsing categories";
      }
    } else {
      retVal.errorMessage = "Failed to load categories from the database.";
      throw Exception("Failed to load categories from the database.");
    }
    return retVal;
  }

  static Future<List<Category>> getAllCategoriesFromGroup(
      String groupId, BuildContext context) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = getAction;
    jsonRequestBody["payload"]
        .putIfAbsent(GroupsManager.GROUP_ID, () => groupId);

    String response =
        await makeApiRequest(apiEndpoint, jsonRequestBody, context);

    if (response != "") {
      try {
        Map<String, dynamic> fullResponseJson = jsonDecode(response);
        List<dynamic> responseJson =
            json.decode(fullResponseJson['resultMessage']);

        return responseJson.map((m) => new Category.fromJson(m)).toList();
      } catch (e) {
        return new List<Category>(); // returns empty list
      }
    } else {
      throw Exception("Failed to load categories from the database.");
    }
  }

  static void deleteCategory(String categoryId, BuildContext context) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = "deleteCategory";
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(CATEGORY_ID, () => categoryId);

    String response =
        await makeApiRequest(apiEndpoint, jsonRequestBody, context);

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
