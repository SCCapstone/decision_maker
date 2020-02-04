import 'dart:async';

import 'package:flutter/material.dart';
import 'package:frontEnd/categories_widgets/categories_home.dart';
import 'package:frontEnd/groups_widgets//groups_list.dart';
import 'package:frontEnd/groups_widgets/groups_create.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/log_out.dart';
import 'package:frontEnd/login_page.dart';
import 'package:frontEnd/models/group.dart';

import '../user_settings.dart';

class GroupsHome extends StatefulWidget {
  Future<List<Group>> groupsFuture;

  GroupsHome({Key key, this.groupsFuture}) : super(key: key);

  @override
  _GroupsHomeState createState() => new _GroupsHomeState();
}

class _GroupsHomeState extends State<GroupsHome> {
  final TextEditingController searchBar = new TextEditingController();
  String searchInput = "";
  List<Group> displayedGroups = new List<Group>();
  Icon searchIcon = new Icon(Icons.search);
  bool searching = false;

  @override
  void initState() {
    widget.groupsFuture = GroupsManager.getGroups();
    searchBar.addListener(() {
      if (searchBar.text.isEmpty) {
        setState(() {
          searchInput = "";
          displayedGroups = Globals.groups;
        });
      } else {
        setState(() {
          searchInput = searchBar.text;
        });
      }
    });
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      drawer: Drawer(
        child: ListView(
          padding: EdgeInsets.zero,
          children: <Widget>[
            Container(
              decoration: BoxDecoration(color: Globals.secondaryColor),
              child: SafeArea(
                // for phones that have a notch, use a safe area so content isn't obstructed
                child: Container(
                  height: 80.0,
                  decoration: BoxDecoration(color: Globals.secondaryColor),
                  margin: EdgeInsets.zero,
                  child: ListTile(
                    contentPadding: EdgeInsets.fromLTRB(10, 25, 0, 0),
                    leading: GestureDetector(
                      onTap: () {
                        Navigator.push(
                            context,
                            MaterialPageRoute(
                                builder: (context) => UserSettings()));
                      },
                      child: CircleAvatar(
                        backgroundImage:
                            AssetImage('assets/images/placeholder.jpg'),
                      ),
                    ),
                    title: Text(
                      Globals.username,
                      style: TextStyle(fontSize: 24, color: Colors.white),
                    ),
                    onTap: () {
                      Navigator.push(
                          context,
                          MaterialPageRoute(
                              builder: (context) => UserSettings()));
                    },
                  ),
                ),
              ),
            ),
            ListTile(
                leading: Icon(Icons.apps), // Placeholder icon
                title: Text('Categories', style: TextStyle(fontSize: 16)),
                onTap: () {
                  Navigator.push(
                      context,
                      MaterialPageRoute(
                          builder: (context) => CategoriesHome()));
                }),
            ListTile(
                leading: Icon(Icons.subdirectory_arrow_left),
                title: Text('Log out', style: TextStyle(fontSize: 16)),
                onTap: () {
                  logOutUser();
                  Navigator.pushAndRemoveUntil(
                      context,
                      MaterialPageRoute(
                        builder: (BuildContext context) => SignInPage(),
                      ),
                      ModalRoute.withName('/'));
                })
          ],
        ),
      ),
      appBar: AppBar(
        centerTitle: true,
        title: Visibility(
          visible: !(searching),
          child: Text(
            "Pocket Poll",
            style: TextStyle(fontSize: 35),
          ),
        ),
        actions: <Widget>[
          Visibility(
            visible: searching,
            child: Container(
              width: MediaQuery.of(context).size.width * .70,
              child: TextFormField(
                controller: searchBar,
                style: TextStyle(color: Colors.white, fontSize: 30),
                decoration: InputDecoration(
                    hintText: "Search Group",
                    hintStyle: TextStyle(
                        color: Colors.white, fontStyle: FontStyle.italic)),
              ),
            ),
          ),
          IconButton(
            icon: searchIcon,
            iconSize: 40,
            onPressed: () {
              searchGroup();
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
            Visibility(
              visible: Globals.groups == null,
              child: Expanded(
                child: Container(
                    width: MediaQuery.of(context).size.width * .80,
                    height: MediaQuery.of(context).size.height * .60,
                    child: FutureBuilder(
                      future: widget.groupsFuture,
                      builder: (BuildContext context, AsyncSnapshot snapshot) {
                        if (snapshot.hasData) {
                          Globals.groups = snapshot.data;
                          return buildList(Globals.user.appSettings.groupSort);
                        } else if (snapshot.hasError) {
                          return Text("Error: ${snapshot.error}");
                        }
                        return Center(child: CircularProgressIndicator());
                      },
                    )),
              ),
            ),
            Visibility(
              visible: Globals.groups != null,
              child: Expanded(
                  child: Container(
                      width: MediaQuery.of(context).size.width * .80,
                      height: MediaQuery.of(context).size.height * .60,
                      child: buildList(Globals.user.appSettings.groupSort))),
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

  Widget buildList(int sortVal) {
    if (Globals.groups == null) {
      // this is a little hacky, but otherwise the visibility widget will call this and get a
      // null pointer exception the first time the app is loaded. This text should never be seen
      return Text("Error loading groups.");
    }
    if (searchInput.isNotEmpty) {
      List<Group> temp = new List<Group>();
      for (int i = 0; i < displayedGroups.length; i++) {
        if (displayedGroups[i]
            .groupName
            .toLowerCase()
            .contains(searchInput.toLowerCase())) {
          temp.add(displayedGroups[i]);
        }
      }
      displayedGroups = temp;
    } else {
      displayedGroups = Globals.groups;
    }
    if (sortVal == Globals.dateSort) {
      displayedGroups = GroupsManager.sortByDate(displayedGroups);
    } else if (sortVal == Globals.alphabeticalSort) {
      displayedGroups = GroupsManager.sortByAlpha(displayedGroups);
    }
    return RefreshIndicator(
        onRefresh: refreshList,
        child: GroupsList(groups: displayedGroups, searching: searching));
  }

  Future<Null> refreshList() async {
    Globals.groups = await GroupsManager.getGroups();
    setState(() {});
  }

  void searchGroup() {
    if (searching) {
      searching = false;
      // already searching, so user has clicked the stop button
      setState(() {
        searchBar.clear();
        displayedGroups = Globals.groups;
        searchIcon = new Icon(Icons.search);
      });
    } else {
      searching = true;
      setState(() {
        searchIcon = new Icon(Icons.close);
      });
    }
  }
}
