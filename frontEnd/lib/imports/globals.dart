import 'package:flutter/material.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/models/user.dart';
import 'package:intl/intl.dart';

class Globals {
  static Color secondaryColor = Color(0xff106126);
  static String username;
  static User user;
  static bool android;
  static DateFormat formatter = DateFormat('MM-dd-yyyy –').add_jm();
  static Group currentGroup;

  static void clearGlobals() {
    username = null;
    android = null;
  }
}
