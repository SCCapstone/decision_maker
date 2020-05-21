import 'dart:async';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/events_widgets/event_card_closed.dart';
import 'package:front_end_pocket_poll/events_widgets/event_card_consider.dart';
import 'package:front_end_pocket_poll/events_widgets/event_card_occurring.dart';
import 'package:front_end_pocket_poll/events_widgets/event_card_voting.dart';
import 'package:front_end_pocket_poll/events_widgets/event_create.dart';
import 'package:front_end_pocket_poll/events_widgets/events_list.dart';
import 'package:front_end_pocket_poll/imports/events_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/event_card_interface.dart';
import 'package:front_end_pocket_poll/models/get_group_response.dart';

import 'group_settings.dart';

class GroupPage extends StatefulWidget {
  final String groupId;
  final String groupName;

  GroupPage({Key key, this.groupId, this.groupName}) : super(key: key);

  @override
  _GroupPageState createState() => new _GroupPageState();
}

class _GroupPageState extends State<GroupPage>
    with WidgetsBindingObserver, SingleTickerProviderStateMixin {
  static final int totalTabs = 5;
  final List<AutoSizeText> tabs = new List(totalTabs);

  // indexes for the tabs
  final int unseenTab = 0;
  final int considerTab = 3;
  final int votingTab = 2;
  final int occurringTab = 1;
  final int closedTab = 4;
  final Map<int, List<EventCardInterface>> eventCards = new Map<int,
      List<EventCardInterface>>(); // map of tab index to list of event cards

  TabController tabController;
  int currentTab;
  bool loading;
  bool errorLoading;
  bool
      refreshFlag; // hacky but the onResume method below is being called twice for some reason
  Widget errorWidget;

  @override
  void initState() {
    this.tabs[unseenTab] = new AutoSizeText("Unseen",
        maxLines: 1,
        style: TextStyle(fontSize: 17),
        minFontSize: 12,
        overflow: TextOverflow.ellipsis,
        key: Key("groups_page:unseen_tab"));
    this.tabs[occurringTab] = new AutoSizeText("Occurring",
        maxLines: 1,
        style: TextStyle(fontSize: 17),
        minFontSize: 12,
        overflow: TextOverflow.ellipsis,
        key: Key("groups_page:occurring_tab"));
    this.tabs[votingTab] = new AutoSizeText("Voting",
        maxLines: 1,
        style: TextStyle(fontSize: 17),
        minFontSize: 12,
        overflow: TextOverflow.ellipsis,
        key: Key("groups_page:voting_tab"));
    this.tabs[considerTab] = new AutoSizeText("Consider",
        maxLines: 1,
        style: TextStyle(fontSize: 17),
        minFontSize: 12,
        overflow: TextOverflow.ellipsis,
        key: Key("groups_page:consider_tab"));
    this.tabs[closedTab] = new AutoSizeText("Closed",
        maxLines: 1,
        style: TextStyle(fontSize: 17),
        minFontSize: 12,
        overflow: TextOverflow.ellipsis,
        key: Key("groups_page:closed_tab"));

    WidgetsBinding.instance.addObserver(this);
    this.loading = true;
    this.refreshFlag = false;
    this.errorLoading = false;
    this.tabController =
        new TabController(length: this.tabs.length, vsync: this);
    this.tabController.addListener(handleTabChange);

    for (int i = 0; i < totalTabs; i++) {
      // init the map of event cards with empty lists
      this.eventCards.putIfAbsent(i, () => new List<EventCardInterface>());
    }

    // allows this page to be refreshed if a new notification comes in for the group
    Globals.refreshGroupPage = refreshList;
    getGroup();
    super.initState();
  }

  @override
  void dispose() {
    this.tabController.dispose();
    Globals.currentGroupResponse.group = null;
    Globals.refreshGroupPage = null;
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    print("whyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy");
    if (state == AppLifecycleState.resumed) {
      Globals.refreshGroupPage = refreshList;
      // TODO why tf is this being called twice...
      print("***************************");
      // app was recently resumed with this state being active, so signal to refresh the group in case changes happened
      getGroup();
    }
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
            Globals.currentGroupResponse.group.groupName,
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
          bottom: PreferredSize(
            // used to display the tabs and the sort icon
            preferredSize: Size(MediaQuery.of(context).size.width,
                MediaQuery.of(context).size.height * .045),
            child: Row(
              children: <Widget>[
                Expanded(
                  child: TabBar(
                      controller: this.tabController,
                      isScrollable: true,
                      indicatorWeight: 3,
                      indicatorColor: Colors.blueAccent,
                      tabs: this.tabs),
                )
              ],
            ),
          ),
        ),
        key: Key("group_page:scaffold"),
        body: Center(
          child: Column(
            children: <Widget>[
              Container(
                // height has to be here otherwise it overflows
                height: MediaQuery.of(context).size.height * .045,
                child: Visibility(
                  visible: (Globals.user.groups[widget.groupId] != null &&
                      this.currentTab == unseenTab &&
                      Globals.user.groups[widget.groupId].eventsUnseen > 0),
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
              ),
              Expanded(
                child: TabBarView(
                  controller: this.tabController,
                  key: Key("group_page:tab_view"),
                  children: List.generate(this.tabs.length, (index) {
                    return Container(
                      height: MediaQuery.of(context).size.height * .80,
                      child: RefreshIndicator(
                        child: EventsList(
                          group: Globals.currentGroupResponse.group,
                          events: this.eventCards[index],
                          isUnseenTab: (index == unseenTab) ? true : false,
                          refreshEventsUnseen: updatePage,
                          refreshPage: refreshList,
                          getNextBatch: getNextBatch,
                        ),
                        onRefresh: refreshList,
                      ),
                    );
                  }),
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
                    MaterialPageRoute(builder: (context) => EventCreate()))
                .then((val) {
              if (val != null) {
                // this means that an event was created, so don't make an API call to refresh
                populateEventStages();
              } else {
                // no event created, so make API call to make sure page is most up to date
                refreshList();
              }
            });
          },
        ),
      );
    }
  }

  // attempts to get group from DB. If success then display all events. Else show error
  void getGroup() async {
    int batchNum = (Globals.currentGroupResponse.group == null)
        ? 0
        : Globals.currentGroupResponse.group.currentBatchNum;
    ResultStatus<GetGroupResponse> status =
        await GroupsManager.getGroup(widget.groupId, batchNumber: batchNum);
    this.loading = false;
    if (status.success) {
      this.errorLoading = false;
      Globals.currentGroupResponse = status.data;
      Globals.currentGroupResponse.group.currentBatchNum = batchNum;
      populateEventStages();
    } else {
      this.errorLoading = true;
      this.errorWidget = groupError(status.errorMessage);
      updatePage();
    }
  }

  // populates the listviews of all the different event stages with what was returned by db
  void populateEventStages() {
    for (int tab in this.eventCards.keys) {
      this.eventCards[tab].clear();
    }

    for (String eventId in Globals.currentGroupResponse.group.events.keys) {
      int eventMode = EventsManager.getEventMode(
          Globals.currentGroupResponse.group.events[eventId]);
      if (eventMode == EventsManager.considerMode) {
        EventCardInterface eventCard = new EventCardConsider(
            Globals.currentGroupResponse.group.groupId,
            Globals.currentGroupResponse.group.events[eventId],
            eventId,
            updatePage,
            refreshList);
        this.eventCards[considerTab].add(eventCard);
        if (Globals.currentGroupResponse.eventsUnseen.keys.contains(eventId)) {
          this.eventCards[unseenTab].add(eventCard);
        }
      } else if (eventMode == EventsManager.votingMode) {
        EventCardInterface eventCard = new EventCardVoting(
            Globals.currentGroupResponse.group.groupId,
            Globals.currentGroupResponse.group.events[eventId],
            eventId,
            updatePage,
            refreshList);
        this.eventCards[votingTab].add(eventCard);
        if (Globals.currentGroupResponse.eventsUnseen.keys.contains(eventId)) {
          this.eventCards[unseenTab].add(eventCard);
        }
      } else if (eventMode == EventsManager.occurringMode) {
        EventCardInterface eventCard = new EventCardOccurring(
            Globals.currentGroupResponse.group.groupId,
            Globals.currentGroupResponse.group.events[eventId],
            eventId,
            updatePage,
            refreshList);
        this.eventCards[occurringTab].add(eventCard);
        if (Globals.currentGroupResponse.eventsUnseen.keys.contains(eventId)) {
          this.eventCards[unseenTab].add(eventCard);
        }
      } else if (eventMode == EventsManager.closedMode) {
        EventCardInterface eventCard = new EventCardClosed(
            Globals.currentGroupResponse.group.groupId,
            Globals.currentGroupResponse.group.events[eventId],
            eventId,
            updatePage,
            refreshList);
        this.eventCards[closedTab].add(eventCard);
        if (Globals.currentGroupResponse.eventsUnseen.keys.contains(eventId)) {
          this.eventCards[unseenTab].add(eventCard);
        }
      }
    }
    updatePage();
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
    if (!this.mounted) {
      return; // don't set state if the widget is no longer here
    }
    setState(() {});
  }

  // called when next/back buttons are pressed in event list. Gets the next/previous number of events in group
  void getNextBatch() {
    setState(() {
      this.loading = true;
    });
    getGroup();
  }

  // whenever the tab changes make sure to save current tab index
  void handleTabChange() {
    setState(() {
      this.currentTab = this.tabController.index;
    });
  }

  // blind send to mark all events as seen, not critical
  void markAllEventsSeen() {
    UsersManager.markAllEventsAsSeen(widget.groupId);
    Globals.user.groups[widget.groupId].eventsUnseen = 0;
    Globals.currentGroupResponse.eventsUnseen.clear();
    populateEventStages();
  }
}
