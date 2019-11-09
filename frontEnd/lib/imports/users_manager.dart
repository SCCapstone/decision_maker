//TODO add unit testing https://github.com/SCCapstone/decision_maker/issues/80

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

import '../utilities/utilities.dart';

class UsersManager {
  static final String apiEndpoint = "https://9zh1udqup3.execute-api.us-east-2.amazonaws.com/beta/usersendpoint";

  static void insertNewUser(String cogID, String username, BuildContext context) async {

    http.Response response = await http.post(apiEndpoint, body: "{\"" + cogID + "\": \"" + username + "\"}");

    if (response.statusCode == 200 && response.body == "Data inserted successfully!") {
      showPopupMessage("Data inserted successfully!", context);

    } else {
      showPopupMessage("Error adding user to Dynamo database.", context);
    }


}
}