import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/user_tokens_manager.dart';
import 'package:frontEnd/utilities/config.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

final String idTokenKey = "id";

Future<String> makeApiRequest(
    String apiEndpoint, Map<String, dynamic> requestContent,
    {firstAttempt: true, isDioRequest: false}) async {
  SharedPreferences tokens = await Globals.getTokens();

  if (tokens.containsKey(idTokenKey)) {
    Map<String, String> headers = {
      "Authorization": "Bearer " + tokens.getString(idTokenKey)
    };

    http.Response httpResponse;
    Response dioResponse;

    if (isDioRequest) {
      Dio dio = new Dio();
      FormData formData = FormData.fromMap(requestContent);
      dioResponse = await dio.post(
          Config.apiRootUrl + Config.apiDeployment + apiEndpoint,
          data: formData,
          options: Options(headers: headers));
    } else {
      httpResponse = await http.post(
          Config.apiRootUrl + Config.apiDeployment + apiEndpoint,
          headers: headers,
          body: json.encode(requestContent));
    }

    if ((!isDioRequest && httpResponse.statusCode == 200) ||
        (isDioRequest && dioResponse.statusCode == 200)) {
      if (isDioRequest) {
        return dioResponse.data.toString();
      } else {
        return httpResponse.body;
      }
    } else if (firstAttempt) {
      // in case the id_token has expired
      if (await refreshUserTokens()) {
        return makeApiRequest(apiEndpoint, requestContent,
            firstAttempt: false, isDioRequest: isDioRequest);
      }
    }
  } else {
    //clear navigation stack and head to the login page?
  }
  return ""; // none of our apis should return this so this indicates error
}

Map<String, dynamic> getEmptyApiRequest() {
  return {"action": "", "payload": {}};
}
