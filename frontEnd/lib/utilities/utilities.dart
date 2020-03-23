import 'dart:io';

import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/user_tokens_manager.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:provider/provider.dart';
import '../main.dart';
import 'package:frontEnd/models/user.dart';

Future<bool> internetCheck() async {
  bool retVal = true;
  try {
    await InternetAddress.lookup('google.com');
  } on SocketException catch (_) {
    retVal = false;
  }
  return retVal;
}

void logOutUser(BuildContext context) {
  UsersManager.unregisterPushEndpoint();
  Globals.clearGlobals();
  Globals.activeUserCategories.clear();
  clearTokens();
}

ImageProvider getUserIconUrl(User user) {
  return user.icon == null
      ? AssetImage("assets/images/defaultUser.png")
      : NetworkImage(Globals.imageUrl + user.icon);
}

ImageProvider getUserIconUrlStr(String icon) {
  return icon == null
      ? AssetImage("assets/images/defaultUser.png")
      : NetworkImage(Globals.imageUrl + icon);
}

ImageProvider getGroupIconUrlStr(String icon) {
  return icon == null
      ? AssetImage("assets/images/defaultGroup.png")
      : NetworkImage(Globals.imageUrl + icon);
}

String getSortMethodString(int sortVal) {
  String retVal;
  if (sortVal == Globals.alphabeticalSort) {
    retVal = Globals.alphabeticalSortString;
  } else if (sortVal == Globals.alphabeticalReverseSort) {
    retVal = Globals.alphabeticalReverseSortString;
  } else if (sortVal == Globals.dateNewestSort) {
    retVal = Globals.dateNewestSortString;
  } else if (sortVal == Globals.dateOldestSort) {
    retVal = Globals.dateOldestSortString;
  } else {
    // shouldn't ever happen but if so just sort by newest first
    retVal = Globals.dateNewestSortString;
  }
  return retVal;
}

int getSortMethod(String sortString) {
  int retVal;
  if (sortString == Globals.alphabeticalSortString) {
    retVal = Globals.alphabeticalSort;
  } else if (sortString == Globals.alphabeticalReverseSortString) {
    retVal = Globals.alphabeticalReverseSort;
  } else if (sortString == Globals.dateNewestSortString) {
    retVal = Globals.dateNewestSort;
  } else if (sortString == Globals.dateOldestSortString) {
    retVal = Globals.dateOldestSort;
  } else {
    // shouldn't ever happen but if so just sort by newest first
    retVal = Globals.dateNewestSort;
  }
  return retVal;
}

int boolToInt(bool val) {
  if (val) {
    return 1;
  } else {
    return 0;
  }
}

bool intToBool(int val) {
  return val == 1;
}

String convertDateToString(DateTime date) {
  return date.toString().substring(0, 10);
}

String convertTimeToString(TimeOfDay time) {
  return time.toString().substring(10, 15);
}

String convertTimeToStringFormatted(TimeOfDay time) {
  String retVal = "";
  if (time.hourOfPeriod == 0) {
    // midnight and noon are given a value of zero
    retVal += "12:";
  } else {
    retVal += time.hourOfPeriod.toString() + ":";
  }
  if (time.minute < 10) {
    retVal += "0" + time.minute.toString();
  } else {
    retVal += time.minute.toString();
  }
  if (time.period == DayPeriod.am) {
    retVal += " AM";
  } else {
    retVal += " PM";
  }
  return retVal;
}

String convertDateTimeToString(DateTime dateTime) {
  return dateTime.toString().substring(0, 16);
}

int getHour(String time) {
  return int.parse(time.substring(0, 2));
}

int getMinute(String time) {
  return int.parse(time.substring(3, 5));
}

Color getBorderColor() {
  return (Globals.user.appSettings.darkTheme) ? Colors.white : Colors.black;
}

void showPopupMessage(String message, BuildContext context,
    {Function callback}) {
  if (context == null) {
    return;
  }

  if (callback == null) {
    callback = (_) => {};
  }

  showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text("Response status:"),
          content: Text(message),
        );
      }).then(callback);
}

void showErrorMessage(
    String errorTitle, String errorMsg, BuildContext context) {
  showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text(errorTitle),
          actions: <Widget>[
            FlatButton(
              child: Text("Ok"),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
          ],
          content: Text(errorMsg),
        );
      });
}

void showLoadingDialog(BuildContext context, String msg, bool dismissible) {
  showDialog(
      barrierDismissible: false,
      context: context,
      builder: (context) {
        return WillPopScope(
          // prevents the dialog from being exited by the back button
          onWillPop: () async => dismissible,
          child: AlertDialog(
            content: Flex(
              direction: Axis.horizontal,
              children: <Widget>[
                CircularProgressIndicator(),
                Padding(
                  padding: EdgeInsets.all(20),
                ),
                Flexible(
                  flex: 8,
                  child: Text(msg),
                )
              ],
            ),
          ),
        );
      });
}

void showUserImage(ImageProvider image, BuildContext buildContext) {
  showDialog(
      context: buildContext,
      builder: (context) {
        return AlertDialog(
          content: Image(image: image),
          actions: <Widget>[
            FlatButton(
              child: Text("Return"),
              onPressed: () {
                Navigator.of(context).pop();
              },
            )
          ],
        );
      });
}

void showGroupImage(ImageProvider image, BuildContext buildContext) {
  showDialog(
      context: buildContext,
      builder: (context) {
        return AlertDialog(
          content: Image(image: image),
          actions: <Widget>[
            FlatButton(
              child: Text("Return"),
              onPressed: () {
                Navigator.of(context).pop();
              },
            )
          ],
        );
      });
}

void hideKeyboard(BuildContext context) {
  FocusScope.of(context).requestFocus(new FocusNode());
}

void changeTheme(BuildContext context) {
  ThemeData selectedTheme;
  if (Globals.user.appSettings.darkTheme) {
    selectedTheme = Globals.darkTheme;
  } else {
    selectedTheme = Globals.lightTheme;
  }
  final ThemeNotifier themeNotifier =
      Provider.of<ThemeNotifier>(context, listen: false);
  themeNotifier.setTheme(selectedTheme);
}
