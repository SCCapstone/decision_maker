import 'dart:async';
import 'package:flutter/material.dart';
import 'package:frontEnd/create_group.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/group_page.dart';
import 'categories_home.dart';
import 'login_page.dart';
import 'models/group.dart';
import 'imports/globals.dart';
import 'log_out.dart';

class GroupsHome extends StatefulWidget {
  Future<List<Group>> groups;

  GroupsHome({Key key, this.groups}) : super(key: key);

  @override
  _GroupsHomeState createState() => new _GroupsHomeState();
}

class _GroupsHomeState extends State<GroupsHome> {
  @override
  void initState() {
    widget.groups = GroupsManager.getGroups();
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
      drawer: Drawer(
          child: ListView(
        padding: EdgeInsets.zero,
        children: <Widget>[
          Container(
            height: 80.0,
            decoration: BoxDecoration(
              color: Globals.secondaryColor,
            ),
            margin: EdgeInsets.zero,
            child: ListTile(
              contentPadding: EdgeInsets.fromLTRB(10, 25, 0, 0),
              leading: CircleAvatar(
                //TODO let the user set their own avatar (https://github.com/SCCapstone/decision_maker/issues/139)
                backgroundImage: AssetImage('assets/images/placeholder.jpg'),
              ),
              title: Text(Globals.username,
                  style: TextStyle(fontSize: 24, color: Colors.white)),
              onTap: () {
                //TODO direct the user to something like a profile settings page (https://github.com/SCCapstone/decision_maker/issues/140)
              },
            ),
          ),
          ListTile(
            leading: Icon(Icons.apps), // Placeholder icon
            title: Text('Categories', style: TextStyle(fontSize: 16)),
            onTap: () {
              Navigator.push(context,
                  MaterialPageRoute(builder: (context) => CategoriesHome()));
            },
          ),
          //TODO implement an app settings page and navigate to it from a new ListTile here (https://github.com/SCCapstone/decision_maker/issues/141)
          ListTile(
              leading: Icon(Icons.subdirectory_arrow_left),
              title: Text('Log out', style: TextStyle(fontSize: 16)),
              onTap: () {
                logOutUser();
                Navigator.of(context).pushReplacement(
                    new MaterialPageRoute(builder: (context) => LoginScreen()));
              })
        ],
      )),
      appBar: new AppBar(
        centerTitle: true,
        title: Text(
          "Pocket Poll",
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
            style: TextStyle(
                fontSize: DefaultTextStyle.of(context).style.fontSize * 0.5)),
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
      height: MediaQuery.of(context).size.height * .14,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          IconButton(
            iconSize: MediaQuery.of(context).size.width * .20,
            icon: Image(
              image: AssetImage('assets/images/placeholder.jpg'),
            ),
          ),
          Expanded(
            child: RaisedButton(
              child: Text(
                group.groupName,
                style: TextStyle(fontSize: 25),
              ),
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => GroupPage(group: this.group)),
                ).then((_) => GroupsHome());
              },
            ),
          )
        ],
      ),
    );
  }
}
