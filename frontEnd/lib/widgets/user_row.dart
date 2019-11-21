import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/user.dart';

class UserRow extends StatelessWidget {
  final String username;
  final int index;
  final VoidCallback deleteUser;

  UserRow(this.username, this.index, {this.deleteUser});

  @override
  Widget build(BuildContext context) {
    bool showDelete = true;
    if(username == Globals.username){
      showDelete = false;
    }
    return Container(
      height: MediaQuery.of(context).size.height * .07,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Expanded(
            child: Text(
              username,
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
