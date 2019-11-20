import 'dart:async';
import 'package:flutter/material.dart';
import 'package:frontEnd/group_settings.dart';
import 'package:frontEnd/models/group.dart';
import 'imports/categories_manager.dart';
import 'models/category.dart';
import 'imports/globals.dart';

const String SETTINGS_ITEM = "Settings";
const String PREFERENCES_ITEM = "Preferences";

class GroupPage extends StatefulWidget {
  final Group group;

  GroupPage({Key key, this.group}) : super(key: key);

  @override
  _GroupPageState createState() => new _GroupPageState();
}

class _GroupPageState extends State<GroupPage> {
  final List<String> dropdownChoices = <String>[
    SETTINGS_ITEM,
    PREFERENCES_ITEM
  ];

  @override
  void initState() {
    // TODO load all the events
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return new Scaffold(
      appBar: new AppBar(
        centerTitle: true,
        title: Text(
          widget.group.groupName,
          style: TextStyle(fontSize: 35),
        ),
        actions: <Widget>[
          PopupMenuButton(
            onSelected: menuItemSelected,
            itemBuilder: (BuildContext context) {
              return dropdownChoices.map((String choice) {
                return PopupMenuItem<String>(
                  value: choice,
                  child: Text(choice),
                );
              }).toList();
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
                style: TextStyle(fontSize: 30),
              ),
              onPressed: () {},
            )
          ],
        ),
      ),
    );
  }

  void menuItemSelected(String item) {
    if (item == SETTINGS_ITEM) {
      Navigator.push(
        context,
        MaterialPageRoute(
            builder: (context) => GroupSettings(group: widget.group)),
      ).then((_) => GroupPage(group: widget.group));
    } else if (item == PREFERENCES_ITEM) {
      // TODO link to preferences
    }
  }
}
