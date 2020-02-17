import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/favorite.dart';

String validGroupIcon(String input) {
  if (!input.contains("http")) {
    return "Not a valid url.";
  } else {
    return null;
  }
}

String validGroupName(String input) {
  if (input.length == 0) {
    return "Group name cannot be empty.";
  } else if (input.length > 200) {
    return "Group name is too large.";
  } else {
    return null;
  }
}

String validEventName(String input) {
  if (input.length == 0) {
    return "Event name cannot be empty.";
  } else if (input.length > 200) {
    return "Event name is too large.";
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
    retVal = "Not a number.";
  }
  return retVal;
}

String validPollDuration(String input) {
  String retVal;
  try {
    int num = int.parse(input);
    if (num <= 0) {
      retVal = "Too small.";
    } else if (num > 10000) {
      retVal = "Too big.";
    }
  } catch (e) {
    retVal = "Not a number.";
  }
  return retVal;
}

String validUser(String user, List<String> users) {
  if (user.isEmpty) {
    return "Username cannot be empty.";
  } else if (user == Globals.username) {
    return "Can't add yourself.";
  } else if (users.contains(user)) {
    return "Username already added.";
  } else {
    return null;
  }
}

String validChoice(String input) {
  if (input.length == 0) {
    return "Choice name cannot be empty.";
  } else {
    return null;
  }
}

String validCategory(String input) {
  if (input.length == 0) {
    return "Category name cannot be empty.";
  } else {
    return null;
  }
}

String validEmail(String input) {
  /*
    Validates an email with a given inputField object. Cannot have an empty email or one without a @ symbol
   */
  String retVal;
  if (input.isEmpty) {
    retVal = "Email cannot be empty.";
  } else if (!input.contains("@")) {
    retVal = "Enter a valid email address.";
  }
  return retVal;
}

String validNewPassword(String input) {
  String retVal = ""; // initialize it this way to avoid null pointer exception
  if (input.length < 8) {
    retVal += "Must contain at least 8 characters.\n";
  }
  if (!RegExp(r'[a-z]+').hasMatch(input)) {
    retVal += "Must contain at least one lowercase letter.\n";
  }
  if (!RegExp(r'[A-Z]+').hasMatch(input)) {
    retVal += "Must contain at least one uppercase letter.\n";
  }
  if (!RegExp(r'[0-9]+').hasMatch(input)) {
    retVal += "Must contain at least one number.\n";
  }
  if (!RegExp(r'[!@#$%^&*(),.?":{}\[\]\\|<>]+').hasMatch(input)) {
    retVal += "Must contain at least one special character.\n";
  }
  if (retVal == "") {
    retVal = null;
  } else {
    retVal.trim(); // get rid of potential extra new line
  }
  return retVal;
}

String validPassword(String input) {
  String retVal;
  if (input.isEmpty) {
    retVal = "Password cannot be empty.";
  }
  return retVal;
}

String validUsername(String input) {
  String retVal;
  if (input.isEmpty) {
    retVal = "Username cannot be empty.";
  }
  return retVal;
}

String validName(String input) {
  String retVal;
  if (input.isEmpty) {
    retVal = "Name cannot be empty.";
  }
  return retVal;
}

String validNewFavorite(String input, List<Favorite> favorites) {
  String retVal;
  bool duplicates = false;
  for (Favorite favorite in favorites) {
    if (favorite.username == input) {
      duplicates = true;
    }
  }
  if (input.isEmpty) {
    retVal = "Username cannot be empty.";
  } else if (duplicates) {
    retVal = "You already have this username saved.";
  } else if (input == Globals.username) {
    retVal = "Cannot add yourself.";
  }
  return retVal;
}
