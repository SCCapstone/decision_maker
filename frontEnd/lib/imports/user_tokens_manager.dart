import 'response_item.dart';

final String userPoolUrl = "https://pocket-poll.auth.us-east-2.amazoncognito.com";
final String clientId = "7eh4otm1r5p351d1u9j3h3rf1o";
final String redirectUri = "https://google.com"; // this needs to match what is entered in the development console

final String authorizeEndpoint = "/authorize?";
final String tokenEndpoint = "/token?";
final String userInfoEndpoint = "/userInfo?";
final String logoutEndpoint = "/logout?";

ResponseItem logUserIn() {
  bool success = false;
  String actionMessage = "function not implemented";

  //attempt to log user in with cognito

  //if login is successful, call getUserTokens with parsed code from Location header

  return new ResponseItem(success, actionMessage);
}

bool hasValidTokensSet() {
  //need a check to see if they're stored in local memory and to see if they can get
  //data from the user endpoint, if not, try to refresh, if can't refresh, return false
  return false;
}

ResponseItem registerNewUser() {
  bool success = false;
  String actionMessage = "function not implemented";

  //attempt to create user account with cognito

  //if adding user is successful, add call to 'insertNewUser' located in users_manager.dart

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

void storeUserTokens() {

}