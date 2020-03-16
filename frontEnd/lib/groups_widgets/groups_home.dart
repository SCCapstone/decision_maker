import 'dart:async';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:frontEnd/categories_widgets/categories_home.dart';
import 'package:frontEnd/groups_widgets//groups_list.dart';
import 'package:frontEnd/groups_widgets/groups_create.dart';
import 'package:frontEnd/groups_widgets/groups_left_list.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/login_page.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/models/user.dart';
import 'package:frontEnd/utilities/utilities.dart';

import '../user_settings.dart';

class GroupsHome extends StatefulWidget {
  GroupsHome({Key key}) : super(key: key);

  @override
  _GroupsHomeState createState() => new _GroupsHomeState();
}

class _GroupsHomeState extends State<GroupsHome>
    with SingleTickerProviderStateMixin {
  final TextEditingController searchBarController = new TextEditingController();
  TabController tabController;
  List<Group> searchGroups = new List<Group>();
  List<Group> totalGroups = new List<Group>();
  List<Group> groupsLeft = new List<Group>();
  Icon searchIcon = new Icon(Icons.search);
  bool searching = false;
  int currentTab, sortVal;
  final int totalTabs = 2;

  final FirebaseMessaging firebaseMessaging = FirebaseMessaging();

  @override
  void initState() {
    this.currentTab = 0;
    this.tabController = new TabController(length: this.totalTabs, vsync: this);
    this.tabController.addListener(handleTabChange);
    loadGroups();
    this.searchBarController.addListener(() {
      if (this.currentTab == 0) {
        // in the group home tab
        if (this.searchBarController.text.isEmpty) {
          setState(() {
            this.searchGroups.clear();
            this.searchGroups.addAll(this.totalGroups);
          });
        } else {
          setState(() {
            String searchInput = this.searchBarController.text;
            List<Group> temp = new List<Group>();
            for (int i = 0; i < this.totalGroups.length; i++) {
              if (this
                  .totalGroups[i]
                  .groupName
                  .toLowerCase()
                  .contains(searchInput.toLowerCase())) {
                temp.add(this.totalGroups[i]);
              }
            }
            this.searchGroups = temp;
            GroupsManager.sortByAlphaAscending(this.searchGroups);
          });
        }
      } else {
        // in the groups left tab
        if (this.searchBarController.text.isEmpty) {
          setState(() {
            this.searchGroups.clear();
            this.searchGroups.addAll(this.groupsLeft);
          });
        } else {
          setState(() {
            String searchInput = this.searchBarController.text;
            List<Group> temp = new List<Group>();
            for (int i = 0; i < this.groupsLeft.length; i++) {
              if (this
                  .groupsLeft[i]
                  .groupName
                  .toLowerCase()
                  .contains(searchInput.toLowerCase())) {
                temp.add(this.groupsLeft[i]);
              }
            }
            this.searchGroups = temp;
            GroupsManager.sortByAlphaAscending(this.searchGroups);
          });
        }
      }
    });
    // set up notification listeners
    Future<String> token = this.firebaseMessaging.getToken();
    UsersManager.registerPushEndpoint(token);

    this.firebaseMessaging.configure(
        onMessage: (Map<String, dynamic> message) async {
      final data = message['data'];
      showErrorMessage("Notice", data['default'], context);
      refreshList();
    }, onLaunch: (Map<String, dynamic> message) async {
      print("onLaunch: $message");
    }, onResume: (Map<String, dynamic> message) async {
      print("onResume: $message");
    });
    this.firebaseMessaging.requestNotificationPermissions(
        const IosNotificationSettings(sound: true, badge: true, alert: true));

    super.initState();
  }

  @override
  void dispose() {
    tabController.dispose();
    searchBarController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: this.totalTabs,
      child: Scaffold(
        drawer: Drawer(
          child: Column(
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
                          // close the drawer menu when clicked
                          Navigator.of(context).pop();
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
                        // close the drawer menu when clicked
                        Navigator.of(context).pop();
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
                  title: Text('My Categories', style: TextStyle(fontSize: 16)),
                  onTap: () {
                    // close the drawer menu when clicked
                    Navigator.of(context).pop();
                    Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (context) => CategoriesHome()));
                  }),
              ListTile(
                  leading: Icon(Icons.settings),
                  title: Text('Settings', style: TextStyle(fontSize: 16)),
                  onTap: () {
                    // close the drawer menu when clicked
                    Navigator.of(context).pop();
                    Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (context) => UserSettings()));
                  }),
              Expanded(
                child: Align(
                  alignment: Alignment.bottomCenter,
                  child: ListTile(
                      leading: Icon(Icons.exit_to_app),
                      title: Text('Log Out', style: TextStyle(fontSize: 16)),
                      onTap: () {
                        logOutUser(context);
                        Navigator.pushAndRemoveUntil(
                            context,
                            MaterialPageRoute(
                              builder: (BuildContext context) => SignInPage(),
                            ),
                            ModalRoute.withName('/'));
                      }),
                ),
              )
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
                visible: this.searching,
                child: Container(
                  width: MediaQuery.of(context).size.width * .70,
                  child: TextField(
                    autofocus: true,
                    controller: this.searchBarController,
                    style: TextStyle(color: Colors.white, fontSize: 30),
                    decoration: InputDecoration(
                        border: InputBorder.none,
                        hintText: "Search Group",
                        hintStyle: TextStyle(
                            color: Colors.white, fontStyle: FontStyle.italic)),
                  ),
                ),
              ),
            ),
            IconButton(
              icon: this.searchIcon,
              iconSize: 40,
              onPressed: () {
                // prevents quick flash of rows that appears when clicking search icon
                this.searchGroups.clear();
                if (this.tabController.index == 0) {
                  this.searchGroups.addAll(this.totalGroups);
                } else {
                  this.searchGroups.addAll(this.groupsLeft);
                }
                toggleSearch();
              },
            )
          ],
          bottom: PreferredSize(
            preferredSize: Size(MediaQuery.of(context).size.width,
                MediaQuery.of(context).size.height * .045),
            child: Visibility(
              visible: !this.searching,
              child: Row(
                children: <Widget>[
                  Container(
                    width: MediaQuery.of(context).size.width * .70,
                    child: TabBar(
                      controller: this.tabController,
                      isScrollable: false,
                      indicatorWeight: 3,
                      indicatorColor: Colors.blueAccent,
                      tabs: <Widget>[
                        AutoSizeText(
                          "Groups Home",
                          maxLines: 1,
                          style: TextStyle(fontSize: 17),
                          minFontSize: 12,
                          overflow: TextOverflow.ellipsis,
                        ),
                        AutoSizeText(
                          "Groups Left",
                          maxLines: 1,
                          style: TextStyle(fontSize: 17),
                          minFontSize: 12,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  Padding(
                    padding: EdgeInsets.fromLTRB(
                        MediaQuery.of(context).size.width * .095,
                        0,
                        MediaQuery.of(context).size.width * .095,
                        0),
                  ),
                  Container(
                    height: MediaQuery.of(context).size.height * .04,
                    child: Visibility(
                      visible: this.currentTab == 0,
                      child: PopupMenuButton<int>(
                        child: Icon(
                          Icons.sort,
                          size: MediaQuery.of(context).size.height * .04,
                          color: Colors.black,
                        ),
                        tooltip: "Sort Groups",
                        onSelected: (int result) {
                          if (this.sortVal != result) {
                            // prevents useless updates if sort didn't change
                            this.sortVal = result;
                            setState(() {
                              setSort(true);
                            });
                          }
                        },
                        itemBuilder: (BuildContext context) =>
                            <PopupMenuEntry<int>>[
                          PopupMenuItem<int>(
                            value: Globals.dateNewestSort,
                            child: Text(Globals.dateNewestSortString),
                          ),
                          PopupMenuItem<int>(
                            value: Globals.dateOldestSort,
                            child: Text(Globals.dateOldestSortString),
                          ),
                          PopupMenuItem<int>(
                            value: Globals.alphabeticalSort,
                            child: Text(Globals.alphabeticalSortString),
                          ),
                          PopupMenuItem<int>(
                            value: Globals.alphabeticalReverseSort,
                            child: Text(Globals.alphabeticalReverseSortString),
                          ),
                        ],
                      ),
                    ),
                  )
                ],
              ),
            ),
          ),
        ),
        body: TabBarView(
          controller: this.tabController,
          children: <Widget>[
            // tab for groups home
            Center(
              child: Column(
                children: <Widget>[
                  Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .015),
                  ),
                  Expanded(
                    child: Container(
                        width: MediaQuery.of(context).size.width * .95,
                        child: RefreshIndicator(
                            onRefresh: refreshList,
                            child: GroupsList(
                              groups: (this.searching)
                                  ? this.searchGroups
                                  : this.totalGroups,
                              searching: this.searching,
                              refreshGroups: refreshList,
                            ))),
                  ),
                  Padding(
                    // used to make sure the group list doesn't go too far down, expanded widget stops when reaching this
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .02),
                  ),
                ],
              ),
            ),
            // tab for groups left
            Center(
              child: Column(
                children: <Widget>[
                  Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .015),
                  ),
                  Expanded(
                    child: Container(
                        width: MediaQuery.of(context).size.width * .95,
                        child: RefreshIndicator(
                            onRefresh: refreshList,
                            child: GroupsLeftList(
                              groupsLeft: (this.searching)
                                  ? this.searchGroups
                                  : this.groupsLeft,
                              searching: this.searching,
                              refreshGroups: refreshList,
                            ))),
                  ),
                  Padding(
                    // used to make sure the group list doesn't go too far down, expanded widget stops when reaching this
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .02),
                  ),
                ],
              ),
            ),
          ],
        ),
        floatingActionButton: Visibility(
          visible: (!this.searching && this.currentTab == 0),
          child: FloatingActionButton(
            child: Icon(Icons.add),
            onPressed: () {
              // Navigate to second route when tapped.
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => CreateGroup()),
              ).then((val) {
                // TODO figure out a better way to refresh without making unnecessary API calls
                refreshList();
              });
            },
          ),
        ),
      ),
    );
  }

  void handleTabChange() {
    setState(() {
      this.currentTab = this.tabController.index;
    });
  }

  void setSort(bool sendUpdate) {
    if (sortVal == Globals.dateNewestSort) {
      GroupsManager.sortByDateNewest(totalGroups);
    } else if (sortVal == Globals.dateOldestSort) {
      GroupsManager.sortByDateOldest(totalGroups);
    } else if (sortVal == Globals.alphabeticalReverseSort) {
      GroupsManager.sortByAlphaDescending(totalGroups);
    } else if (sortVal == Globals.alphabeticalSort) {
      GroupsManager.sortByAlphaAscending(totalGroups);
    }
    if (sendUpdate) {
      // blind send, don't care if it doesn't work since its just a sort value
      // TODO make a backend method to accept specific settings since it is very verbose using current one
    }
  }

  Future<Null> refreshList() async {
    ResultStatus<User> resultStatus = await UsersManager.getUserData();
    if (resultStatus.success) {
      Globals.user = resultStatus.data;
      setState(() {
        loadGroups();
      });
    } else {
      showErrorMessage("Error", resultStatus.errorMessage, context);
    }
  }

  void loadGroups() {
    this.totalGroups.clear();
    this.groupsLeft.clear();
    for (String groupId in Globals.user.groups.keys) {
      Group group = new Group(
          groupId: groupId,
          groupName: Globals.user.groups[groupId][GroupsManager.GROUP_NAME],
          icon: Globals.user.groups[groupId][GroupsManager.ICON],
          lastActivity: Globals.user.groups[groupId]
              [GroupsManager.LAST_ACTIVITY]);
      this.totalGroups.add(group);
    }
    // get the groups left from the user object
    for (String groupId in Globals.user.groupsLeft.keys) {
      Group group = new Group(
          groupId: groupId,
          groupName: Globals.user.groupsLeft[groupId][GroupsManager.GROUP_NAME],
          icon: Globals.user.groupsLeft[groupId][GroupsManager.ICON]);
      this.groupsLeft.add(group);
    }
    this.sortVal = Globals.user.appSettings.groupSort;
    setSort(false);
  }

  void toggleSearch() {
    if (this.searching) {
      // already searching, so user has clicked the stop button
      setState(() {
        this.searching = false;
        this.searchBarController.clear();
        this.searchIcon = new Icon(Icons.search);
      });
    } else {
      setState(() {
        this.searching = true;
        this.searchIcon = new Icon(Icons.close);
      });
    }
  }

  Future<bool> handleBackPress() async {
    // this allows for android users to press the back button when done searching and it will remove the search bar
    if (this.searching) {
      toggleSearch();
      return false;
    } else {
      return true;
    }
  }
}
