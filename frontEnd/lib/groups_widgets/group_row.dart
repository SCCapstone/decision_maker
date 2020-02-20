import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/groups_widgets/group_page.dart';
import 'package:frontEnd/groups_widgets/groups_home.dart';
import 'package:frontEnd/utilities/utilities.dart';

class GroupRow extends StatelessWidget {
  final Group group;
  final int index;

  GroupRow(this.group, this.index);

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        Container(
          height: MediaQuery.of(context).size.height * .14,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              IconButton(
                  iconSize: MediaQuery.of(context).size.width * .20,
                  icon: Image(
                    image: getIconUrl(group.icon),
                  ),
                  onPressed: () {
                    Globals.currentGroup = group;
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                          builder: (context) => GroupPage(
                                events: GroupsManager.getGroupEvents(group),
                              )),
                    ).then((_) => GroupsHome());
                  }),
              Expanded(
                child: GestureDetector(
                  onTap: () {
                    Globals.currentGroup = group;
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                          builder: (context) => GroupPage(
                                events: GroupsManager.getGroupEvents(group),
                              )),
                    ).then((_) => GroupsHome());
                  },
                  child: Container(
                    color: Colors.blueGrey.withOpacity(0.25),
                    height: MediaQuery.of(context).size.width * .20,
                    child: Center(
                      child: Text(
                        group.groupName,
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 25),
                      ),
                    ),
                  ),
                ),
              )
            ],
          ),
        ),
        Padding(
          padding: EdgeInsets.all(MediaQuery.of(context).size.height * .004),
        ),
      ],
    );
  }
}
