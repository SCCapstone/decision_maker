import 'package:flutter/material.dart';
import 'package:frontEnd/group_settings.dart';
import 'package:frontEnd/models/group.dart';

class GroupPage extends StatefulWidget {
  final Group group;

  GroupPage({Key key, this.group}) : super(key: key);

  @override
  _GroupPageState createState() => new _GroupPageState();
}

class _GroupPageState extends State<GroupPage> {
  @override
  void initState() {
    // TODO load all the events (https://github.com/SCCapstone/decision_maker/issues/44)
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return new Scaffold(
      appBar: new AppBar(
        centerTitle: true,
        title: Text(
          widget.group.groupName,
          style: TextStyle(fontSize: DefaultTextStyle.of(context).style.fontSize * 0.8),
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
              height: MediaQuery.of(context).size.height * .60,
              child: ListView(
                shrinkWrap: true,
                padding: EdgeInsets.all(10.0),
                children: <Widget>[
                  Container(
                      width: MediaQuery.of(context).size.width * .80,
                      height: MediaQuery.of(context).size.height * .25,
                      color: Colors.green,
                      child: Center(child: Text("Events will go here"))),
                  Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .01),
                  ),
                  Container(
                      width: MediaQuery.of(context).size.width * .80,
                      height: MediaQuery.of(context).size.height * .25,
                      color: Colors.green,
                      child: Center(child: Text("Events will go here"))),
                  Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .01),
                  ),
                  Container(
                      width: MediaQuery.of(context).size.width * .80,
                      height: MediaQuery.of(context).size.height * .25,
                      color: Colors.green,
                      child: Center(child: Text("Events will go here"))),
                ],
              ),
            ),
            Padding(
              padding: EdgeInsets.all(MediaQuery.of(context).size.height * .01),
            ),
            Padding(
              padding: EdgeInsets.all(MediaQuery.of(context).size.height * .01),
            ),
            RaisedButton(
              child: Text(
                "Create Event",
                style: TextStyle(fontSize: DefaultTextStyle.of(context).style.fontSize * 0.6),
              ),
              onPressed: () {},
            )
          ],
        ),
      ),
    );
  }
}
