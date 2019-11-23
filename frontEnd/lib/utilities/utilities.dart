import 'package:flutter/material.dart';

void showPopupMessage(String message, BuildContext context, {Function callback}) {
  showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text("Response status:"),
          content: Text(message),
        );
      }).then(callback);
}

Map<String, dynamic> getEmptyApiRequest() {
  return {"action": "", "payload": {}};
}
