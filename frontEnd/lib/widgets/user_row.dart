import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';

class UserRow extends StatelessWidget {
  final String displayName;
  final String username;
  final VoidCallback deleteUser;

  UserRow(this.displayName, this.username, {this.deleteUser});

  @override
  Widget build(BuildContext context) {
    bool showDelete = (this.username == Globals.username);

    return Container(
      height: MediaQuery.of(context).size.height * .07,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Expanded(
            child: Text(
              this.displayName,
              style: TextStyle(fontSize: 20),
            ),
          ),
          Visibility(
            visible: showDelete, // if owner, can't remove itself
            child: IconButton(
              icon: Icon(Icons.remove_circle_outline),
              color: Colors.red,
              onPressed: this.deleteUser,
            ),
          )
        ],
      ),
      decoration:
          new BoxDecoration(border: new Border(bottom: new BorderSide())),
    );
  }
}
