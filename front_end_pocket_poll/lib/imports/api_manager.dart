import 'dart:convert';
import 'dart:io';

import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/user_tokens_manager.dart';
import 'package:front_end_pocket_poll/utilities/config.dart';
import 'package:front_end_pocket_poll/utilities/request_fields.dart';
import 'package:http/http.dart' as http;
import 'package:package_info/package_info.dart';
import 'package:shared_preferences/shared_preferences.dart';

final String idTokenKey = "id";
final String appVersion = "AppVersion";

Future<ResultStatus<String>> makeApiRequest(Map<String, dynamic> requestContent,
    {firstAttempt: true}) async {
  ResultStatus<String> retVal =
      new ResultStatus(success: false, networkError: false);

  SharedPreferences tokens = await Globals.getSharedPrefs();
  if (tokens.containsKey(idTokenKey)) {
    Map<String, String> headers = {
      "Authorization": "Bearer " + tokens.getString(idTokenKey)
    };

    try {
      //add the app version to the request payload
      PackageInfo packageInfo = await Globals.getPackageInfo();
      requestContent[RequestFields.PAYLOAD]
          .putIfAbsent(appVersion, () => packageInfo.buildNumber);

      //make the api request
      http.Response response = await http.post(
          Config.apiRootUrl +
              Config.apiDeployment +
              requestContent[RequestFields.ACTION],
          headers: headers,
          body: json.encode(requestContent[RequestFields.PAYLOAD]));
      if (response.statusCode == 200) {
        retVal.success = true;
        retVal.data = response.body;
      } else if (firstAttempt) {
        // in case the id_token has expired
        if (await refreshUserTokens()) {
          retVal = await makeApiRequest(requestContent,
              firstAttempt: false);
        }
      }
    } on SocketException catch (_) {
      retVal.networkError = true;
    }
  }
  return retVal;
}

Map<String, dynamic> getEmptyApiRequest() {
  return {RequestFields.ACTION: "", RequestFields.PAYLOAD: {}};
}
