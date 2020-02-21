import 'dart:convert';
import 'dart:io';

import 'package:flutter/cupertino.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/user_tokens_manager.dart';
import 'package:frontEnd/utilities/config.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

final String idTokenKey = "id";

Future<String> makeApiRequest(
    String apiEndpoint, Map<String, dynamic> requestContent,
    {firstAttempt: true, BuildContext context}) async {
  SharedPreferences tokens = await Globals.getTokens();

  if (tokens.containsKey(idTokenKey)) {
    Map<String, String> headers = {
      "Authorization": "Bearer " + tokens.getString(idTokenKey)
    };

    try {
      http.Response response = await http.post(
          Config.apiRootUrl + Config.apiDeployment + apiEndpoint,
          headers: headers,
          body: json.encode(requestContent));
      if (response.statusCode == 200) {
        return response.body;
      } else if (firstAttempt) {
        // in case the id_token has expired
        if (await refreshUserTokens()) {
          return makeApiRequest(apiEndpoint, requestContent,
              firstAttempt: false);
        }
      }
    } on SocketException catch (_) {
      showErrorMessage(
          "Network Error", "No internet connection found.", context);
    }
  } else {
    //clear navigation stack and head to the login page?
  }
  return ""; // none of our apis should return this so this indicates error
}

Map<String, dynamic> getEmptyApiRequest() {
  return {"action": "", "payload": {}};
}
