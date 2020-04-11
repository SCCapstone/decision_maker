import 'dart:async';
import 'dart:io' show Platform;

import 'package:auto_size_text/auto_size_text.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:front_end_pocket_poll/about_widgets/about_page.dart';
import 'package:front_end_pocket_poll/about_widgets/user_manual.dart';
import 'package:front_end_pocket_poll/categories_widgets/categories_home.dart';
import 'package:front_end_pocket_poll/groups_widgets//groups_list.dart';
import 'package:front_end_pocket_poll/groups_widgets/groups_create.dart';
import 'package:front_end_pocket_poll/groups_widgets/groups_left_list.dart';
import 'package:front_end_pocket_poll/imports/events_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/login_page.dart';
import 'package:front_end_pocket_poll/models/group_left.dart';
import 'package:front_end_pocket_poll/models/message.dart';
import 'package:front_end_pocket_poll/models/user.dart';
import 'package:front_end_pocket_poll/models/user_group.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';

import '../user_settings.dart';
import 'group_page.dart';

class GroupsHome extends StatefulWidget {
  GroupsHome({Key key}) : super(key: key);

  @override
  _GroupsHomeState createState() => new _GroupsHomeState();
}

class _GroupsHomeState extends State<GroupsHome>
    with SingleTickerProviderStateMixin {
  final TextEditingController searchBarController = new TextEditingController();
  final int totalTabs = 2;
  final int groupsHomeTab = 0;
  final int groupsLeftTab = 1;

  FirebaseMessaging firebaseMessaging;
  TabController tabController;
  List<UserGroup> searchGroups;
  List<GroupLeft> searchGroupsLeft;
  List<UserGroup> totalGroups;
  List<GroupLeft> groupsLeft;
  Icon searchIcon;
  bool searching;
  int currentTab, groupHomeSortVal, groupsLeftSortVal;

  @override
  void initState() {
    this.searching = false;
    this.searchIcon = new Icon(Icons.search);
    this.searchGroups = new List<UserGroup>();
    this.searchGroupsLeft = new List<GroupLeft>();
    this.totalGroups = new List<UserGroup>();
    this.groupsLeft = new List<GroupLeft>();

    if (Platform.isAndroid) {
      // for now only have firebase messaging on android since ios requires license
      this.firebaseMessaging = FirebaseMessaging();
    }

    this.groupsLeftSortVal = Globals.alphabeticalSort;
    this.currentTab = this.groupsHomeTab;
    this.tabController = new TabController(length: this.totalTabs, vsync: this);
    this.tabController.addListener(handleTabChange);
    loadGroups();
    //region searchBar
    this.searchBarController.addListener(() {
      if (this.currentTab == this.groupsHomeTab) {
        // in the group home tab
        if (this.searchBarController.text.isEmpty) {
          setState(() {
            this.searchGroups.clear();
            this.searchGroups.addAll(this.totalGroups);
          });
        } else {
          // user has started to type so filter groups based on input
          setState(() {
            String searchInput = this.searchBarController.text;
            List<UserGroup> temp = new List<UserGroup>();
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
            this.searchGroupsLeft.clear();
            this.searchGroupsLeft.addAll(this.groupsLeft);
          });
        } else {
          // user has started to type so filter groups based on input
          setState(() {
            String searchInput = this.searchBarController.text;
            List<GroupLeft> temp = new List<GroupLeft>();
            for (int i = 0; i < this.groupsLeft.length; i++) {
              if (this
                  .groupsLeft[i]
                  .groupName
                  .toLowerCase()
                  .contains(searchInput.toLowerCase())) {
                temp.add(this.groupsLeft[i]);
              }
            }
            this.searchGroupsLeft = temp;
            GroupsManager.sortByAlphaAscending(this.searchGroups);
          });
        }
      }
    });
    //endregion
    // set up notification listener
    if (Platform.isAndroid) {
      Future<String> token = this.firebaseMessaging.getToken();
      UsersManager.registerPushEndpoint(token);
      if (!Globals.fireBaseConfigured) {
        this.firebaseMessaging.configure(
              onMessage: _onMessage,
              onLaunch: _onLaunch,
              onResume: _onResume,
            );
        Globals.fireBaseConfigured = true;
      }

      this.firebaseMessaging.requestNotificationPermissions(
          const IosNotificationSettings(sound: true, badge: true, alert: true));
    }

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
                            backgroundImage:
                                getUserIconImage(Globals.user.icon)),
                      ),
                      title: AutoSizeText(
                        Globals.username,
                        minFontSize: 12,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
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
                  key: Key("groups_home:my_categories_button"),
                  onTap: () {
                    // close the drawer menu when clicked
                    Navigator.of(context).pop();
                    Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (context) => CategoriesHome())).then((_) {
                      refreshList();
                    });
                  }),
              ListTile(
                  leading: Icon(Icons.settings),
                  title: Text('Settings', style: TextStyle(fontSize: 16)),
                  key: Key("groups_home:user_settings_button"),
                  onTap: () {
                    // close the drawer menu when clicked
                    Navigator.of(context).pop();
                    Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (context) => UserSettings())).then((_) {
                      refreshList();
                    });
                  }),
              ListTile(
                  leading: Icon(Icons.accessibility),
                  title: Text('User Manual', style: TextStyle(fontSize: 16)),
                  onTap: () {
                    // close the drawer menu when clicked
                    Navigator.of(context).pop();
                    Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (context) => UserManual())).then((_) {
                      refreshList();
                    });
                  }),
              ListTile(
                  leading: Icon(Icons.help_outline),
                  title: Text('About', style: TextStyle(fontSize: 16)),
                  onTap: () {
                    // close the drawer menu when clicked
                    Navigator.of(context).pop();
                    Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (context) => AboutPage())).then((_) {
                      refreshList();
                    });
                  }),
              Expanded(
                child: Align(
                  alignment: Alignment.bottomCenter,
                  child: ListTile(
                      leading: Icon(Icons.exit_to_app),
                      title: Text('Log Out', style: TextStyle(fontSize: 16)),
                      onTap: () {
                        logOutUser(context);
                        if (Platform.isAndroid) {
                          Globals.fireBaseConfigured = false;
                          // not 100% sure the below does what i think it does, i think it resets the firebaseMessaging
                          firebaseMessaging.deleteInstanceID();
                        }
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
            child: AutoSizeText(
              "Pocket Poll",
              maxLines: 1,
              minFontSize: 20,
              style: TextStyle(fontSize: 40),
            ),
          ),
          actions: <Widget>[
            WillPopScope(
              onWillPop: handleBackPress,
              child: Visibility(
                // shows a search bar once the user clicks the search icon
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
                this.searchGroupsLeft.clear();
                if (this.tabController.index == this.groupsHomeTab) {
                  this.searchGroups.addAll(this.totalGroups);
                } else {
                  this.searchGroupsLeft.addAll(this.groupsLeft);
                }
                toggleSearch();
              },
            )
          ],
          bottom: PreferredSize(
            // used to display the tabs and the sort icon
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
                    child: PopupMenuButton<int>(
                        child: Icon(
                          Icons.sort,
                          size: MediaQuery.of(context).size.height * .04,
                          color: Colors.black,
                        ),
                        tooltip: "Sort Groups",
                        onSelected: (int result) {
                          if (this.currentTab == this.groupsHomeTab) {
                            if (this.groupHomeSortVal != result) {
                              // prevents useless updates if sort didn't change
                              this.groupHomeSortVal = result;
                              setState(() {
                                setGroupsHomeSort(true);
                              });
                            }
                          } else {
                            if (this.groupsLeftSortVal != result) {
                              // prevents useless updates if sort didn't change
                              this.groupsLeftSortVal = result;
                              setState(() {
                                setGroupsLeftSort();
                              });
                            }
                          }
                        },
                        itemBuilder: (BuildContext context) {
                          if (this.currentTab == this.groupsLeftTab) {
                            // if the current tab is groups left, don't show sort by date
                            return <PopupMenuEntry<int>>[
                              PopupMenuItem<int>(
                                value: Globals.alphabeticalSort,
                                child: Text(
                                  Globals.alphabeticalSortString,
                                  style: TextStyle(
                                      // if it is selected, underline it
                                      decoration: (this.groupsLeftSortVal ==
                                              Globals.alphabeticalSort)
                                          ? TextDecoration.underline
                                          : null),
                                ),
                              ),
                              PopupMenuItem<int>(
                                value: Globals.alphabeticalReverseSort,
                                child: Text(
                                    Globals.alphabeticalReverseSortString,
                                    style: TextStyle(
                                        // if it is selected, underline it
                                        decoration: (this.groupsLeftSortVal ==
                                                Globals.alphabeticalReverseSort)
                                            ? TextDecoration.underline
                                            : null)),
                              ),
                            ];
                          } else {
                            return <PopupMenuEntry<int>>[
                              // group home tab, so show the sort by dates
                              PopupMenuItem<int>(
                                value: Globals.dateNewestSort,
                                child: Text(
                                  Globals.dateNewestSortString,
                                  style: TextStyle(
                                      // if it is selected, underline it
                                      decoration: (this.groupHomeSortVal ==
                                              Globals.dateNewestSort)
                                          ? TextDecoration.underline
                                          : null),
                                ),
                              ),
                              PopupMenuItem<int>(
                                value: Globals.dateOldestSort,
                                child: Text(
                                  Globals.dateOldestSortString,
                                  style: TextStyle(
                                      // if it is selected, underline it
                                      decoration: (this.groupHomeSortVal ==
                                              Globals.dateOldestSort)
                                          ? TextDecoration.underline
                                          : null),
                                ),
                              ),
                              PopupMenuItem<int>(
                                value: Globals.alphabeticalSort,
                                child: Text(
                                  Globals.alphabeticalSortString,
                                  style: TextStyle(
                                      // if it is selected, underline it
                                      decoration: (this.groupHomeSortVal ==
                                              Globals.alphabeticalSort)
                                          ? TextDecoration.underline
                                          : null),
                                ),
                              ),
                              PopupMenuItem<int>(
                                value: Globals.alphabeticalReverseSort,
                                child: Text(
                                  Globals.alphabeticalReverseSortString,
                                  style: TextStyle(
                                      // if it is selected, underline it
                                      decoration: (this.groupHomeSortVal ==
                                              Globals.alphabeticalReverseSort)
                                          ? TextDecoration.underline
                                          : null),
                                ),
                              ),
                            ];
                          }
                        }),
                  )
                ],
              ),
            ),
          ),
        ),
        key: Key("groups_home:scaffold"),
        body: TabBarView(
          controller: this.tabController,
          children: <Widget>[
            Center(
              // tab for groups home
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
            Center(
              // tab for groups left
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
                                  ? this.searchGroupsLeft
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
          visible: (!this.searching && this.currentTab == this.groupsHomeTab),
          child: FloatingActionButton(
            child: Icon(Icons.add),
            key: Key("groups_home:new_group_button"),
            onPressed: () {
              // Navigate to second route when tapped.
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => CreateGroup()),
              ).then((val) {
                loadGroups();
              });
            },
          ),
        ),
      ),
    );
  }

  // whenever the tab changes make sure to save current tab index
  void handleTabChange() {
    setState(() {
      this.currentTab = this.tabController.index;
    });
  }

  void setGroupsHomeSort(bool sendUpdate) {
    if (this.groupHomeSortVal == Globals.dateNewestSort) {
      GroupsManager.sortByDateNewest(this.totalGroups);
    } else if (this.groupHomeSortVal == Globals.dateOldestSort) {
      GroupsManager.sortByDateOldest(totalGroups);
    } else if (this.groupHomeSortVal == Globals.alphabeticalReverseSort) {
      GroupsManager.sortByAlphaDescending(this.totalGroups);
    } else if (this.groupHomeSortVal == Globals.alphabeticalSort) {
      GroupsManager.sortByAlphaAscending(this.totalGroups);
    }
    if (sendUpdate) {
      // blind send, don't care if it doesn't work since it's just a sort value
      UsersManager.updateSortSetting(
          UsersManager.APP_SETTINGS_GROUP_SORT, this.groupHomeSortVal);
      Globals.user.appSettings.groupSort = this.groupHomeSortVal;
    }
  }

  void setGroupsLeftSort() {
    // don't need to update DB, separate method so there isn't a break in consistency
    if (this.groupsLeftSortVal == Globals.alphabeticalReverseSort) {
      GroupsManager.sortByAlphaDescending(this.groupsLeft);
    } else if (this.groupsLeftSortVal == Globals.alphabeticalSort) {
      GroupsManager.sortByAlphaAscending(this.groupsLeft);
    }
  }

  /*
    Attempts to refresh the list using a call to the DB for user object
   */
  Future<Null> refreshList() async {
    ResultStatus<User> resultStatus = await UsersManager.getUserData();
    if (resultStatus.success) {
      Globals.user = resultStatus.data;
      setState(() {
        loadGroups();
      });
    } else {
      showErrorMessage("Error", resultStatus.errorMessage, this.context);
    }
  }

  // loads all groups and groups left from the local user object
  void loadGroups() {
    this.totalGroups = Globals.user.groups.values.toList();
    this.groupsLeft = Globals.user.groupsLeft.values.toList();
    this.groupHomeSortVal = Globals.user.appSettings.groupSort;
    setGroupsHomeSort(false);
    setGroupsLeftSort();
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

  /*
    Allows android users to press back button and have useful shortcuts
   */
  Future<bool> handleBackPress() async {
    if (this.searching) {
      // when done searching remove the search bar
      toggleSearch();
      return false;
    } else if (this.currentTab != 0) {
      // allows the back button to switch tabs
      this
          .tabController
          .animateTo((this.tabController.index + 1) % this.totalTabs);
      return false;
    } else {
      return true;
    }
  }

  // region FirebaseMessaging Methods

  // this method is activated whenever the user receives a notification with the app in the foreground
  Future<void> _onMessage(Map<String, dynamic> notificationRaw) async {
    try {
      final Message notification = Message.fromJSON(notificationRaw);

      if (notification.title != null && notification.body != null) {
        Fluttertoast.showToast(
            msg: "${notification.title}\n${notification.body}",
            toastLength: Toast.LENGTH_LONG,
            gravity: ToastGravity.CENTER);
      }

      if (notification.action == Globals.removedFromGroupAction) {
        String groupId = notification.payload[GroupsManager.GROUP_ID];
        if (Globals.currentGroup == null) {
          // this avoids the current page flashing for a second, don't need to pop if already here
          refreshList();
        } else if (Globals.currentGroup.groupId == groupId) {
          // somewhere in the app the user is in the group they were kicked out of, so bring them back to the home apge
          Globals.user.groups.remove(Globals.currentGroup.groupId);
          Navigator.of(context).popUntil((route) => route.isFirst);
        }
      } else if (notification.action == Globals.addedToGroupAction) {
        if (ModalRoute.of(context).isCurrent) {
          // only refresh if this widget is visible
          refreshList();
        }
      } else {
        // event updates
        String groupId = notification.payload[GroupsManager.GROUP_ID];
        String eventId = notification.payload[EventsManager.EVENT_ID];
        if (Globals.user.groups[groupId] != null) {
          Globals.user.groups[groupId].eventsUnseen
              .putIfAbsent(eventId, () => true);
        }
        if (Globals.refreshGroupPage != null) {
          // the refresh callback has been properly set, so refresh the current global group
          Globals.refreshGroupPage();
        }
        if (ModalRoute.of(context).isCurrent) {
          // only update groups home if it actually visible
          loadGroups();
          setState(() {});
        }
      }
    } catch (e) {
      //do nothing
    }
    return;
  }

  // this method is called when user clicks on a notification when the app has been terminated
  Future<void> _onLaunch(Map<String, dynamic> notificationRaw) async {
    try {
      final Message notification = Message.fromJSON(notificationRaw);

      final List<String> actionsThatOpenGroupPage = [
        Globals.addedToGroupAction,
        Globals.eventCreatedAction,
        Globals.eventVotingAction,
        Globals.eventChosenAction
      ];

      if (actionsThatOpenGroupPage.contains(notification.action)) {
        // take the user straight to the group they were added to if they click the notification
        Navigator.push(
          context,
          MaterialPageRoute(
              builder: (context) => GroupPage(
                    groupId: notification.payload[GroupsManager.GROUP_ID],
                    groupName: notification.payload[GroupsManager.GROUP_NAME],
                  )),
        ).then((val) {
          refreshList();
        });
      }
    } catch (e) {
      //do nothing
    }
    return;
  }

  /*
    This is called when the user clicks on a notification when the app is in the background.
    
    Currently not implemented because it is very involved to make processes run correctly in the background
   */
  Future<void> _onResume(Map<String, dynamic> message) async {
    return;
  }
// endregion
}
