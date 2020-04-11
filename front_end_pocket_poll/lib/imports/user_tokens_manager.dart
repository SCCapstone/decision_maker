import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/user.dart';
import 'package:http/http.dart';
import 'globals.dart';

final String userPoolUrl =
    "https://pocket-poll.auth.us-east-2.amazoncognito.com";
final String clientId = "7eh4otm1r5p351d1u9j3h3rf1o";
final String redirectUri =
    "https://www.ledr.com/colours/white.htm"; // this needs to match what is entered in the development console

final String authorizeEndpoint = "/authorize?";
final String loginEndpoint = "/login?";
final String tokenEndpoint = "/oauth2/token?";
final String userInfoEndpoint = "/oauth2/userInfo?";
final String logoutEndpoint = "/logout?";

// keys for use in the global shared prefs
final String accessTokenKey = "access";
final String refreshTokenKey = "refresh";
final String idTokenKey = "id";

Future<bool> hasValidTokensSet(BuildContext context) async {
  ResultStatus<User> resultStatus = await UsersManager.getUserData();

  bool retVal = true;
  if (resultStatus.success) {
    Globals.user = resultStatus.data;
    Globals.username = resultStatus.data.username;
  } else {
    retVal = false;
  }

  return retVal;
}

Future<bool> refreshUserTokens() async {
  bool success = false;

  //Use the stored refresh token to get new tokens and then call storeUserTokens to store the new tokens
  //hint, don't overwrite the refresh token, that one token can be used many times and you only get it once
  String refreshToken =
      (await Globals.getSharedPrefs()).getString(refreshTokenKey);

  Map<String, String> headers = {
    "Content-Type": "application/x-www-form-urlencoded"
  };

  String data = 'grant_type=refresh_token&client_id=' +
      clientId +
      '&refresh_token=' +
      refreshToken;

  Response response =
      await post(userPoolUrl + tokenEndpoint, headers: headers, body: data);

  if (response.statusCode == 200) {
    Map<String, dynamic> body = json.decode(response.body);
    await storeUserTokens(body['access_token'], refreshToken, body['id_token']);
    success = true;
  } else {
    clearSharedPrefs(); // if there was anything there, it is junk so clear it
  }

  return success;
}

Future<void> storeUserTokens(
    String accessToken, String refreshToken, String idToken) async {
  (await Globals.getSharedPrefs()).setString(accessTokenKey, accessToken);
  (await Globals.getSharedPrefs()).setString(idTokenKey, idToken);
  (await Globals.getSharedPrefs()).setString(refreshTokenKey, refreshToken);
}

void clearSharedPrefs() async {
  (await Globals.getSharedPrefs()).clear();
}
