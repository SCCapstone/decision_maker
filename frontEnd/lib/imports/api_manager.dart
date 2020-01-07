import 'dart:convert';

import 'package:frontEnd/imports/user_tokens_manager.dart';
import 'package:frontEnd/utilities/config.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

SharedPreferences tokens;
final String idTokenKey = "id";

Future<String> makeApiRequest(String apiEndpoint, Map<String, dynamic> requestContent,
    {firstAttempt: true}) async {
  if (tokens == null) {
    tokens = await SharedPreferences.getInstance();
  }

  if (tokens.containsKey(idTokenKey)) {
    Map<String, String> headers = {
      "Authorization": "Bearer " + tokens.getString(idTokenKey)
    };

    http.Response response =
    await http.post(apiEndpoint, headers: headers, body: json.encode(requestContent));

    if (response.statusCode == 200) {
      return response.body;
    } else if (firstAttempt) { // in case the id_token has expired
      refreshUserTokens();
      return makeApiRequest(Config.apiRootUrl + Config.apiDeployment + apiEndpoint, requestContent, firstAttempt: false);
    }
  } else {
    //clear navigation stack and head to the login page?
  }
  return ""; // none of our apis should return this so this indicates error
}

Map<String, dynamic> getEmptyApiRequest() {
  return {"action": "", "payload": {}};
}