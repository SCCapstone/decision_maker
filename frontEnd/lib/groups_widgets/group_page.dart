import 'package:flutter/material.dart';
import 'groups_settings.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/events_widgets/events_list.dart';

import 'package:frontEnd/events_widgets/event_create.dart';

class GroupPage extends StatefulWidget {
  final Map<String, Event> events;

  GroupPage({Key key, this.events}) : super(key: key);

  @override
  _GroupPageState createState() => new _GroupPageState();
}

class _GroupPageState extends State<GroupPage> {
  @override
  Widget build(BuildContext context) {
    return new Scaffold(
      appBar: new AppBar(
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
                    events: widget.events,
                  ),
                  onRefresh: refreshList,
                ),
              ),
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
    await Future.delayed(
        Duration(milliseconds: 70)); // required to remove the loading animation
    setState(() {});
  }
}
