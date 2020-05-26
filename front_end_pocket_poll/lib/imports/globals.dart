import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/models/category_rating_tuple.dart';
import 'package:front_end_pocket_poll/models/get_group_response.dart';
import 'package:front_end_pocket_poll/models/user.dart';
import 'package:intl/intl.dart';
import 'package:shared_preferences/shared_preferences.dart';

class Globals {
  static String username;
  static User user;
  static GetGroupResponse currentGroupResponse = new GetGroupResponse();
  static List<CategoryRatingTuple> cachedCategories =
      new List<CategoryRatingTuple>();
  static bool fireBaseConfigured = false;
  static Function refreshGroupPage;
  static SharedPreferences sharedPrefs;

  static Future<SharedPreferences> getSharedPrefs() async {
    if (sharedPrefs == null) {
      sharedPrefs = await SharedPreferences.getInstance();
    }
    return sharedPrefs;
  }

  // *************** CONSTANTS ***************
  static final Color pocketPollDarkGreen = Color(0xff106126);
  static final Color pocketPollPrimary = Color(0xff70919f);
  static final Color pocketPollDarkBlue = Color(0xff0a2757);
  static final Color pocketPollGreen = Color(0xff5ce080);
  static final Color pocketPollGrey = Color(0xff303030);
  static final ThemeData darkTheme = ThemeData(
      brightness: Brightness.dark,
      primarySwatch: Colors.blue,
      primaryColor: pocketPollPrimary,
      accentColor: pocketPollPrimary,
      primaryColorDark: Colors.black,
      buttonTheme: ButtonThemeData(buttonColor: pocketPollDarkBlue),
      primaryTextTheme: TextTheme(title: TextStyle(color: Colors.white)),
      textTheme: TextTheme(body1: TextStyle(color: Colors.white)));
  static final ThemeData lightTheme = ThemeData(
    brightness: Brightness.light,
    primarySwatch: Colors.blue,
    primaryColor: pocketPollPrimary,
    accentColor: pocketPollPrimary,
    buttonTheme: ButtonThemeData(
        buttonColor: pocketPollDarkBlue, textTheme: ButtonTextTheme.primary),
    primaryColorDark: Colors.black,
    primaryTextTheme: TextTheme(title: TextStyle(color: Colors.white)),
//      textTheme: TextTheme(body1: TextStyle(color: Colors.black))
  );
  static final DateFormat formatterWithTime =
      DateFormat.yMMMMd("en_US").add_jm();
  static final DateFormat dateFormatter = DateFormat.yMMMMd("en_US");
  static final int defaultChoiceRating = 3;

  // sorting variables
  static final int dateNewestSort = 0;
  static final int alphabeticalSort = 1;
  static final int alphabeticalReverseSort = 2;
  static final int dateOldestSort = 3;
  static final int choiceRatingAscending = 4;
  static final int choiceRatingDescending = 5;
  static final String alphabeticalSortString = "Alphabetical (A-Z)";
  static final String alphabeticalReverseSortString = "Alphabetical (Z-A)";
  static final String dateNewestSortString = "Date Modified (Newest)";
  static final String dateOldestSortString = "Date Modified (Oldest)";
  static final String choiceRatingAscendingSortString = "Ratings (Ascending)";
  static final String choiceRatingDescendingSortString = "Ratings (Descending)";

  // notification actions
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
  static final int maxGroupMembers = 300;
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

  // URLS
  static final String resetPasswordUrl =
      "https://pocket-poll.auth.us-east-2.amazoncognito.com/forgotPassword?client_id=7eh4otm1r5p351d1u9j3h3rf1o&response_type=code&redirect_uri=https://pocket-poll-documents.s3.us-east-2.amazonaws.com/login_redirect.html";
  static final String imageUrl =
      "https://pocketpoll-images.s3.us-east-2.amazonaws.com/";
  static final String termsUrl =
      "https://pocket-poll-documents.s3.us-east-2.amazonaws.com/terms_conditions.html";
  static final String privacyPolicyUrl =
      "https://pocket-poll-documents.s3.us-east-2.amazonaws.com/privacy_policy.html";
  static final String flutterUrl = "https://flutter.dev/";
  static final String awsUrl = "https://aws.amazon.com/";
}
