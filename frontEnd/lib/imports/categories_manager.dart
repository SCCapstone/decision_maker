import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:milestone_3/models/category.dart';

import '../utilities/utilities.dart';

import 'globals.dart';

class CategoriesManager {
  static final String apiEndpoint =
      "https://9zh1udqup3.execute-api.us-east-2.amazonaws.com/beta/categoriesendpoint";

  static Future<List<Category>> getCategories(
      String username, bool getAll) async {
    Map<String, String> requestHeaders = {
      'username': '$username',
      'getAll': '$getAll'
    };
    http.Response response =
        await http.get(apiEndpoint, headers: requestHeaders);

    if (response.statusCode == 200) {
      Map<String, dynamic> fullResponseJson = jsonDecode(response.body);
      var responseJson = jsonDecode(fullResponseJson['body']);
      Category category = new Category.fromJson(responseJson);
      List categories = new List<Category>();
      categories.add(category);
      return categories;
    } else {
      //TODO add logging (https://github.com/SCCapstone/decision_maker/issues/79)
      throw Exception("Failed to load categories from the database.");
    }
  }

  static Future<List<Category>> getAllCategoriesList() async {
    List<Category> allCategories =
        await CategoriesManager.getCategories(Globals.username, true);
    return allCategories;
  }
}
