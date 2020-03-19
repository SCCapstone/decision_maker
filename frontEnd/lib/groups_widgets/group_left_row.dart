import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/models/group_left.dart';
import 'package:frontEnd/models/user_group.dart';
import 'package:frontEnd/utilities/utilities.dart';

import 'groups_home.dart';

class GroupLeftRow extends StatefulWidget {
  final GroupLeft group;
  final Function refreshGroups;

  GroupLeftRow(this.group, {this.refreshGroups});

  @override
  _GroupLeftRowState createState() => _GroupLeftRowState();
}

class _GroupLeftRowState extends State<GroupLeftRow> {
  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        Container(
          height: MediaQuery.of(context).size.height * .14,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              GestureDetector(
                  onTap: () {
                    confirmRejoinGroup();
                  },
                child: Container(
                  height: MediaQuery.of(context).size.width * .20,
                  width: MediaQuery.of(context).size.width * .20,
                  decoration: BoxDecoration(
                      image: DecorationImage(
                          image: getIconUrl(widget.group.icon),
                          fit: BoxFit.cover)),
                ),
              ),
              Padding(
                  padding: EdgeInsets.all(
                      MediaQuery.of(context).size.height * .002)),
              Expanded(
                child: GestureDetector(
                  onTap: () {
                    confirmRejoinGroup();
                  },
                  child: Container(
                    color: Colors.blueGrey.withOpacity(0.25),
                    height: MediaQuery.of(context).size.width * .20,
                    child: Center(
                      child: AutoSizeText(
                        widget.group.groupName,
                        minFontSize: 12,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 25),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
        Padding(
          padding: EdgeInsets.all(MediaQuery.of(context).size.height * .004),
        ),
      ],
    );
  }

  // Display an alert dialog to confirm whether the user wants to rejoin this group
  void confirmRejoinGroup() {
    showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: Text("Rejoin Group?"),
            actions: <Widget>[
              FlatButton(
                child: Text("Yes"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                  tryRejoin();
                },
              ),
              FlatButton(
                child: Text("No"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                },
              )
            ],
            content: Text(
                "Would you like to rejoin ${widget.group.groupName}?"),
          );
        });
  }

  // Send the request to the back end to rejoin the group, refresh the groups
  // list upon success
  void tryRejoin() async {
    showLoadingDialog(context, "Rejoining group...", true);
    ResultStatus resultStatus =
        await GroupsManager.rejoinGroup(widget.group.groupId);
    Navigator.of(context, rootNavigator: true).pop('dialog');

    if (resultStatus.success) {
      Globals.user.groupsLeft.remove(widget.group.groupId);
      widget.refreshGroups();
    } else {
      showErrorMessage("Error", resultStatus.errorMessage, context);
    }
  }
}
