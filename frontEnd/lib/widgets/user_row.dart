import 'package:flutter/material.dart';

class UserRow extends StatelessWidget {
  final String displayName;
  final String username;
  final String icon;
  final bool showDelete;
  final bool adding;
  final VoidCallback deleteUser;
  final VoidCallback addUser;

  UserRow(this.displayName, this.username, this.icon,
      this.showDelete, this.adding,
      {this.deleteUser, this.addUser});

  @override
  Widget build(BuildContext context) {
//    bool showDelete = ((this.activeUserIsGroupOwner &&
//            this.username != Globals.username) ||
//        (!this.activeUserIsGroupOwner && this.username == Globals.username));

    return Container(
      height: MediaQuery.of(context).size.height * .07,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Container(
            decoration: BoxDecoration(
                image: DecorationImage(
                    fit: BoxFit.fitHeight, image: NetworkImage(icon))),
          ),
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
          ),
          Visibility(
            visible: adding,
            // show this if the user is typing in a username from their favorites
            child: IconButton(
              icon: Icon(Icons.add_circle_outline),
              color: Colors.greenAccent,
              onPressed: this.addUser,
            ),
          )
        ],
      ),
      decoration:
          new BoxDecoration(border: new Border(bottom: new BorderSide())),
    );
  }
}
