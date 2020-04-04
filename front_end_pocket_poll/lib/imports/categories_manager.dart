import 'dart:convert';

import 'package:front_end_pocket_poll/imports/response_item.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/utilities/request_fields.dart';

import 'api_manager.dart';
import 'groups_manager.dart';

class CategoriesManager {
  static final String apiEndpoint = "categoriesendpoint";

  //breaking style guide for consistency with backend vars
  static final String CATEGORY_ID = "CategoryId";
  static final String CATEGORY_IDS = "CategoryIds";
  static final String CATEGORY_NAME = "CategoryName";
  static final String CHOICES = "Choices";
  static final String GROUPS = "Groups";
  static final String NEXT_CHOICE_NO = "NextChoiceNo";
  static final String OWNER = "Owner";
  static final String CATEGORY_VERSION = "Version";

  static final String editAction = "editCategory";
  static final String createAction = "newCategory";
  static final String getAction = "getCategories";
  static final String deleteAction = "deleteCategory";

  static Future<ResultStatus<Category>> addOrEditCategory(
      String categoryName,
      Map<String, String> choiceLabels,
      Map<String, String> choiceRatings,
      Category category) async {
    ResultStatus<Category> retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();

    if (category != null) {
      jsonRequestBody[RequestFields.ACTION] = editAction;
      jsonRequestBody[RequestFields.PAYLOAD]
          .putIfAbsent(CATEGORY_ID, () => category.categoryId);
    } else {
      jsonRequestBody[RequestFields.ACTION] = createAction;
    }

    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(CATEGORY_NAME, () => categoryName);
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(CHOICES, () => choiceLabels);
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(RequestFields.USER_RATINGS, () => choiceRatings);

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal.success = true;
          retVal.data =
              new Category.fromJson(json.decode(responseItem.resultMessage));
        } else {
          retVal.errorMessage = "Error saving the category (1).";
        }
      } catch (e) {
        retVal.errorMessage = "Error saving the category (2).";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Failed to update category. Check internet connection.";
    } else {
      retVal.errorMessage = "Failed to update category.";
    }
    return retVal;
  }

  static Future<ResultStatus<List<Category>>> getAllCategoriesList(
      {String categoryId}) async {
    ResultStatus<List<Category>> retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = getAction;
    if (categoryId != null) {
      List<String> categoryIdList = new List<String>.from([categoryId]);
      jsonRequestBody[RequestFields.PAYLOAD]
          .putIfAbsent(CATEGORY_IDS, () => categoryIdList);
    }

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          List<dynamic> responseJson = json.decode(responseItem.resultMessage);
          retVal.data =
              responseJson.map((m) => new Category.fromJson(m)).toList();
          retVal.success = true;
        } else {
          retVal.errorMessage = "Unable to load categories";
        }
      } catch (e) {
        retVal.errorMessage = "Error when reading request";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Failed to load categories from the database. Check internet connection.";
    } else {
      retVal.errorMessage = "Failed to load categories from the database.";
    }
    return retVal;
  }

  static Future<ResultStatus<List<Category>>> getAllCategoriesFromGroup(
      String groupId) async {
    ResultStatus<List<Category>> retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = getAction;
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(GroupsManager.GROUP_ID, () => groupId);

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          List<dynamic> responseJson = json.decode(responseItem.resultMessage);
          retVal.success = true;
          retVal.data =
              responseJson.map((m) => new Category.fromJson(m)).toList();
        } else {
          // not sure if we want to return an error here, but it needs to return an empty list to avoid problems in popups
          retVal.errorMessage = "Error, bad request";
        }
      } catch (e) {
        retVal.errorMessage = "Error reading request";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Failed to load categories from the database. Check internet connection.";
    } else {
      retVal.errorMessage = "Failed to load categories from the database.";
    }
    return retVal;
  }

  static Future<ResultStatus> deleteCategory(String categoryId) async {
    ResultStatus retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = deleteAction;
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(CATEGORY_ID, () => categoryId);

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal.success = true;
        } else {
          retVal.errorMessage = "Error deleting the category (1).";
        }
      } catch (e) {
        retVal.errorMessage = "Error deleting the category (2).";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Failed to delete category. Check internet connection.";
    } else {
      retVal.errorMessage = "Failed to delete category.";
    }
    return retVal;
  }

  static void sortByAlphaAscending(List<Category> categories) {
    categories.sort((a, b) =>
        a.categoryName.toUpperCase().compareTo(b.categoryName.toUpperCase()));
  }

  static void sortByAlphaDescending(List<Category> categories) {
    categories.sort((a, b) =>
        b.categoryName.toUpperCase().compareTo(a.categoryName.toUpperCase()));
  }
}