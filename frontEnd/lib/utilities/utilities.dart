import 'package:flutter/material.dart';

void showPopupMessage(String message, BuildContext context) {
  showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text("Response status:"),
          content: Text(message),
        );
      }
  );
}

Map<String, dynamic> getEmptyApiRequest() {
  return {
    "action": "",
    "payload": {}
  };
}