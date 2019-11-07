import 'package:flutter/material.dart';

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

void showLoadingDialog(BuildContext context, String msg) {
  showDialog(
      barrierDismissible: false,
      context: context,
      builder: (context) {
        return AlertDialog(
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
        );
      });
}
