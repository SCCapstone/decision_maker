import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/models/category_rating_tuple.dart';
import 'package:front_end_pocket_poll/models/group.dart';
import 'package:front_end_pocket_poll/models/user.dart';
import 'package:intl/intl.dart';
import 'package:shared_preferences/shared_preferences.dart';

class Globals {
  static Color pocketPollDarkGreen = Color(0xff106126);
  static Color pocketPollGreen = Color(0xff5ce080);
  static Color pocketPollGrey = Color(0xff303030);
  static String username;
  static User user;
  static DateFormat formatterWithTime = DateFormat.yMMMMd("en_US").add_jm();
  static DateFormat dateFormatter = DateFormat.yMMMMd("en_US");
  static Group currentGroup;
  static List<CategoryRatingTuple> cachedCategories = new List<CategoryRatingTuple>();
  static SharedPreferences sharedPrefs;
  static int dateNewestSort = 0;
  static int alphabeticalSort = 1;
  static int alphabeticalReverseSort = 2;
  static int dateOldestSort = 3;
  static String alphabeticalSortString = "Alphabetical (A-Z)";
  static String alphabeticalReverseSortString = "Alphabetical (Z-A)";
  static String dateNewestSortString = "Date Modified (Newest)";
  static String dateOldestSortString = "Date Modified (Oldest)";
  static bool fireBaseConfigured = false;
  static Function refreshGroupPage;
  static final String removedFromGroupAction = "removedFromGroup";
  static final String addedToGroupAction = "addedToGroup";
  static final String eventCreatedAction = "eventCreated";
  static final String eventChosenAction = "eventChosen";
  static final String eventVotingAction = "eventVoting";

  // validation variables
  static final int maxCategoryCacheSize = 10;
  static final int maxEventsPulled = 25;
  static final int maxCategories = 25;
  static final int minEmailLength = 3;
  static final int maxEmailLength = 60;
  static final int minUsernameLength = 4;
  static final int maxUsernameLength = 25;
  static final int minPasswordLength = 8;
  static final int maxPasswordLength = 30;
  static final int minDisplayNameLength = 1;
  static final int maxDisplayNameLength = 40;
  static final int minGroupNameLength = 1;
  static final int maxGroupNameLength = 40;
  static final int minEventNameLength = 1;
  static final int maxEventNameLength = 30;
  static final int minCategoryNameLength = 1;
  static final int maxCategoryNameLength = 25;
  static final int minChoiceNameLength = 1;
  static final int maxChoiceNameLength = 25;
  static final int minConsiderTime = 0;
  static final int maxConsiderTime = 9999;
  static final int maxConsiderDigits = maxConsiderTime.toString().length;
  static final int minVotingTime = 0;
  static final int maxVotingTime = 9999;
  static final int maxVotingDigits = maxVotingTime.toString().length;
  static final int maxChoiceRating = 5;
  static final int minChoiceRating = 0;
  static final int maxChoiceRatingDigits = maxChoiceRating.toString().length;

  static String resetPasswordUrl =
      "https://pocket-poll.auth.us-east-2.amazoncognito.com/forgotPassword?client_id=7eh4otm1r5p351d1u9j3h3rf1o&response_type=code&redirect_uri=https://pocket-poll-documents.s3.us-east-2.amazonaws.com/login_redirect.html";
  static String imageUrl =
      "https://pocketpoll-images.s3.us-east-2.amazonaws.com/";
  static final String termsUrl =
      "https://pocket-poll-documents.s3.us-east-2.amazonaws.com/terms_conditions.html";
  static final String privacyPolicyUrl =
      "https://pocket-poll-documents.s3.us-east-2.amazonaws.com/privacy_policy.html";
  static final String flutterUrl = "https://flutter.dev/";
  static final String awsUrl = "https://aws.amazon.com/";

  static ThemeData darkTheme = ThemeData(
      brightness: Brightness.dark,
      primarySwatch: Colors.green,
      primaryColor: pocketPollGreen,
      accentColor: pocketPollGreen,
      primaryColorDark: Colors.black,
      buttonTheme: ButtonThemeData(buttonColor: Color(0xff106126)),
      primaryTextTheme: TextTheme(title: TextStyle(color: Colors.black)),
      textTheme: TextTheme(body1: TextStyle(color: Colors.white)));
  static ThemeData lightTheme = ThemeData(
      brightness: Brightness.light,
      primarySwatch: Colors.green,
      primaryColor: pocketPollGreen,
      accentColor: pocketPollGreen,
      primaryColorDark: Colors.black,
      primaryTextTheme: TextTheme(title: TextStyle(color: Colors.black)),
      textTheme: TextTheme(body1: TextStyle(color: Colors.black)));

  static Future<SharedPreferences> getSharedPrefs() async {
    if (sharedPrefs == null) {
      sharedPrefs = await SharedPreferences.getInstance();
    }
    return sharedPrefs;
  }
}
