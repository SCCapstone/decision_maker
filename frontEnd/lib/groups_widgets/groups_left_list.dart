import 'package:flutter/material.dart';
import 'package:frontEnd/groups_widgets/group_left_row.dart';
import 'package:frontEnd/models/group_left.dart';

class GroupsLeftList extends StatefulWidget {
  final List<GroupLeft> groupsLeft;
  final bool searching;
  final Function refreshGroups;

  GroupsLeftList({Key key, this.groupsLeft, this.searching, this.refreshGroups})
      : super(key: key);

  @override
  _GroupsLeftListState createState() => _GroupsLeftListState();
}

class _GroupsLeftListState extends State<GroupsLeftList> {
  @override
  Widget build(BuildContext context) {
    if (widget.groupsLeft.isEmpty) {
      return ListView(
        // list view so the refresh can be enabled
        children: <Widget>[
          Center(
            child: Text(
                (widget.searching)
                    ? "Group not found."
                    : "You have not left any groups.",
                style: TextStyle(fontSize: 30)),
          ),
        ],
      );
    } else {
      return Scrollbar(
        child: ListView.builder(
          shrinkWrap: true,
          itemCount: widget.groupsLeft.length,
          itemBuilder: (context, index) {
            return GroupLeftRow(
              widget.groupsLeft[index],
              refreshGroups: widget.refreshGroups,
            );
          },
        ),
      );
    }
  }
}
