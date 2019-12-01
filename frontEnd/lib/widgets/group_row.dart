import 'package:flutter/material.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/models/group.dart';
import '../group_page.dart';
import '../groups_home.dart';

class GroupRow extends StatelessWidget {
  final Group group;
  final int index;

  GroupRow(this.group, this.index);

  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * .14,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          IconButton(
            iconSize: MediaQuery.of(context).size.width * .20,
            icon: Image(
              image: NetworkImage(group.icon),
            ),
          ),
          Expanded(
            child: RaisedButton(
              child: Text(
                group.groupName,
                style: TextStyle(fontSize: 25),
              ),
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => GroupPage(
                            group: this.group,
                            events: GroupsManager.getGroupEvents(group),
                          )),
                ).then((_) => GroupsHome());
              },
            ),
          )
        ],
      ),
    );
  }
}
