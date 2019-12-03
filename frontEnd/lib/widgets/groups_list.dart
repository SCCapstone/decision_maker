import 'package:flutter/material.dart';
import 'package:frontEnd/models/group.dart';
import 'group_row.dart';

class GroupsList extends StatefulWidget {
  final List<Group> groups;
  final bool searching;

  GroupsList({Key key, this.groups, this.searching}) : super(key: key);

  @override
  _GroupsListState createState() => _GroupsListState();
}

class _GroupsListState extends State<GroupsList> {
  @override
  Widget build(BuildContext context) {
    if (widget.groups.length == 0) {
      return Center(
        child: Text(
            (widget.searching)
                ? "Group not found"
                : "No groups found! Click the plus button below to create one!",
            style: TextStyle(fontSize: 30)),
      );
    } else {
      return Scrollbar(
        child: ListView.builder(
          shrinkWrap: true,
          itemCount: widget.groups.length,
          itemBuilder: (context, index) {
            return GroupRow(widget.groups[index], index);
          },
        ),
      );
    }
  }
}
