import 'package:flutter/material.dart';
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

void addUserPopup(
    BuildContext context, List<String> users, Function function) {
  // displays a popup for adding a new user to a group
  TextEditingController controller = new TextEditingController();
  final formKey = GlobalKey<FormState>();
  showDialog(
      context: context,
      builder: (context) {
        return Form(
          autovalidate: false,
          key: formKey,
          child: AlertDialog(
            title: Text("Enter a username"),
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
                    Navigator.of(context).pop();
                  }
                },
              ),
            ],
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                TextFormField(
                    controller: controller,
                    validator: (user) => validUser(user, users),
                    keyboardType: TextInputType.text,
                    decoration: InputDecoration(
                      labelText: "Username",
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
