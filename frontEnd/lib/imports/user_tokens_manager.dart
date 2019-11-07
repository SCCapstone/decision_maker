import 'package:flutter/cupertino.dart';

import 'response_item.dart';
import 'package:http/http.dart' as http;

final String userPoolUrl =
    "https://pocket-poll.auth.us-east-2.amazoncognito.com/";
final String clientId = "7eh4otm1r5p351d1u9j3h3rf1o";
final String redirectUri =
    "https://google.com"; // this needs to match what is entered in the development console
final String _csrf = "5fa76012-5581-422f-aa19-953e4f073749";

final String authorizeEndpoint = "/authorize?";
final String tokenEndpoint = "/token?";
final String userInfoEndpoint = "/userInfo?";
final String logoutEndpoint = "/logout?";
final String loginEndpoint = "/login";
final String signupEndpoint = "/signup";

Future<ResponseItem> logUserIn(
    BuildContext context, String username, String password) async {
  bool success = false;
  String actionMessage = "Log in failed";

  //attempt to log user in with cognito
  String endpoint = userPoolUrl +
      authorizeEndpoint +
      "?client_id=" +
      clientId +
      "&response_type=code&redirect_uri=" +
      redirectUri;
  print(endpoint);
  http.Response response = await http.get(endpoint,
      headers: {
        "Content-type": "application/x-www-form-urlencoded",
        "username": "$username",
        "password": "$password"
      });
  print("Status is: " + response.statusCode.toString());
  if (response.statusCode == 200) {
    success = true;
    actionMessage = response.body;
  }
  //if login is successful, call getUserTokens with parsed code from Location header

  return new ResponseItem(success, actionMessage);
}

Future<ResponseItem> registerNewUser(BuildContext context, String email,
    String username, String password) async {
  bool success = false;
  String actionMessage = "function not implemented";
  //attempt to create user account with cognito

  //if login is successful, call getUserTokens with parsed code from Location header

  return new ResponseItem(success, actionMessage);
}

void getUserTokens() {
  //This will take the 'AUTHORIZATION_CODE' from the authorize endpoint and use
  //it to get tokens from the token endpoint. Then call storeUserTokens to store these tokens
}

void refreshUserTokens() {
  //Use the stored refresh token to get new tokens and then call storeUserTokens to store the new tokens
  //hint, don't overwrite the refresh token, that one token can be used many times and you only get it once
}

void storeUserTokens() {}
