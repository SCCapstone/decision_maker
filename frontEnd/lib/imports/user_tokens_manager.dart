import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/user.dart';
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

//One option to consider for the future is making one SharedPreferences variable
//as a global variable and just use it for all local storage, but for now I'll keep
//the scope within the context of tokens.

final String accessTokenKey = "access"; //_tokens is like a Map, so we declare
final String refreshTokenKey = "refresh"; //the keys for the tokens here.
final String idTokenKey = "id";
bool gotTokens = false;

Future<bool> hasValidTokensSet() async {
  User user = await UsersManager.getUserData();

  if (user != null) {
    Globals.user = user;
    Globals.username = user.username; //Store the username
  } else {
    return false;
  }

  return true;
}

Future<void> refreshUserTokens() async {
  //Use the stored refresh token to get new tokens and then call storeUserTokens to store the new tokens
  //hint, don't overwrite the refresh token, that one token can be used many times and you only get it once
  String refreshToken = (await Globals.getTokens()).getString(refreshTokenKey);

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
    storeUserTokens(body['access_token'], refreshToken, body['id_token']);
    debugPrint("THE REFRESH OCCURED WITH STATUS CODE 200");
  } else {
    debugPrint(
        "FAILED REFRESH RESPONSE WITH CODE: " + response.statusCode.toString());
    debugPrint(response.toString());
    debugPrint(response.body);
    clearTokens(); // if there was anything there, it is junk so clear it
  }
}

void storeUserTokens(
    String accessToken, String refreshToken, String idToken) async {
  (await Globals.getTokens()).setString(accessTokenKey, accessToken);
  (await Globals.getTokens()).setString(idTokenKey, idToken);
  (await Globals.getTokens()).setString(refreshTokenKey, refreshToken);
}

void clearTokens() async {
  (await Globals.getTokens()).clear();
}
