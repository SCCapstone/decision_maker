import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/models/favorite.dart';

bool validCharacters(String input) {
  return RegExp(r"^[a-zA-Z0-9 !@#$%^&*()_+-=;:'/?.>,<\[\]{}|~`]*$")
      .hasMatch(input);
}

bool validCharactersPassword(String input) {
  return RegExp(
          "^[a-zA-Z0-9 !@#%&,:;<>_~`/'\\\-\\\^\\\$\\\*\\\(\\\)\\\.\\\?\\\{\\\}\\\"\\\[\\\|\\\]\\\\]*\$")
      .hasMatch(input);
}

String validGroupName(String input) {
  input = input.trim(); // sanity check trim
  String retVal;
  if (input.isEmpty) {
    retVal = "Group name cannot be empty.";
  } else if (input.length > Globals.maxGroupNameLength) {
    retVal = "Group name is too large.";
  } else if (!validCharacters(input)) {
    retVal = "Invalid characters.";
  }
  return retVal;
}

String validEventName(String input) {
  input = input.trim(); // sanity check trim
  String retVal;
  if (input.isEmpty) {
    retVal = "Event name cannot be empty.";
  } else if (input.length > Globals.maxEventNameLength) {
    retVal = "Event name is too large.";
  } else if (!validCharacters(input)) {
    retVal = "Invalid characters.";
  }
  return retVal;
}

String validVotingDuration(String input, bool verbose) {
  input = input.trim(); // sanity check trim
  String retVal;
  try {
    int num = int.parse(input);
    if (num < Globals.minVotingTime) {
      retVal = "Too small.";
    } else if (num > Globals.maxVotingTime) {
      retVal = "Too big.";
    }
  } catch (e) {
    if (verbose) {
      if (input.isEmpty) {
        retVal = "Duration cannot be empty.";
      } else {
        retVal = "Not a number.";
      }
    } else {
      if (input.isEmpty) {
        retVal = "Empty.";
      } else {
        retVal = "Error.";
      }
    }
  }
  return retVal;
}

String validConsiderDuration(String input, bool verbose) {
  input = input.trim(); // sanity check trim
  String retVal;
  try {
    int num = int.parse(input);
    if (num < Globals.minConsiderTime) {
      retVal = "Too small.";
    } else if (num > Globals.maxConsiderTime) {
      retVal = "Too big.";
    }
  } catch (e) {
    if (verbose) {
      if (input.isEmpty) {
        retVal = "Duration cannot be empty.";
      } else {
        retVal = "Not a number.";
      }
    } else {
      if (input.isEmpty) {
        retVal = "Empty.";
      } else {
        retVal = "Error.";
      }
    }
  }
  return retVal;
}

String validNewUser(String user, List<String> users) {
  user = user.trim(); // sanity check trim
  String retVal;
  if (user.isEmpty) {
    retVal = "Username cannot be empty.";
  } else if (user.length > Globals.maxUsernameLength) {
    retVal = "Username is too large.";
  } else if (user == Globals.username) {
    retVal = "Can't add yourself.";
  } else if (users.contains(user)) {
    retVal = "Username already added.";
  } else if (!validCharacters(user)) {
    retVal = "Invalid characters.";
  }
  return retVal;
}

String validChoiceName(String input) {
  input = input.trim(); // sanity check trim
  String retVal;
  if (input.isEmpty) {
    retVal = "Choice name cannot be empty.";
  } else if (input.length > Globals.maxChoiceNameLength) {
    retVal = "Choice name is too large.";
  } else if (!validCharacters(input)) {
    retVal = "Invalid characters.";
  }
  return retVal;
}

String validCategoryName(String input, {String categoryId}) {
  input = input.trim(); // sanity check trim
  String retVal;
  bool repeat = false;
  for (Category category in Globals.user.ownedCategories) {
    if ((categoryId == null || category.categoryId != categoryId) &&
        category.categoryName == input) {
      repeat = true;
    }
  }

  if (input.isEmpty) {
    retVal = "Category name cannot be empty.";
  } else if (input.length > Globals.maxCategoryNameLength) {
    retVal = "Category name is too large.";
  } else if (repeat) {
    retVal = "Category name already exists.";
  } else if (!validCharacters(input)) {
    retVal = "Invalid characters.";
  }
  return retVal;
}

String validEmail(String input) {
  /*
    Validates an email with a given inputField object. Cannot have an empty email or one without a @ symbol
   */
  input = input.trim(); // sanity check trim
  String retVal;
  if (input.isEmpty) {
    retVal = "Email cannot be empty.";
  } else if (!input.contains("@")) {
    retVal = "Enter a valid email address.";
  } else if (input.length > Globals.maxEmailLength) {
    return "Email is too large";
  } else if (!validCharacters(input)) {
    retVal = "Invalid characters.";
  }
  return retVal;
}

String validNewPassword(String input) {
  input = input.trim(); // sanity check trim
  String retVal = ""; // initialize it this way to avoid null pointer exception
  if (input.length < Globals.minPasswordLength) {
    retVal +=
        "Must contain at least ${Globals.minPasswordLength} characters.\n";
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
  if (!RegExp(
          "[!@#%&,:;<>_~`/'\\\-\\\^\\\$\\\*\\\(\\\)\\\.\\\?\\\{\\\}\\\"\\\[\\\|\\\]\\\\]+")
      .hasMatch(input)) {
    retVal += "Must contain at least one special character.\n";
  }
  if (!validCharactersPassword(input)) {
    retVal += "Password contains invalid characters.\n";
  }
  if (retVal == "") {
    retVal = null;
  } else {
    retVal.trim(); // get rid of potential extra new line
  }
  return retVal;
}

String validPassword(String input) {
  input = input.trim(); // sanity check trim
  String retVal;
  if (input.isEmpty) {
    retVal = "Password cannot be empty.";
  } else if (input.length > Globals.maxPasswordLength) {
    retVal = "Password is too large.";
  } else if (!validCharactersPassword(input)) {
    retVal = "Invalid characters.";
  }
  return retVal;
}

String validUsername(String input) {
  input = input.trim(); // sanity check trim
  String retVal;
  if (input.isEmpty) {
    retVal = "Username cannot be empty.";
  } else if (input.length > Globals.maxUsernameLength) {
    retVal = "Username is too large.";
  } else if (!validCharacters(input)) {
    retVal = "Invalid characters.";
  }
  return retVal;
}

String validDisplayName(String input) {
  input = input.trim(); // sanity check trim
  String retVal;
  if (input.isEmpty) {
    retVal = "Name cannot be empty.";
  } else if (input.length > Globals.maxDisplayNameLength) {
    retVal = "Name is too large.";
  } else if (!validCharacters(input)) {
    retVal = "Invalid characters.";
  }
  return retVal;
}

String validNewFavorite(String input, List<Favorite> favorites) {
  input = input.trim(); // sanity check trim
  String retVal;
  bool duplicates = false;
  for (Favorite favorite in favorites) {
    if (favorite.username == input) {
      duplicates = true;
    }
  }
  if (input.isEmpty) {
    retVal = "Username cannot be empty.";
  } else if (input.length > Globals.maxUsernameLength) {
    retVal = "Username is too large.";
  } else if (duplicates) {
    retVal = "You already have this username saved.";
  } else if (input == Globals.username) {
    retVal = "Cannot add yourself.";
  } else if (!validCharacters(input)) {
    retVal = "Invalid characters.";
  }
  return retVal;
}

String validMeridianHour(String input) {
  input = input.trim(); // sanity check trim
  String retVal;
  try {
    int hr = int.parse(input);
    if (hr <= 0 || hr > 12) {
      retVal = "Invalid";
    }
  } catch (e) {
    if (input.isEmpty) {
      retVal = "Empty.";
    } else {
      retVal = "Error.";
    }
  }
  return retVal;
}

String validMinute(String input) {
  input = input.trim(); // sanity check trim
  String retVal;
  try {
    int min = int.parse(input);
    if (min < 0 || min > 59) {
      retVal = "Invalid";
    }
  } catch (e) {
    if (input.isEmpty) {
      retVal = "Empty.";
    } else {
      retVal = "Error.";
    }
  }
  return retVal;
}
