import 'package:frontEnd/imports/globals.dart';

String validGroupIcon(String input) {
  if (!input.contains("http")) {
    return "Not a valid url";
  } else {
    return null;
  }
}

String validGroupName(String input) {
  if (input.length == 0) {
    return "Group name cannot be empty!";
  } else if (input.length > 200) {
    return "Group name is too large!";
  } else {
    return null;
  }
}

String validPassPercentage(String input) {
  String retVal = null;
  try {
    int num = int.parse(input);
    if (num < 0 || num > 100) {
      retVal = "(0-100)";
    }
  } catch (e) {
    retVal = "Not a number";
  }
  return retVal;
}

String validPollDuration(String input) {
  String retVal = null;
  try {
    int num = int.parse(input);
    if (num <= 0) {
      retVal = "Too small";
    } else if (num > 10000) {
      retVal = "Too big";
    }
  } catch (e) {
    retVal = "Not a number";
  }
  return retVal;
}

String validUser(String user, List<String> users) {
  if (user.isEmpty) {
    return "Username cannot be empty!";
  } else if (users.contains(user)) {
    return "Username already added!";
  } else if (user == Globals.username) {
    return "Can't add youtself!";
  } else {
    return null;
  }
}
