import 'package:flutter/material.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/widgets/user_row.dart';

class UsersDropdown extends StatelessWidget {
  final Map<String, dynamic> users;
  final Function deleteCallback;
  final Function addCallback;
  final String dropdownTitle;

  UsersDropdown(this.dropdownTitle, this.users,
      {this.deleteCallback, this.addCallback});

  @override
  Widget build(BuildContext context) {
    if (users.length == 0) {
      return SizedBox(
        height: MediaQuery.of(context).size.height * .2,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: <Widget>[
            Text("No users! Click \"Add\" to add some!"),
            RaisedButton(
              child: Text("Add"),
              onPressed: () {
                addUserPopup(context, users, this.addCallback);
              },
            )
          ],
        ),
      );
    } else {

      List<Widget> userRows = new List<Widget>();
      for (String username in this.users.keys) {
        userRows.add(UserRow(users[username].toString(), username, deleteUser: () => deleteCallback(users[username])));
      }

      return ExpansionTile(
        title: Text(dropdownTitle),
        children: <Widget>[
          SizedBox(
            height: MediaQuery.of(context).size.height * .2,
            child: ListView(
              shrinkWrap: true,
              children: userRows,
            ),
          ),
          RaisedButton(
            child: Text("Add"),
            onPressed: () {
              addUserPopup(context, users, this.addCallback);
            },
          )
        ],
      );
    }
  }
}
