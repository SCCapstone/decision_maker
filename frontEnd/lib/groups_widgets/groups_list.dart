import 'package:flutter/material.dart';
import 'package:frontEnd/models/user_group.dart';
import 'group_row.dart';

class GroupsList extends StatefulWidget {
  final List<UserGroup> groups;
  final bool searching;
  final Function refreshGroups;

  GroupsList({Key key, this.groups, this.searching, this.refreshGroups})
      : super(key: key);

  @override
  _GroupsListState createState() => _GroupsListState();
}

class _GroupsListState extends State<GroupsList> {
  @override
  Widget build(BuildContext context) {
    if (widget.groups.isEmpty) {
      return ListView(
        // list view so the refresh can be enabled
        children: <Widget>[
          Center(
            child: Text(
                (widget.searching)
                    ? "Group not found"
                    : "No groups found! Click the plus button below to create one!",
                style: TextStyle(fontSize: 30)),
          ),
        ],
      );
    } else {
      return Scrollbar(
        child: ListView.builder(
          shrinkWrap: true,
          itemCount: widget.groups.length,
          itemBuilder: (context, index) {
            return GroupRow(
              widget.groups[index],
              refreshGroups: widget.refreshGroups,
            );
          },
        ),
      );
    }
  }
}
