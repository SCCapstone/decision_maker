import 'dart:async';
import 'package:flutter/material.dart';
import 'package:frontEnd/create_group.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/group_page.dart';
import 'package:frontEnd/main.dart';
import 'imports/categories_manager.dart';
import 'models/group.dart';
import 'imports/globals.dart';

class GroupsHome extends StatefulWidget {
  Future<List<Group>> groups;

  GroupsHome({Key key, this.groups}) : super(key: key);

  @override
  _GroupsHomeState createState() => new _GroupsHomeState();
}

class _GroupsHomeState extends State<GroupsHome> {
  @override
  void initState() {
    widget.groups = GroupsManager.getAllGroupsList();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    Icon navIcon;
    if (Globals.android) {
      navIcon = new Icon(Icons.dehaze);
    } else {
      navIcon = new Icon(Icons.arrow_back);
    }
    return new Scaffold(
      appBar: new AppBar(
        centerTitle: true,
        title: Text(
          "PocketPoll",
          style: TextStyle(fontSize: 35),
        ),
        actions: <Widget>[
          IconButton(
            icon: Icon(Icons.search),
            iconSize: 40,
            onPressed: () {
              // TODO implement a group search (https://github.com/SCCapstone/decision_maker/issues/42)
            },
          )
        ],
        leading: IconButton(
          icon: navIcon,
          iconSize: 40,
          onPressed: () {
            // TODO link up with nav bar (https://github.com/SCCapstone/decision_maker/issues/78)
          },
        ),
      ),
      body: Center(
        child: Column(
          children: <Widget>[
            Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .015),
            ),
            Expanded(
              child: new Container(
                  width: MediaQuery.of(context).size.width * .80,
                  height: MediaQuery.of(context).size.height * .60,
                  child: FutureBuilder(
                    future: widget.groups,
                    builder: (BuildContext context, AsyncSnapshot snapshot) {
                      if (snapshot.hasData) {
                        List<Group> groups = snapshot.data;
                        return GroupsList(groups: groups);
                      } else if (snapshot.hasError) {
                        print(snapshot.error);
                        return Text("Error: ${snapshot.error}");
                      }
                      return Center(child: CircularProgressIndicator());
                    },
                  )),
            ),
            Padding(
              // used to make sure the group list doesn't go too far down, expanded widget stops when reaching this
              padding: EdgeInsets.all(MediaQuery.of(context).size.height * .08),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        child: Icon(Icons.add),
        onPressed: () {
          // Navigate to second route when tapped.
          Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => CreateGroup()),
          ).then((_) => GroupsHome());
        },
      ),
    );
  }
}

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
            style: TextStyle(fontSize: 25)),
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

class GroupRow extends StatelessWidget {
  final Group group;
  final int index;

  GroupRow(this.group, this.index);

  @override
  Widget build(BuildContext context) {
    return Container(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          IconButton(
            // TODO have picture
            icon: Icon(Icons.accessibility),
          ),
          RaisedButton(
            child: Text(
              group.groupName,
              style: TextStyle(fontSize: 30),
            ),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (context) => GroupPage(group: this.group)),
              ).then((_) => GroupsHome());
            },
          )
        ],
      ),
      decoration:
          new BoxDecoration(border: new Border(bottom: new BorderSide())),
    );
  }
}
