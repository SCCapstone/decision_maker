import 'package:flutter/material.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/models/group.dart';
import 'groups_settings.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/events_widgets/events_list.dart';

import 'package:frontEnd/events_widgets/event_create.dart';

class GroupPage extends StatefulWidget {
  final String groupId;
  final String groupName;

  GroupPage({Key key, this.groupId, this.groupName}) : super(key: key);

  @override
  _GroupPageState createState() => new _GroupPageState();
}

class _GroupPageState extends State<GroupPage> {
  bool initialLoad = true;
  bool errorLoading = false;

  @override
  void initState() {
    getGroup();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    if (initialLoad) {
      return groupLoading();
    } else if (errorLoading) {
      return groupError();
    } else {
      return Scaffold(
        appBar: AppBar(
          centerTitle: true,
          title: Text(
            Globals.currentGroup.groupName,
            style: TextStyle(
                fontSize: DefaultTextStyle.of(context).style.fontSize * 0.8),
          ),
          actions: <Widget>[
            IconButton(
              icon: Icon(Icons.settings),
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => GroupSettings()),
                ).then((_) => GroupPage());
              },
            ),
          ],
        ),
        body: Center(
          child: Column(
            children: <Widget>[
              Padding(
                padding:
                    EdgeInsets.all(MediaQuery.of(context).size.height * .015),
              ),
              Expanded(
                child: Container(
                  height: MediaQuery.of(context).size.height * .80,
                  child: RefreshIndicator(
                    child: EventsList(
                      group: Globals.currentGroup,
                      events:
                          GroupsManager.getGroupEvents(Globals.currentGroup),
                    ),
                    onRefresh: refreshList,
                  ),
                ),
              ),
              Padding(
                padding:
                    EdgeInsets.all(MediaQuery.of(context).size.height * .02),
              )
            ],
          ),
        ),
        floatingActionButton: Visibility(
          visible: !initialLoad,
          child: FloatingActionButton(
            child: Icon(Icons.add),
            onPressed: () {
              Navigator.push(context,
                      MaterialPageRoute(builder: (context) => CreateEvent()))
                  .then((_) => GroupPage());
            },
          ),
        ),
      );
    }
  }

  void getGroup() async {
    List<String> groupId = new List<String>();
    groupId.add(widget.groupId);
    List<Group> tempList = await GroupsManager.getGroups(groupIds: groupId);
    if (tempList.length != 0) {
      print(tempList.first);
      initialLoad = false;
      Globals.currentGroup = tempList.first;
      setState(() {});
    } else {
      initialLoad = false;
      errorLoading = true;
      setState(() {});
    }
  }

  Widget groupLoading() {
    return Scaffold(
        appBar: AppBar(
            centerTitle: true,
            title: Text(
              (initialLoad) ? widget.groupName : Globals.currentGroup.groupName,
              style: TextStyle(
                  fontSize: DefaultTextStyle.of(context).style.fontSize * 0.8),
            )),
        body: Center(child: CircularProgressIndicator()));
  }

  Widget groupError() {
    return Scaffold(
        appBar: AppBar(
            centerTitle: true,
            title: Text(
              widget.groupName,
              style: TextStyle(
                  fontSize: DefaultTextStyle.of(context).style.fontSize * 0.8),
            )),
        body: Center(
            child: Text(
          "Error loading the group.",
          style: TextStyle(fontSize: 30),
        )));
  }

  Future<Null> refreshList() async {
    getGroup();
    setState(() {});
  }
}
