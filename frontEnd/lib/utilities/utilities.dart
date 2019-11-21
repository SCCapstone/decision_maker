import 'package:flutter/material.dart';
import 'validator.dart';

void showPopupMessage(String message, BuildContext context) {
  showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text("Response status:"),
          content: Text(message),
        );
      });
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
