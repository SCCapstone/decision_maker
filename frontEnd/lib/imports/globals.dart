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
  static String resetUrl =
      "https://pocket-poll.auth.us-east-2.amazoncognito.com/forgotPassword?client_id=7eh4otm1r5p351d1u9j3h3rf1o&response_type=code&redirect_uri=https://www.shiftadmin.com";
  static ThemeData darkTheme = ThemeData(
      brightness: Brightness.dark,
      primarySwatch: Colors.green,
      primaryColor: Color(0xff5ce080),
      accentColor: Color(0xff5ce080),
      primaryColorDark: Colors.black,
      buttonTheme: ButtonThemeData(buttonColor: Color(0xff106126)),
      primaryTextTheme: TextTheme(title: TextStyle(color: Colors.black)),
      textTheme: TextTheme(body1: TextStyle(color: Colors.white)));
  static ThemeData lightTheme = ThemeData(
      brightness: Brightness.light,
      primarySwatch: Colors.green,
      primaryColor: Color(0xff5ce080),
      accentColor: Color(0xff5ce080),
      primaryColorDark: Colors.black,
      primaryTextTheme: TextTheme(title: TextStyle(color: Colors.black)),
      textTheme: TextTheme(body1: TextStyle(color: Colors.black)));
  static String imageUrl =
      "https://pocketpoll-images.s3.us-east-2.amazonaws.com/";

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
}
