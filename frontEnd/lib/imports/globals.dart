import 'package:flutter/material.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/models/user.dart';
import 'package:intl/intl.dart';
import 'package:shared_preferences/shared_preferences.dart';

class Globals {
  static Color secondaryColor = Color(0xff106126);
  static String username;
  static User user;
  static bool android;
  static DateFormat formatter = DateFormat('MM-dd-yyyy –').add_jm();
  static Group currentGroup;
  static SharedPreferences tokens;
  static int dateSort = 0;
  static int alphabeticalSort = 1;

  static void clearGlobals() {
    username = null;
    android = null;
  }

  static Future<SharedPreferences> getTokens() async {
    if (tokens == null) {
      tokens = await SharedPreferences.getInstance();
    }

    return tokens;
  }

  static int boolToInt(bool val) {
    if (val) {
      return 1;
    } else {
      return 0;
    }
  }

  static bool intToBool(int val) {
    return val == 1;
  }
}
