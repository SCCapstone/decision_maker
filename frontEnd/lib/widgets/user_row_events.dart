import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';

class UserRow extends StatelessWidget {
  final String displayName;

  UserRow(this.displayName);

  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * .07,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Expanded(
            child: Center(
              child: Text(
                this.displayName,
                style: TextStyle(fontSize: 20),
              ),
            ),
          ),
        ],
      ),
      decoration: new BoxDecoration(
          border: new Border(
              bottom: new BorderSide(
                  color: (Globals.user.appSettings.darkTheme)
                      ? Colors.white
                      : Colors.black))),
    );
  }
}
