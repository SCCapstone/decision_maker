import 'package:flutter/material.dart';
import 'package:frontEnd/group_settings.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/widgets/events_list.dart';

import 'create_event.dart';

class GroupPage extends StatefulWidget {
  final Group group;
  final Map<String, Event> events;

  GroupPage({Key key, this.group, this.events}) : super(key: key);

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
          widget.group.groupName,
          style: TextStyle(
              fontSize: DefaultTextStyle.of(context).style.fontSize * 0.8),
        ),
        actions: <Widget>[
          IconButton(
            icon: Icon(Icons.settings),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (context) => GroupSettings(group: widget.group)),
              ).then((_) => GroupPage(group: widget.group));
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
            Container(
              height: MediaQuery.of(context).size.height * .70,
              child: EventsList(
                group: widget.group,
                events: widget.events,
              ),
            ),
            Padding(
              padding: EdgeInsets.all(MediaQuery.of(context).size.height * .02),
            ),
            RaisedButton(
              child: Text(
                "Create Event",
                style: TextStyle(
                    fontSize:
                        DefaultTextStyle.of(context).style.fontSize * 0.6),
              ),
              onPressed: () {
                Navigator.push(context,
                    MaterialPageRoute(builder: (context) =>
                        CreateEvent(group: widget.group)))
                    .then((_) => GroupPage(group: widget.group));
              },
            )
          ],
        ),
      ),
    );
  }
}
