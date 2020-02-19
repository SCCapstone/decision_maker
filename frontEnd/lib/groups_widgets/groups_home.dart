import 'dart:async';

import 'package:flutter/material.dart';
import 'package:frontEnd/categories_widgets/categories_home.dart';
import 'package:frontEnd/groups_widgets//groups_list.dart';
import 'package:frontEnd/groups_widgets/groups_create.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/log_out.dart';
import 'package:frontEnd/login_page.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/utilities/utilities.dart';

import '../user_settings.dart';

class GroupsHome extends StatefulWidget {
  GroupsHome({Key key}) : super(key: key);

  @override
  _GroupsHomeState createState() => new _GroupsHomeState();
}

class _GroupsHomeState extends State<GroupsHome> {
  final TextEditingController searchBar = new TextEditingController();
  String searchInput = "";
  List<Group> searchGroups = new List<Group>();
  List<Group> totalGroups = new List<Group>();
  Icon searchIcon = new Icon(Icons.search);
  bool searching = false;

  @override
  void initState() {
    loadGroups();
    searchBar.addListener(() {
      if (searchBar.text.isEmpty) {
        setState(() {
          searchGroups.clear();
          searchGroups.addAll(totalGroups);
        });
      } else {
        setState(() {
          searchInput = searchBar.text;
          List<Group> temp = new List<Group>();
          for (int i = 0; i < totalGroups.length; i++) {
            if (totalGroups[i]
                .groupName
                .toLowerCase()
                .contains(searchInput.toLowerCase())) {
              temp.add(totalGroups[i]);
            }
          }
          searchGroups = temp;
          GroupsManager.sortByAlpha(searchGroups);
        });
      }
    });
    super.initState();
  }

  @override
  void dispose() {
    searchBar.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      drawer: Drawer(
        child: ListView(
          padding: EdgeInsets.zero,
          children: <Widget>[
            Container(
              decoration: BoxDecoration(color: Theme.of(context).accentColor),
              child: SafeArea(
                // for phones that have a notch, use a safe area so content isn't obstructed
                child: Container(
                  height: 80.0,
                  decoration:
                      BoxDecoration(color: Theme.of(context).accentColor),
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
                          backgroundImage: getUserIconUrl(Globals.user)),
                    ),
                    title: Text(
                      Globals.username,
                      style: TextStyle(
                          fontSize: 24,
                          color: Theme.of(context).primaryColorDark),
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
                leading: Icon(Icons.format_list_bulleted),
                title: Text('Categories', style: TextStyle(fontSize: 16)),
                onTap: () {
                  Navigator.push(
                      context,
                      MaterialPageRoute(
                          builder: (context) => CategoriesHome()));
                }),
            ListTile(
                leading: Icon(Icons.exit_to_app),
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
            style: TextStyle(
                fontSize: DefaultTextStyle.of(context).style.fontSize * 0.8),
          ),
        ),
        actions: <Widget>[
          WillPopScope(
            onWillPop: handleBackPress,
            child: Visibility(
              visible: searching,
              child: Container(
                width: MediaQuery.of(context).size.width * .70,
                child: TextField(
                  autofocus: true,
                  controller: searchBar,
                  style: TextStyle(color: Colors.white, fontSize: 30),
                  decoration: InputDecoration(
                      hintText: "Search Group",
                      hintStyle: TextStyle(
                          color: Colors.white, fontStyle: FontStyle.italic)),
                ),
              ),
            ),
          ),
          IconButton(
            icon: searchIcon,
            iconSize: 40,
            onPressed: () {
              searchGroups.clear();
              searchGroups.addAll(totalGroups);
              toggleSearch();
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
              child: Container(
                  width: MediaQuery.of(context).size.width * .90,
                  height: MediaQuery.of(context).size.height * .60,
                  child: RefreshIndicator(
                      onRefresh: refreshList,
                      child: GroupsList(
                          groups: (searching) ? searchGroups : totalGroups,
                          searching: searching))),
            ),
            Padding(
              // used to make sure the group list doesn't go too far down, expanded widget stops when reaching this
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .015),
            ),
          ],
        ),
      ),
      floatingActionButton: Visibility(
        visible: !searching,
        child: FloatingActionButton(
          child: Icon(Icons.add),
          onPressed: () {
            // Navigate to second route when tapped.
            Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => CreateGroup()),
            ).then((_) => GroupsHome());
          },
        ),
      ),
    );
  }

  Future<Null> refreshList() async {
    Globals.user = await UsersManager.getUserData();
    setState(() {
      loadGroups();
    });
  }

  void loadGroups() {
    for (String groupId in Globals.user.groups.keys) {
      Group group = new Group(
          groupId: groupId,
          groupName: Globals.user.groups[groupId][GroupsManager.GROUP_NAME],
          icon: Globals.user.groups[groupId][GroupsManager.ICON]);
      if (!totalGroups.contains(group)) {
        totalGroups.add(group);
      }
    }
    // TODO re-enable once group objects in user have the last modified date
//    if (sortVal == Globals.dateSort) {
//      displayedGroups = GroupsManager.sortByDate(displayedGroups);
//    } else if (sortVal == Globals.alphabeticalSort) {
//      displayedGroups = GroupsManager.sortByAlpha(displayedGroups);
//    }
    GroupsManager.sortByAlpha(totalGroups);
  }

  void toggleSearch() {
    if (searching) {
      // already searching, so user has clicked the stop button
      setState(() {
        searching = false;
        searchBar.clear();
        searchIcon = new Icon(Icons.search);
      });
    } else {
      setState(() {
        searching = true;
        searchIcon = new Icon(Icons.close);
      });
    }
  }

  Future<bool> handleBackPress() async {
    // this allows for android users to press the back button when done searching and it will remove the search bar
    if (searching) {
      toggleSearch();
      return false;
    } else {
      return true;
    }
  }
}
