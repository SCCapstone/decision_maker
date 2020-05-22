import 'dart:convert';

import 'package:front_end_pocket_poll/imports/response_item.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/models/category_rating_tuple.dart';
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
  static final String OWNER = "Owner";

  static final String editAction = "editCategory";
  static final String createAction = "newCategory";
  static final String getAction = "getCategories";
  static final String deleteAction = "deleteCategory";

  static Future<ResultStatus<Category>> addOrEditCategory(
      String categoryName,
      Map<String, int> choiceLabels,
      Map<String, String> choiceRatings,
      Category category) async {
    ResultStatus<Category> retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();

    if (category != null) {
      jsonRequestBody[RequestFields.ACTION] = editAction;
      jsonRequestBody[RequestFields.PAYLOAD]
          .putIfAbsent(CATEGORY_ID, () => category.categoryId);
    } else {
      // if no category is passed in, then we are creating a category
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
          retVal.errorMessage = "Unable to save category.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to save category.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to save category. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to save category.";
    }
    return retVal;
  }

  static Future<ResultStatus<List<CategoryRatingTuple>>> getCategoriesList(
      {String categoryId}) async {
    ResultStatus<List<CategoryRatingTuple>> retVal = new ResultStatus(success: false);

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
              responseJson.map((m) => new CategoryRatingTuple.fromJson(m)).toList();
          retVal.success = true;
        } else {
          retVal.errorMessage = "Unable to load categories.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to load categories.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to load categories. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to load categories.";
    }
    return retVal;
  }

  static Future<ResultStatus<List<CategoryRatingTuple>>> getAllCategoriesFromGroup(
      String groupId) async {
    ResultStatus<List<CategoryRatingTuple>> retVal = new ResultStatus(success: false);

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
              responseJson.map((m) => new CategoryRatingTuple.fromJson(m)).toList();
        } else {
          // not sure if we want to return an error here, but it needs to return an empty list to avoid problems in popups
          retVal.errorMessage = "Unable to load group categories.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to load group categories.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to load group categories. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to load group categories.";
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
          retVal.errorMessage = "Unable to delete category.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to delete category.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to delete category. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to delete category.";
    }
    return retVal;
  }

  // sorts a list of categories alphabetically (ascending)
  static void sortByAlphaAscending(List<Category> categories) {
    categories.sort((a, b) =>
        a.categoryName.toUpperCase().compareTo(b.categoryName.toUpperCase()));
  }

  // sorts a list of categories alphabetically (descending)
  static void sortByAlphaDescending(List<Category> categories) {
    categories.sort((a, b) =>
        b.categoryName.toUpperCase().compareTo(a.categoryName.toUpperCase()));
  }
}
