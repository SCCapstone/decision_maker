import 'package:flutter/material.dart';

class UserRow extends StatelessWidget {
  final String displayName;
  final String username;
  final String icon;
  final bool showDelete;
  final bool adding;
  final bool isGroupOwner;
  final VoidCallback deleteUser;
  final VoidCallback addUser;

  UserRow(this.displayName, this.username, this.icon, this.showDelete,
      this.adding, this.isGroupOwner,
      {this.deleteUser, this.addUser});

  @override
  Widget build(BuildContext context) {
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
              "${this.displayName} (@${this.username})",
              style: TextStyle(fontSize: 18),
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
            visible: isGroupOwner,
            // show a special icon if user is the owner of the group. Use a button to make it centered
            child: IconButton(icon: Icon(Icons.star, color: Colors.yellow)),
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
