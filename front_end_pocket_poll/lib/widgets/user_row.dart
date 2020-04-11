import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';

class UserRow extends StatelessWidget {
  final String displayName;
  final String username;
  final String icon;
  final bool showDelete;
  final bool showAdd;
  final bool isGroupOwner;
  final VoidCallback deleteUser;
  final VoidCallback addUser;

  UserRow(this.displayName, this.username, this.icon, this.showDelete,
      this.showAdd, this.isGroupOwner,
      {this.deleteUser, this.addUser});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * .07,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          GestureDetector(
            onTap: () {
              showUserImage(getUserIconImage(this.icon), context);
            },
            child: Container(
              height: MediaQuery.of(context).size.height * .2,
              width: MediaQuery.of(context).size.width * .15,
              decoration: BoxDecoration(
                  image: DecorationImage(
                      image: getUserIconImage(this.icon), fit: BoxFit.cover)),
            ),
          ),
          Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .005)),
          Expanded(
            child: AutoSizeText(
              "${this.displayName} (@${this.username})",
              maxLines: 1,
              minFontSize: 11,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontSize: 18),
            ),
          ),
          Visibility(
            visible: this.showDelete, // if owner, can't remove itself
            child: IconButton(
              icon: Icon(Icons.remove_circle_outline),
              color: Colors.red,
              onPressed: this.deleteUser,
            ),
          ),
          Visibility(
            visible: this.isGroupOwner,
            // show a special icon if user is the owner of the group. Used a button to make it centered
            child: IconButton(
              icon: Icon(Icons.star, color: Colors.yellow),
              tooltip: "Owner",
            ),
          ),
          Visibility(
            visible: this.showAdd,
            // show this if the user is typing in a username from their favorites
            child: IconButton(
              icon: Icon(Icons.add_circle_outline),
              color: Colors.greenAccent,
              onPressed: this.addUser,
            ),
          )
        ],
      ),
      decoration: BoxDecoration(
          border: Border(bottom: BorderSide(color: getBorderColor()))),
    );
  }
}
