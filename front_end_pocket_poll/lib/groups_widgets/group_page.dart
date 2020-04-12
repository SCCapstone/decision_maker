import 'dart:async';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/group.dart';
import 'group_settings.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/events_widgets/events_list.dart';

import 'package:front_end_pocket_poll/events_widgets/event_create.dart';

class GroupPage extends StatefulWidget {
  final String groupId;
  final String groupName;

  GroupPage({Key key, this.groupId, this.groupName}) : super(key: key);

  @override
  _GroupPageState createState() => new _GroupPageState();
}

class _GroupPageState extends State<GroupPage> {
  bool loading;
  bool errorLoading;
  Widget errorWidget;

  @override
  void initState() {
    this.loading = true;
    this.errorLoading = false;
    // allows this page to be refreshed if a new notification comes in for the group
    Globals.refreshGroupPage = refreshList;
    getGroup();
    super.initState();
  }

  @override
  void dispose() {
    Globals.currentGroup = null;
    Globals.refreshGroupPage = null;
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (this.loading) {
      return groupLoading();
    } else if (this.errorLoading) {
      return this.errorWidget;
    } else {
      return Scaffold(
        appBar: AppBar(
          centerTitle: true,
          title: AutoSizeText(
            Globals.currentGroup.groupName,
            maxLines: 1,
            style: TextStyle(fontSize: 40),
            minFontSize: 12,
            overflow: TextOverflow.ellipsis,
          ),
          actions: <Widget>[
            IconButton(
              icon: Icon(Icons.settings),
              tooltip: "Settings",
              key: Key("group_page:group_settings_button"),
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => GroupSettings()),
                ).then((_) => refreshList());
              },
            ),
          ],
        ),
        key: Key("group_page:scaffold"),
        body: Center(
          child: Column(
            children: <Widget>[
              Container(
                // height has to be here otherwise it overflows
                height: MediaQuery.of(context).size.height * .045,
                child: Stack(
                  children: <Widget>[
                    Align(
                      alignment: Alignment.center,
                      child: AutoSizeText(
                        "Events",
                        minFontSize: 12,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          decoration: TextDecoration.underline,
                          fontSize: 26,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                    Visibility(
                      visible: (Globals.user.groups[widget.groupId] != null &&
                          Globals.user.groups[widget.groupId].eventsUnseen
                              .isNotEmpty),
                      child: Align(
                        alignment: Alignment.centerRight,
                        child: Container(
                          child: IconButton(
                            icon: Icon(Icons.done_all),
                            color: Globals.pocketPollGreen,
                            tooltip: "Mark all seen",
                            onPressed: () {
                              markAllEventsSeen();
                            },
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              Expanded(
                child: Container(
                  height: MediaQuery.of(context).size.height * .80,
                  child: RefreshIndicator(
                    child: EventsList(
                      group: Globals.currentGroup,
                      events: Globals.currentGroup.events,
                      refreshEventsUnseen: updatePage,
                      refreshPage: refreshList,
                      getNextBatch: getNextBatch,
                    ),
                    onRefresh: refreshList,
                  ),
                ),
              ),
              Padding(
                padding:
                    EdgeInsets.all(MediaQuery.of(context).size.height * .02),
              )
            ],
          ),
        ),
        floatingActionButton: FloatingActionButton(
          child: Icon(Icons.add),
          key: Key("group_page:create_event_button"),
          onPressed: () {
            Navigator.push(context,
                    MaterialPageRoute(builder: (context) => CreateEvent()))
                .then((_) {
              this.refreshList();
            });
          },
        ),
      );
    }
  }

  // attempts to get group from DB. If success then display all events. Else show error
  void getGroup() async {
    int batchNum = (Globals.currentGroup == null)
        ? 0
        : Globals.currentGroup.currentBatchNum;
    ResultStatus<Group> status =
        await GroupsManager.getGroup(widget.groupId, batchNumber: batchNum);
    this.loading = false;
    if (status.success) {
      this.errorLoading = false;
      Globals.currentGroup = status.data;
      Globals.currentGroup.currentBatchNum = batchNum;
      setState(() {});
    } else {
      this.errorLoading = true;
      this.errorWidget = groupError(status.errorMessage);
      setState(() {});
    }
  }

  Widget groupLoading() {
    return Scaffold(
        appBar: AppBar(
            centerTitle: true,
            title: AutoSizeText(
              widget.groupName,
              maxLines: 1,
              style: TextStyle(fontSize: 40),
              minFontSize: 12,
              overflow: TextOverflow.ellipsis,
            ),
            actions: <Widget>[
              Visibility(
                // hacky but prevents weird autosizing of text since settings icon isn't there
                visible: false,
                maintainState: true,
                maintainAnimation: true,
                maintainSize: true,
                child: IconButton(
                  icon: Icon(Icons.settings),
                ),
              )
            ]),
        body: Center(child: CircularProgressIndicator()),
        key: Key("group_page:loading_scaffold"));
  }

  Widget groupError(String errorMsg) {
    return Scaffold(
        appBar: AppBar(
          centerTitle: true,
          title: AutoSizeText(
            widget.groupName,
            maxLines: 1,
            style: TextStyle(fontSize: 40),
            minFontSize: 12,
            overflow: TextOverflow.ellipsis,
          ),
          actions: <Widget>[
            Visibility(
              // hacky but prevents weird autosizing of text since settings icon isn't there
              visible: false,
              maintainState: true,
              maintainAnimation: true,
              maintainSize: true,
              child: IconButton(
                icon: Icon(Icons.settings),
              ),
            )
          ],
        ),
        body: Container(
          height: MediaQuery.of(context).size.height * .80,
          child: RefreshIndicator(
            onRefresh: refreshList,
            child: ListView(
              children: <Widget>[
                Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .15)),
                Center(child: Text(errorMsg, style: TextStyle(fontSize: 30))),
              ],
            ),
          ),
        ),
        key: Key("group_page:error_scaffold"));
  }

  // attempts to get the latest data for the group from the DB
  Future<void> refreshList() async {
    if (ModalRoute.of(context).isCurrent) {
      // only refresh if this page is actually visible
      getGroup();
      updatePage();
    }
    return;
  }

  /*
    Re-builds the page.
    It's own method to allow the mark all seen button to disappear if marking the last event seen
   */
  void updatePage() {
    if (!this.mounted)
      return; // don't set state if the widget is no longer here
    setState(() {});
  }

  // called when next/back buttons are pressed in event list. Gets the next/previous number of events in group
  void getNextBatch() {
    setState(() {
      this.loading = true;
    });
    getGroup();
  }

  // blind send to mark all events as seen, not critical
  void markAllEventsSeen() {
    UsersManager.markAllEventsAsSeen(widget.groupId);
    Globals.user.groups[widget.groupId].eventsUnseen.clear();
    updatePage();
  }
}
