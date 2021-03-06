import 'dart:io';

import 'package:flutter/material.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/imports/user_tokens_manager.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/favorite.dart';
import 'package:front_end_pocket_poll/models/group.dart';
import 'package:front_end_pocket_poll/utilities/validator.dart';
import 'package:provider/provider.dart';
import '../main.dart';

// checks if internet connection is okay on startup
Future<bool> internetCheck() async {
  bool retVal = true;
  try {
    await InternetAddress.lookup('google.com');
  } on SocketException catch (_) {
    retVal = false;
  }
  return retVal;
}

// log out the user and clear all local variables they might have stored
void logOutUser(BuildContext context) async {
  UsersManager.unregisterPushEndpoint();
  Globals.cachedCategories.clear();
  await clearSharedPrefs();
  Globals.user = null;
}

ImageProvider getUserIconImage(String iconUrl) {
  return iconUrl == null
      ? AssetImage("assets/images/defaultUser.png")
      : NetworkImage(Globals.imageUrl + iconUrl);
}

ImageProvider getGroupIconImage(String iconUrl) {
  return iconUrl == null
      ? AssetImage("assets/images/defaultGroup.png")
      : NetworkImage(Globals.imageUrl + iconUrl);
}

// converts a sort val from an int to a string based on the values in the global file
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

// converts a sort val from an string to a int based on the values in the global file
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

// converts from AM/PM hour to military time hour
int convertMeridianHrToMilitary(int hour, bool am) {
  int formattedHr = 0;
  if (am) {
    if (hour == 12) {
      formattedHr = 0;
    } else {
      formattedHr = hour;
    }
  } else {
    // time is pm, if it's 12 then don't add 12 on to it
    if (hour == 12) {
      formattedHr = hour;
    } else {
      formattedHr = hour + 12;
    }
  }
  return formattedHr;
}

// converts from military time hour to AM/PM hour
int convertMilitaryHrToMeridian(int hour) {
  int formattedHr;
  if (hour == 0) {
    formattedHr = 12;
  } else if (hour > 12) {
    formattedHr = hour - 12;
  } else {
    formattedHr = hour;
  }
  return formattedHr;
}

// returns border color for containers based on whether user is in dark mode or light mode
Color getBorderColor() {
  return (Globals.user.appSettings.darkTheme) ? Colors.white : Colors.black;
}

// shows an error message popup with a custom title and body
void showErrorMessage(
    String errorTitle, String errorMsg, BuildContext context) {
  showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text(errorTitle),
          key: Key("utilities:error_message_popup"),
          actions: <Widget>[
            FlatButton(
              child: Text("OK"),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
          ],
          content: Text(errorMsg),
        );
      });
}

// shows a help message popup with a custom title and body
void showHelpMessage(String helpTitle, String helpMsg, BuildContext context) {
  showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text(helpTitle),
          key: Key("utilities:help_message_popup"),
          actions: <Widget>[
            FlatButton(
              child: Text("OK"),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
          ],
          content: Text(helpMsg),
        );
      });
}

// shows a loading dialog. Used for API calls.
void showLoadingDialog(BuildContext context, String msg, bool dismissible) {
  showDialog(
      barrierDismissible: false,
      context: context,
      builder: (context) {
        return WillPopScope(
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

// shows a blown up image of another user. Allows for adding said user to favorites
void showUserImage(Favorite user, BuildContext buildContext) {
  bool showFavoriteButton = user.username != Globals.user.username;
  if (showFavoriteButton) {
    for (Favorite favorite in Globals.user.favorites) {
      if (favorite.username == user.username) {
        showFavoriteButton = false;
        break;
      }
    }
  }

  showDialog(
      context: buildContext,
      builder: (context) {
        return AlertDialog(
          content: Image(image: getUserIconImage(user.icon)),
          title: Text("${user.displayName} (@${user.username})"),
          actions: <Widget>[
            Visibility(
              visible: user.username != Globals.user.username,
              child: FlatButton(
                child: Text("REPORT"),
                onPressed: () {
                  reportUserDialog(user, buildContext);
                },
              ),
            ),
            Visibility(
                visible: showFavoriteButton,
                child: FlatButton(
                    child: Text("ADD TO FAVORITES"),
                    onPressed: () {
                      Globals.user.favorites.add(user);
                      Navigator.of(context).pop();

                      UsersManager.addFavorite(user).then((resultStatus) {
                        if (!resultStatus.success) {
                          Globals.user.favorites.remove(user);
                          showErrorMessage(
                              "Error", resultStatus.errorMessage, buildContext);
                        }
                      });
                    })),
            FlatButton(
              child: Text("RETURN"),
              onPressed: () {
                Navigator.of(context).pop();
              },
            )
          ],
        );
      });
}

// allows active user to report specific user
void reportUserDialog(Favorite user, BuildContext buildContext) {
  final TextEditingController editingController = new TextEditingController();
  final GlobalKey<FormState> reportForm = GlobalKey<FormState>();
  showDialog(
      context: buildContext,
      builder: (context) {
        return AlertDialog(
          title: Text("Report User"),
          content: Form(
            key: reportForm,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Text(
                      "Please provide a brief description of why you wish to report \"${user.username}\"."),
                  TextFormField(
                    controller: editingController,
                    maxLines: 3,
                    maxLength: Globals.maxReportMessageLength,
                    validator: validReportMessage,
                    smartQuotesType: SmartQuotesType.disabled,
                    smartDashesType: SmartDashesType.disabled,
                  )
                ],
              ),
            ),
          ),
          actions: <Widget>[
            FlatButton(
              child: Text("CANCEL"),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            FlatButton(
              child: Text("SUBMIT"),
              onPressed: () {
                final form = reportForm.currentState;
                if (form.validate()) {
                  UsersManager.reportUser(
                      user.username, editingController.text.toString().trim());
                  Navigator.of(context).pop();
                  Fluttertoast.showToast(
                      msg: "Thank you! We are processing your report now.",
                      toastLength: Toast.LENGTH_LONG,
                      gravity: ToastGravity.CENTER);
                }
              },
            )
          ],
        );
      });
}

// allows the user to report a group (can be for inappropriate images/events)
void reportGroupDialog(Group group, BuildContext buildContext) {
  final TextEditingController editingController = new TextEditingController();
  final GlobalKey<FormState> reportForm = GlobalKey<FormState>();
  showDialog(
      context: buildContext,
      builder: (context) {
        return AlertDialog(
          title: Text("Report Group"),
          content: Form(
            key: reportForm,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Text(
                      "Please provide a brief description of why you wish to report the group: \"${group.groupName}\"."),
                  TextFormField(
                    controller: editingController,
                    maxLines: 3,
                    maxLength: Globals.maxReportMessageLength,
                    validator: validReportMessage,
                    smartQuotesType: SmartQuotesType.disabled,
                    smartDashesType: SmartDashesType.disabled,
                  )
                ],
              ),
            ),
          ),
          actions: <Widget>[
            FlatButton(
              child: Text("CANCEL"),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            FlatButton(
              child: Text("SUBMIT"),
              onPressed: () {
                final form = reportForm.currentState;
                if (form.validate()) {
                  GroupsManager.reportGroup(
                      group.groupId, editingController.text.toString().trim());
                  Navigator.of(context).pop();
                  Fluttertoast.showToast(
                      msg: "Thank you! We are processing your report now.",
                      toastLength: Toast.LENGTH_LONG,
                      gravity: ToastGravity.CENTER);
                }
              },
            )
          ],
        );
      });
}

// allows the user to give general app feedback
void appFeedbackDialog(BuildContext buildContext) {
  final TextEditingController editingController = new TextEditingController();
  final GlobalKey<FormState> reportForm = GlobalKey<FormState>();
  showDialog(
      context: buildContext,
      builder: (context) {
        return AlertDialog(
          title: Text("Pocket Poll Feedback"),
          content: Form(
            key: reportForm,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Text(
                      "Let us know of any issues or concerns you have with the app! "
                      "Feel free to also let us know of features you enjoy or would like to be implemented in the future.\n\n"
                      "Consider giving us a rating on the app store too!"),
                  TextFormField(
                    controller: editingController,
                    maxLines: 3,
                    maxLength: Globals.maxReportMessageLength,
                    validator: validReportMessage,
                    smartQuotesType: SmartQuotesType.disabled,
                    smartDashesType: SmartDashesType.disabled,
                  )
                ],
              ),
            ),
          ),
          actions: <Widget>[
            FlatButton(
              child: Text("CANCEL"),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            FlatButton(
              child: Text("SUBMIT"),
              onPressed: () {
                final form = reportForm.currentState;
                if (form.validate()) {
                  UsersManager.giveAppFeedback(Globals.user.username,
                      editingController.text.toString().trim());
                  Navigator.of(context).pop();
                  Fluttertoast.showToast(
                      msg: "Thank you for your feedback!.",
                      toastLength: Toast.LENGTH_LONG,
                      gravity: ToastGravity.CENTER);
                }
              },
            )
          ],
        );
      });
}

// shows a blown up image of active user. Static image for now
void showActiveUserImage(ImageProvider image, BuildContext buildContext) {
  showDialog(
      context: buildContext,
      builder: (context) {
        return AlertDialog(
          content: Image(image: image),
          actions: <Widget>[
            FlatButton(
              child: Text("RETURN"),
              onPressed: () {
                Navigator.of(context).pop();
              },
            )
          ],
        );
      });
}

// shows a blown up image of a group icon
void showGroupImage(ImageProvider image, BuildContext buildContext) {
  showDialog(
      context: buildContext,
      builder: (context) {
        return AlertDialog(
          content: Image(image: image),
          actions: <Widget>[
            FlatButton(
              child: Text("RETURN"),
              onPressed: () {
                Navigator.of(context).pop();
              },
            )
          ],
        );
      });
}

// hides the user input keyboard if it is already open
void hideKeyboard(BuildContext context) {
  FocusScope.of(context).requestFocus(new FocusNode());
}

// changes the global theme of the app
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

// used for returning a UTC timestamp to the DB for new events
int getUtcSecondsSinceEpoch(final DateTime dateTime) {
  return (dateTime.millisecondsSinceEpoch / 1000).ceil();
}
