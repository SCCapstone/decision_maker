import 'dart:async';
import 'package:flutter/material.dart';
import 'package:frontEnd/groups_widgets/groups_create.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/groups_widgets//groups_list.dart';
import 'package:frontEnd/categories_widgets/categories_home.dart';
import 'package:frontEnd/login_page.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/models/app_settings.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/log_out.dart';

class GroupsHome extends StatefulWidget {
  Future<List<Group>> groupsFuture;
  Future<AppSettings> settingsFuture;

  GroupsHome({Key key, this.groupsFuture}) : super(key: key);

  @override
  _GroupsHomeState createState() => new _GroupsHomeState();
}

class _GroupsHomeState extends State<GroupsHome> {
  final TextEditingController searchBar = new TextEditingController();
  String searchInput = "";
  List<Group> displayedGroups = new List<Group>();
  List<Group> totalGroups = new List<Group>();
  AppSettings userSettings = new AppSettings();
  Icon searchIcon = new Icon(Icons.search);
  bool searching = false;

  @override
  void initState() {
    widget.groupsFuture = GroupsManager.getGroups();
    widget.settingsFuture = UsersManager.getUserAppSettings(context);
    searchBar.addListener(() {
      if (searchBar.text.isEmpty) {
        setState(() {
          searchInput = "";
          displayedGroups = totalGroups;
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
    // to catch any changes that may have been made when editing
    widget.groupsFuture = GroupsManager.getGroups();
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
                    leading: CircleAvatar(
                      //TODO let the user set their own avatar (https://github.com/SCCapstone/decision_maker/issues/139)
                      backgroundImage:
                      AssetImage('assets/images/placeholder.jpg'),
                    ),
                    title: Text(
                      Globals.username,
                      style: TextStyle(fontSize: 24, color: Colors.white),
                    ),
                    onTap: () {
                      //TODO direct the user to something like a profile settings page (https://github.com/SCCapstone/decision_maker/issues/140)
                    },
                  ),
                ),
              ),
            ),
            ListTile(
              key: new Key("blah"),
              leading: Icon(Icons.apps), // Placeholder icon
              title: Text('Categories', style: TextStyle(fontSize: 16)),
              onTap: () {
                Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (context) => CategoriesHome()));
              }),
            //TODO implement an app settings page and navigate to it from a new ListTile here (https://github.com/SCCapstone/decision_maker/issues/141)
            ListTile(
                leading: Icon(Icons.subdirectory_arrow_left),
                title: Text('Log out', style: TextStyle(fontSize: 16)),
                onTap: () {
                  logOutUser();
                  Navigator.of(context).pushReplacement(new MaterialPageRoute(
                      builder: (context) => LoginScreen()));
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
              width: MediaQuery
                  .of(context)
                  .size
                  .width * .70,
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
              EdgeInsets.all(MediaQuery
                  .of(context)
                  .size
                  .height * .015),
            ),
            Expanded(
              child: new Container(
                  width: MediaQuery
                      .of(context)
                      .size
                      .width * .80,
                  height: MediaQuery
                      .of(context)
                      .size
                      .height * .60,
                  child: FutureBuilder(
                    future: widget.groupsFuture,
                    builder: (BuildContext context, AsyncSnapshot snapshot) {
                      if (snapshot.hasData) {
                        totalGroups = snapshot.data;
                        displayedGroups = snapshot.data;
                        return buildListFuture();
                      } else if (snapshot.hasError) {
                        return Text("Error: ${snapshot.error}");
                      }
                      return Center(child: CircularProgressIndicator());
                    },
                  )),
            ),
            Padding(
              // used to make sure the group list doesn't go too far down, expanded widget stops when reaching this
              padding: EdgeInsets.all(MediaQuery
                  .of(context)
                  .size
                  .height * .08),
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

  Widget buildListFuture() {
    // TODO this entire flow needs to change. This is the cause of this issue (https://github.com/SCCapstone/decision_maker/issues/173)
    return FutureBuilder(
        future: widget.settingsFuture,
        builder: (BuildContext context, AsyncSnapshot snapshot) {
          if (snapshot.hasData) {
            userSettings = snapshot.data;
            return buildList(userSettings.groupSort);
          } else if (snapshot.hasError) {
            return Text("Error: ${snapshot.error}");
          } else {
            return buildList(
                0); // default value if the user settings for some reason doesn't have data
          }
        });
  }

  Widget buildList(int sortVal) {
    if (sortVal == 0) {
      displayedGroups = GroupsManager.sortByDate(displayedGroups);
    } else if (sortVal == 1) {
      displayedGroups = GroupsManager.sortByAlpha(displayedGroups);
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
    }
    return RefreshIndicator(
        onRefresh: refreshList,
        child: GroupsList(groups: displayedGroups, searching: searching));
  }

  Future<Null> refreshList() async {
    setState(() {
      widget.groupsFuture = GroupsManager.getGroups();
    });
  }

  void searchGroup() {
    if (searching) {
      searching = false;
      // already searching, so user has clicked the stop button
      setState(() {
        searchBar.clear();
        displayedGroups = totalGroups;
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
