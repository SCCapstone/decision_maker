import 'package:dynamic_theme/dynamic_theme.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:provider/provider.dart';
import '../main.dart';
import 'validator.dart';

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

int getHour(String time) {
  return int.parse(time.substring(0, 2));
}

int getMinute(String time) {
  return int.parse(time.substring(3, 5));
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

void groupIconPopup(BuildContext context, bool validate,
    TextEditingController controller, Function function) {
  // displays a popup for editing the group icon's url

  final formKey = GlobalKey<FormState>();
  showDialog(
      context: context,
      builder: (context) {
        return Form(
          autovalidate: validate,
          key: formKey,
          child: AlertDialog(
            title: Text("Edit Icon url"),
            actions: <Widget>[
              FlatButton(
                child: Text("Cancel"),
                onPressed: () {
                  controller.clear();
                  Navigator.of(context).pop();
                },
              ),
              FlatButton(
                child: Text("Submit"),
                onPressed: () {
                  if (formKey.currentState.validate()) {
                    function(controller.text);
                  }
                },
              ),
            ],
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                TextFormField(
                    controller: controller,
                    validator: validGroupIcon,
                    keyboardType: TextInputType.url,
                    decoration: InputDecoration(
                      labelText: "Enter a icon link",
                    )),
              ],
            ),
          ),
        );
      });
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

void showLoadingDialog(BuildContext context, String msg) {
  showDialog(
      barrierDismissible: false,
      context: context,
      builder: (context) {
        return WillPopScope(
          // prevents the dialog from being exited by the back button
          onWillPop: () async => false,
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

void hideKeyboard(BuildContext context) {
  FocusScope.of(context).requestFocus(new FocusNode());
}

void changeTheme(BuildContext context) {
  if (Globals.user.appSettings.darkTheme) {
    Globals.selectedTheme = Globals.darkTheme;
  } else {
    Globals.selectedTheme = Globals.lightTheme;
  }
//  final ThemeNotifier themeNotifier =
//      Provider.of<ThemeNotifier>(context, listen: false);
//  themeNotifier.setTheme(Globals.selectedTheme);
  DynamicTheme.of(context).setThemeData(Globals.selectedTheme);
}
