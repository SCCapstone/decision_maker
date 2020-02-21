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
  Future<List<Group>> groupFuture;
  bool initialLoad = true;

  @override
  void initState() {
    List<String> groupId = new List<String>();
    groupId.add(widget.groupId);
    groupFuture = GroupsManager.getGroups(groupIds: groupId);
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        centerTitle: true,
        title: Text(
          (initialLoad) ? widget.groupName : Globals.currentGroup.groupName,
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
            FutureBuilder(
              future: groupFuture,
              builder: (BuildContext context, AsyncSnapshot snapshot) {
                if (snapshot.hasData) {
                  if (initialLoad) {
                    Globals.currentGroup = snapshot.data.first;
                    initialLoad = false;
                  }
                  return Expanded(
                    child: Container(
                      height: MediaQuery.of(context).size.height * .80,
                      child: RefreshIndicator(
                        child: EventsList(
                          group: Globals.currentGroup,
                          events: GroupsManager.getGroupEvents(
                              Globals.currentGroup),
                        ),
                        onRefresh: refreshList,
                      ),
                    ),
                  );
                } else if (snapshot.hasError) {
                  return Text("Error: ${snapshot.error}");
                }
                return Center(child: CircularProgressIndicator());
              },
            ),
            Padding(
              padding: EdgeInsets.all(MediaQuery.of(context).size.height * .02),
            )
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        child: Icon(Icons.add),
        onPressed: () {
          Navigator.push(context,
                  MaterialPageRoute(builder: (context) => CreateEvent()))
              .then((_) => GroupPage());
        },
      ),
    );
  }

  Future<Null> refreshList() async {
    List<String> groupId = new List<String>();
    groupId.add(widget.groupId);
    Globals.currentGroup =
        (await GroupsManager.getGroups(groupIds: groupId)).first;
    setState(() {});
  }
}
