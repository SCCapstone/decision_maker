import 'dart:convert';
import 'dart:io';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/user_tokens_manager.dart';
import 'package:frontEnd/utilities/config.dart';
import 'package:frontEnd/widgets/internet_loss.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

final String idTokenKey = "id";

Future<String> makeApiRequest(String apiEndpoint,
    Map<String, dynamic> requestContent, BuildContext context,
    {firstAttempt: true}) async {
  String retVal = "";
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
        retVal = response.body;
      } else if (firstAttempt) {
        // in case the id_token has expired
        if (await refreshUserTokens()) {
          retVal = await makeApiRequest(apiEndpoint, requestContent, context,
              firstAttempt: false);
        }
      }
    } on SocketException catch (_) {
      Navigator.push(
        context,
        MaterialPageRoute(
            builder: (context) => InternetLoss(
                  initialCheck: false,
                )),
      );
    }
  } else {
    //clear navigation stack and head to the login page?
  }
  return retVal; // none of our apis should return this so this indicates error
}

Map<String, dynamic> getEmptyApiRequest() {
  return {"action": "", "payload": {}};
}
