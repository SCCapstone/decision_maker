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

String validEventName(String input) {
  if (input.length == 0) {
    return "Event name cannot be empty!";
  } else if (input.length > 200) {
    return "Event name is too large!";
  } else {
    return null;
  }
}

String validPassPercentage(String input) {
  String retVal;
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
  String retVal;
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

String validUser(String user, Map<String, dynamic> users) {
  if (user.isEmpty) {
    return "Username cannot be empty!";
  } else if (users.keys.contains(user)) {
    return "Username already added!";
  } else if (user == Globals.username) {
    return "Can't add yourself!";
  } else {
    return null;
  }
}

String validChoice(String input) {
  if (input.length == 0) {
    return "Choice name cannot be empty!";
  } else {
    return null;
  }
}

String validCategory(String input) {
  if (input.length == 0) {
    return "Category name cannot be empty!";
  } else {
    return null;
  }
}
