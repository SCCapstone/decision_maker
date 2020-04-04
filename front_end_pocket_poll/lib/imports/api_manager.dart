import 'dart:convert';
import 'dart:io';

import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/user_tokens_manager.dart';
import 'package:front_end_pocket_poll/utilities/config.dart';
import 'package:front_end_pocket_poll/utilities/request_fields.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

final String idTokenKey = "id";

Future<ResultStatus<String>> makeApiRequest(
    String apiEndpoint, Map<String, dynamic> requestContent,
    {firstAttempt: true}) async {
  ResultStatus<String> retVal =
      new ResultStatus(success: false, networkError: false);

  SharedPreferences tokens = await Globals.getSharedPrefs();
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
        retVal.success = true;
        retVal.data = response.body;
      } else if (firstAttempt) {
        // in case the id_token has expired
        if (await refreshUserTokens()) {
          retVal = await makeApiRequest(apiEndpoint, requestContent,
              firstAttempt: false);
        }
      }
    } on SocketException catch (_) {
      retVal.networkError = true;
    }
  } else {
    //clear navigation stack and head to the login page?
  }
  return retVal;
}

Map<String, dynamic> getEmptyApiRequest() {
  return {RequestFields.ACTION: "", RequestFields.PAYLOAD: {}};
}
