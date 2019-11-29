import 'package:flutter/material.dart';
import 'package:frontEnd/models/group.dart';
import 'group_row.dart';

class GroupsList extends StatefulWidget {
  final List<Group> groups;

  GroupsList({Key key, this.groups}) : super(key: key);

  @override
  _GroupsListState createState() => _GroupsListState();
}

class _GroupsListState extends State<GroupsList> {
  @override
  Widget build(BuildContext context) {
    if (widget.groups.length == 0) {
      return Center(
        child: Text(
            "No groups found! Click the plus button below to create one!",
            style: TextStyle(
                fontSize: DefaultTextStyle.of(context).style.fontSize * 0.7)),
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
