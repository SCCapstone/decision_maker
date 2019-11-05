import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

import 'pair.dart';
import 'utilities.dart';

class DevTestingManager {
  static final String tableName = "testing_tables";
  static final String apiEndpoint = "https://9zh1udqup3.execute-api.us-east-2.amazonaws.com/beta";

  static Future<List<Pair>> getAllPairs() async {
    http.Response response = await http.get(apiEndpoint);

    if (response.statusCode == 200) {
      List responseJson = json.decode(response.body);
      return responseJson.map((m) => new Pair.fromJson(m)).toList();
    } else {
      //TODO add logging (https://github.com/SCCapstone/decision_maker/issues/79)
      throw Exception("Failed to load value pairs from the database.");
    }
  }

  static void processAddPair(String key, String value, BuildContext context) async {
    http.Response response = await http.post(
        apiEndpoint,
        body: "{\"" + key + "\": \"" + value + "\"}");

    if (response.statusCode == 200 && response.body == "Data inserted successfully!") {
      showPopupMessage("Data inserted successfully!", context);
    } else {
      showPopupMessage("Unable to insert pair, ensure the key does not start with a number and does not contain spaces.", context);
    }
  }
}