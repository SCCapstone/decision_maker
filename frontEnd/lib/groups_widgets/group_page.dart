import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/result_status.dart';
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
  Widget errorWidget;

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
      return errorWidget;
    } else {
      return Scaffold(
        appBar: AppBar(
          centerTitle: true,
          title: AutoSizeText(
            Globals.currentGroup.groupName,
            maxLines: 1,
            style: TextStyle(fontSize: 40),
            minFontSize: 12,
            overflow: TextOverflow.ellipsis,
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
        floatingActionButton: FloatingActionButton(
          child: Icon(Icons.add),
          onPressed: () {
            Navigator.push(context,
                    MaterialPageRoute(builder: (context) => CreateEvent()))
                .then((_) {
              // TODO figure out a better way to refresh without making unnecessary API calls
              this.refreshList();
            });
          },
        ),
      );
    }
  }

  void getGroup() async {
    List<String> groupId = new List<String>();
    groupId.add(widget.groupId);
    ResultStatus<List<Group>> status =
        await GroupsManager.getGroups(groupIds: groupId);
    initialLoad = false;
    if (status.success) {
      errorLoading = false;
      Globals.currentGroup = status.data.first;
      setState(() {});
    } else {
      errorLoading = true;
      errorWidget = groupError(status.errorMessage);
      setState(() {});
    }
  }

  Widget groupLoading() {
    return Scaffold(
        appBar: AppBar(
            centerTitle: true,
            title: Text(
              widget.groupName,
              style: TextStyle(
                  fontSize: DefaultTextStyle.of(context).style.fontSize * 0.8),
            )),
        body: Center(child: CircularProgressIndicator()));
  }

  Widget groupError(String errorMsg) {
    return Scaffold(
        appBar: AppBar(
            centerTitle: true,
            title: Text(
              widget.groupName,
              style: TextStyle(
                  fontSize: DefaultTextStyle.of(context).style.fontSize * 0.8),
            )),
        body: Container(
          height: MediaQuery.of(context).size.height * .80,
          child: RefreshIndicator(
            onRefresh: refreshList,
            child: ListView(
              children: <Widget>[
                Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .15)),
                Center(child: Text(errorMsg, style: TextStyle(fontSize: 30))),
              ],
            ),
          ),
        ));
  }

  Future<Null> refreshList() async {
    getGroup();
    setState(() {});
  }
}
