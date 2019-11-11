import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart';
import 'dart:convert';
import 'globals.dart';
import 'dart:async';
import 'package:flutter/material.dart';

final String userPoolUrl = "https://pocket-poll.auth.us-east-2.amazoncognito.com";
final String clientId = "7eh4otm1r5p351d1u9j3h3rf1o";
final String redirectUri = "https://www.ledr.com/colours/white.htm"; // this needs to match what is entered in the development console

final String authorizeEndpoint = "/authorize?";
final String loginEndpoint = "/login?";
final String tokenEndpoint = "/oauth2/token?";
final String userInfoEndpoint = "/oauth2/userInfo?";
final String logoutEndpoint = "/logout?";

//One option to consider for the future is making one SharedPreferences variable
//as a global variable and just use it for all local storage, but for now I'll keep
//the scope within the context of tokens.
SharedPreferences _tokens;
final String accessTokenKey = "access";   //_tokens is like a Map, so we declare
final String refreshTokenKey = "refresh"; //the keys for the tokens here.
final String idTokenKey = "id";
bool gotTokens = false;


Future<bool> hasValidTokensSet() async {
  //check to see if the tokens are stored in local memory and to see if they can get
  //data from the user endpoint. if not, try to refresh. if can't refresh, return false

  try {
    //Initialize the SharedPreferences object and check for the existence of
    //the tokens.
    _tokens = await SharedPreferences.getInstance();
    if (_tokens.containsKey(accessTokenKey) &&
        _tokens.containsKey(refreshTokenKey) &&
        _tokens.containsKey(idTokenKey)) {

      //Set up the HTTP GET request.
      Map<String, String> headers = {
        "Authorization": "Bearer " + _tokens.getString(accessTokenKey)
      };
      Response response = await get(
          userPoolUrl + userInfoEndpoint, headers: headers);

      if (response.statusCode == 200) {
        var body = json.decode(response.body);
        Globals.username = body['username']; //Store the username for other functions.
        return true;
      } else {
        //The access token didn't work, so refresh and try again. If it fails a
        //second time, return false so that the user is taken back to the login.
        //TODO add logging (https://github.com/SCCapstone/decision_maker/issues/79)
        debugPrint("THE ACCESS TOKEN DID NOT WORK, ATTEMPTING REFRESH");
        headers.clear();

        await refreshUserTokens();
        headers = {
          "Authorization": "Bearer " + _tokens.getString(accessTokenKey)
        };

        await get(userPoolUrl + userInfoEndpoint, headers: headers);
        if (response.statusCode == 200) {
          debugPrint("THE REFRESHED ACCESS TOKEN WORKED");
          var body = json.decode(response.body);
          Globals.username = body['username'];
          return true;
        } else {
          return false;
        }
      }
    }
  }
  catch (e) {
    //TODO add logging (https://github.com/SCCapstone/decision_maker/issues/79)
    debugPrint("THE KEYS DO NOT EXIST");
    debugPrint(e.toString());
    return false;
  }
  return false;
}

Future<void> getUserTokens(String authorizationCode) async {
  //This will take the 'AUTHORIZATION_CODE' from the authorize endpoint and use
  //it to get tokens from the token endpoint, then store them via storeUserTokens.

  Map<String, String> headers = {
    "Content-Type": "application/x-www-form-urlencoded"
  };

  String data = 'grant_type=authorization_code&client_id=' +
      clientId + '&code=' + authorizationCode + '&redirect_uri=' +
      redirectUri;

  Response response = await post(
      userPoolUrl + tokenEndpoint, headers: headers,
      body: data);

  if (response.statusCode == 200) {
    var body = json.decode(response.body);

    storeUserTokens(
        body['access_token'], body['refresh_token'], body['id_token']);
    gotTokens = true;
  } else {
    debugPrint("FAILED GET TOKEN RESPONSE WITH CODE: " + response.statusCode.toString());
    debugPrint(response.toString());
  }
}

Future<void> refreshUserTokens() async {
  //Use the stored refresh token to get new tokens and then call storeUserTokens to store the new tokens
  //hint, don't overwrite the refresh token, that one token can be used many times and you only get it once
  String refreshToken = _tokens.getString(refreshTokenKey);

  Map<String, String> headers = {
    "Content-Type": "application/x-www-form-urlencoded"
  };

  String data = 'grant_type=refresh_token&client_id=' +
      clientId + '&refresh_token=' + refreshToken;

  Response response = await post(
    userPoolUrl + tokenEndpoint, headers: headers,
    body: data);

  if (response.statusCode == 200) {
    var body = json.decode(response.body);
    storeUserTokens(body['access_token'], refreshToken, body['id_token']);
  } else {
    debugPrint("FAILED REFRESH RESPONSE WITH CODE: " + response.statusCode.toString());
    debugPrint(response.toString());
    debugPrint(response.body);
  }
}

void storeUserTokens(String accessToken, String refreshToken, String idToken) {
  _tokens.setString(accessTokenKey, accessToken);
  _tokens.setString(idTokenKey, idToken);
  _tokens.setString(refreshTokenKey, refreshToken);
}