import 'dart:async';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
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
  final Map<int, int> listIndexesToEventTypes = new Map<int, int>();
  final Map<int, int> eventTypesToCurrentHighestBatchIndex = {
    EventsList.eventsTypeNew: 0,
    EventsList.eventsTypeVoting: 0,
    EventsList.eventsTypeConsider: 0,
    EventsList.eventsTypeClosed: 0,
    EventsList.eventsTypeOccurring: 0
  };

  //this 'batch limit' is the index of the first batch that comes back empty
  final Map<int, int> eventTypesToBatchLimits = {
    EventsList.eventsTypeNew: null,
    EventsList.eventsTypeVoting: null,
    EventsList.eventsTypeConsider: null,
    EventsList.eventsTypeClosed: null,
    EventsList.eventsTypeOccurring: null
  };

  //this 'batch limit' is the index of the first batch that comes back empty
  final Map<int, Map<int, List<String>>> eventTypesToBatchEventIds = {
    EventsList.eventsTypeNew: new Map<int, List<String>>(),
    EventsList.eventsTypeVoting: new Map<int, List<String>>(),
    EventsList.eventsTypeConsider: new Map<int, List<String>>(),
    EventsList.eventsTypeClosed: new Map<int, List<String>>(),
    EventsList.eventsTypeOccurring: new Map<int, List<String>>()
  };

  final Map<int, List<EventCardInterface>> eventCards = new Map<int,
      List<EventCardInterface>>(); // map of tab index to list of event cards

  TabController tabController;
  int currentTab;
  bool loading;
  bool errorLoading;
  bool firstLoading;
  Widget errorWidget;

  @override
  void initState() {
    WidgetsBinding.instance.addObserver(this);

    this.listIndexesToEventTypes[unseenTab] = EventsList.eventsTypeNew;
    this.listIndexesToEventTypes[considerTab] = EventsList.eventsTypeConsider;
    this.listIndexesToEventTypes[votingTab] = EventsList.eventsTypeVoting;
    this.listIndexesToEventTypes[occurringTab] = EventsList.eventsTypeOccurring;
    this.listIndexesToEventTypes[closedTab] = EventsList.eventsTypeClosed;

    this.tabs[unseenTab] = new AutoSizeText("New",
        maxLines: 1,
        style: TextStyle(fontSize: 17),
        minFontSize: 12,
        overflow: TextOverflow.ellipsis,
        key: Key("groups_page:unseen_tab"));
    this.tabs[occurringTab] = new AutoSizeText("Ready",
        maxLines: 1,
        style: TextStyle(fontSize: 17),
        minFontSize: 12,
        overflow: TextOverflow.ellipsis,
        key: Key("groups_page:ready_tab"));
    this.tabs[votingTab] = new AutoSizeText("Vote",
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
    this.tabs[closedTab] = new AutoSizeText("Old",
        maxLines: 1,
        style: TextStyle(fontSize: 17),
        minFontSize: 12,
        overflow: TextOverflow.ellipsis,
        key: Key("groups_page:closed_tab"));

    this.loading = true;
    this.errorLoading = false;
    this.firstLoading = true;
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
    if (state == AppLifecycleState.resumed) {
      Globals.refreshGroupPage = refreshList;
      // app was recently resumed with this state being active, so refresh the group in case changes happened
      getGroup();
    }
  }

  @override
  Widget build(BuildContext context) {
    print("group_page build");
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
                MediaQuery.of(context).size.height * .035),
            child: Row(
              children: <Widget>[
                Expanded(
                  child: TabBar(
                      controller: this.tabController,
                      isScrollable: true,
                      indicatorWeight: 3,
                      indicatorColor: Colors.white,
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
              Expanded(
                child: TabBarView(
                  controller: this.tabController,
                  key: Key("group_page:tab_view"),
                  children: List.generate(this.tabs.length, (index) {
                    return RefreshIndicator(
                      child: EventsList(
                        group: Globals.currentGroupResponse.group,
                        events: this.eventCards[index],
                        eventsType: this.listIndexesToEventTypes[index],
                        refreshEventsUnseen: updatePage,
                        markAllEventsSeen: markAllEventsSeen,
                        refreshPage: refreshList,
                        getNextBatch: getNextBatch,
                      ),
                      onRefresh: refreshList,
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
            if (Globals.currentGroupResponse.group.categories.isNotEmpty) {
              Navigator.push(context,
                      MaterialPageRoute(builder: (context) => EventCreate()))
                  .then((val) {
                if (val != null) {
                  // this means that an event was created, so don't make an API call to refresh
                  try {
                    int eventMode = val as int;
                    if (eventMode == EventsManager.considerMode) {
                      this.currentTab = considerTab;
                    } else if (eventMode == EventsManager.votingMode) {
                      this.currentTab = votingTab;
                    } else if (eventMode == EventsManager.occurringMode) {
                      this.currentTab = occurringTab;
                    }
                  } catch (e) {
                    // do nothing, this shouldn't ever be reached
                    debugPrintStack();
                  }
                  this.tabController.animateTo(this.currentTab);
                  populateEventStages();
                } else {
                  // no event created, so make API call to make sure page is most up to date
                  refreshList();
                }
              });
            } else {
              // we aren't allowing users to go to this page if they have no categories, so show a popup telling them this
              createEventError();
            }
          },
        ),
      );
    }
  }

  void createEventError() {
    showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
              title: Text("Cannot create event"),
              actions: <Widget>[
                FlatButton(
                  child: Text("OK"),
                  onPressed: () {
                    Navigator.of(context).pop();
                  },
                ),
              ],
              content: RichText(
                text: TextSpan(
                  style: TextStyle(
                      color: (Globals.user.appSettings.darkTheme)
                          ? Colors.white
                          : Colors.black),
                  children: [
                    TextSpan(
                        text:
                            "No categories are attached to this group. Click on this icon:  "),
                    WidgetSpan(
                      child: Icon(Icons.settings),
                    ),
                    TextSpan(
                        text:
                            " found in the top right corner of this page to add some."),
                  ],
                ),
              ));
        });
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

      this.eventTypesToBatchEventIds[EventsList.eventsTypeNew].putIfAbsent(
          0, () => Globals.currentGroupResponse.group.newEvents.keys.toList());
      this.eventTypesToBatchEventIds[EventsList.eventsTypeVoting].putIfAbsent(0,
          () => Globals.currentGroupResponse.group.votingEvents.keys.toList());
      this.eventTypesToBatchEventIds[EventsList.eventsTypeClosed].putIfAbsent(0,
          () => Globals.currentGroupResponse.group.closedEvents.keys.toList());
      this.eventTypesToBatchEventIds[EventsList.eventsTypeConsider].putIfAbsent(
          0,
          () =>
              Globals.currentGroupResponse.group.considerEvents.keys.toList());
      this
          .eventTypesToBatchEventIds[EventsList.eventsTypeOccurring]
          .putIfAbsent(
              0,
              () => Globals.currentGroupResponse.group.occurringEvents.keys
                  .toList());

      if (this.firstLoading) {
        /*
            On the first loading of the page, check to see if there are any unseen events.
            If so, then the first landing tab will be the unseen events tab. Else, go to the second tab
         */
        if (Globals.currentGroupResponse.eventsUnseen.isEmpty) {
          this.currentTab = occurringTab;
        } else {
          this.currentTab = unseenTab;
        }
        this.firstLoading = false;
      }
      this.tabController.animateTo(this.currentTab);
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

    for (String eventId
        in Globals.currentGroupResponse.group.votingEvents.keys) {
      EventCardInterface eventCard = new EventCardVoting(
          Globals.currentGroupResponse.group.groupId,
          Globals.currentGroupResponse.group.votingEvents[eventId],
          eventId,
          populateEventStages,
          refreshList);
      this.eventCards[votingTab].add(eventCard);
    }

    for (String eventId
        in Globals.currentGroupResponse.group.considerEvents.keys) {
      EventCardInterface eventCard = new EventCardConsider(
          Globals.currentGroupResponse.group.groupId,
          Globals.currentGroupResponse.group.considerEvents[eventId],
          eventId,
          populateEventStages,
          refreshList);
      this.eventCards[considerTab].add(eventCard);
    }

    for (String eventId
        in Globals.currentGroupResponse.group.occurringEvents.keys) {
      EventCardInterface eventCard = new EventCardOccurring(
          Globals.currentGroupResponse.group.groupId,
          Globals.currentGroupResponse.group.occurringEvents[eventId],
          eventId,
          populateEventStages,
          refreshList);
      this.eventCards[occurringTab].add(eventCard);
    }

    for (String eventId
        in Globals.currentGroupResponse.group.closedEvents.keys) {
      EventCardInterface eventCard = new EventCardClosed(
          Globals.currentGroupResponse.group.groupId,
          Globals.currentGroupResponse.group.closedEvents[eventId],
          eventId,
          populateEventStages,
          refreshList);
      this.eventCards[closedTab].add(eventCard);
    }

    for (String eventId in Globals.currentGroupResponse.group.newEvents.keys) {
      int eventMode = EventsManager.getEventMode(
          Globals.currentGroupResponse.group.newEvents[eventId]);
      EventCardInterface eventCard;
      if (eventMode == EventsManager.considerMode) {
        eventCard = new EventCardConsider(
            Globals.currentGroupResponse.group.groupId,
            Globals.currentGroupResponse.group.newEvents[eventId],
            eventId,
            populateEventStages,
            refreshList);
      } else if (eventMode == EventsManager.votingMode) {
        eventCard = new EventCardVoting(
            Globals.currentGroupResponse.group.groupId,
            Globals.currentGroupResponse.group.newEvents[eventId],
            eventId,
            populateEventStages,
            refreshList);
      } else if (eventMode == EventsManager.occurringMode) {
        eventCard = new EventCardOccurring(
            Globals.currentGroupResponse.group.groupId,
            Globals.currentGroupResponse.group.newEvents[eventId],
            eventId,
            populateEventStages,
            refreshList);
      } else if (eventMode == EventsManager.closedMode) {
        eventCard = new EventCardClosed(
            Globals.currentGroupResponse.group.groupId,
            Globals.currentGroupResponse.group.newEvents[eventId],
            eventId,
            populateEventStages,
            refreshList);
      }
      this.eventCards[unseenTab].add(eventCard);
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
            ],
            bottom: PreferredSize(
              // just have this empty so no weird flash after loading
              preferredSize: Size(MediaQuery.of(context).size.width,
                  MediaQuery.of(context).size.height * .035),
              child: Container(),
            )),
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
          bottom: PreferredSize(
            // just have this empty so no weird flash after loading
            preferredSize: Size(MediaQuery.of(context).size.width,
                MediaQuery.of(context).size.height * .035),
            child: Container(),
          ),
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

      //wipe these out, the page refreshed and we don't know if these still hold
      this.eventTypesToBatchLimits[EventsList.eventsTypeNew] = null;
      this.eventTypesToBatchLimits[EventsList.eventsTypeVoting] = null;
      this.eventTypesToBatchLimits[EventsList.eventsTypeConsider] = null;
      this.eventTypesToBatchLimits[EventsList.eventsTypeClosed] = null;
      this.eventTypesToBatchLimits[EventsList.eventsTypeOccurring] = null;

      //reset these, the page refreshed and we just got the zeroed info
      this.eventTypesToCurrentHighestBatchIndex[EventsList.eventsTypeNew] = 0;
      this.eventTypesToCurrentHighestBatchIndex[EventsList.eventsTypeVoting] =
          0;
      this.eventTypesToCurrentHighestBatchIndex[EventsList.eventsTypeConsider] =
          0;
      this.eventTypesToCurrentHighestBatchIndex[EventsList.eventsTypeClosed] =
          0;
      this.eventTypesToCurrentHighestBatchIndex[
          EventsList.eventsTypeOccurring] = 0;

      this.eventTypesToBatchEventIds[EventsList.eventsTypeNew].clear();
      this.eventTypesToBatchEventIds[EventsList.eventsTypeVoting].clear();
      this.eventTypesToBatchEventIds[EventsList.eventsTypeConsider].clear();
      this.eventTypesToBatchEventIds[EventsList.eventsTypeClosed].clear();
      this.eventTypesToBatchEventIds[EventsList.eventsTypeOccurring].clear();
    }
    return;
  }

  /*
    Re-builds the page.
    It's own method to prevent exceptions if user clicks out of the page quickly
   */
  void updatePage() {
    if (!this.mounted) {
      return; // don't set state if the widget is no longer here
    }
    setState(() {});
  }

  // called when next/back buttons are pressed in event list. Gets the next/previous number of events in group
  void getNextBatch(final int batchType) {
    //indicate that we're getting the next batch
    this.eventTypesToCurrentHighestBatchIndex[batchType]++;
    final int batchIndex = this.eventTypesToCurrentHighestBatchIndex[batchType];

    //we only query the db when we haven't hit the batch index limit
    bool queryDb = (this.eventTypesToBatchLimits[batchType] == null ||
        this.eventTypesToBatchLimits[batchType] > batchIndex);

    if (queryDb) {
      GroupsManager.getBatchOfEvents(widget.groupId, batchIndex, batchType)
          .then((ResultStatus<GetGroupResponse> resultStatus) {
        final GetGroupResponse apiResponse = resultStatus.data;
        if (resultStatus.success) {
          if (apiResponse.group.getEventsFromBatchType(batchType).length > 0) {
            Globals.currentGroupResponse.group
                .addEvents(apiResponse.group.getEventsFromBatchType(batchType));

            //add the event ids to their map
            this.eventTypesToBatchEventIds[batchType].putIfAbsent(
                batchIndex,
                () => apiResponse.group
                    .getEventsFromBatchType(batchType)
                    .keys
                    .toList());

            //remove the events at the top based on history
            if (Globals.currentGroupResponse.group
                    .getEventsFromBatchType(batchType)
                    .length >
                3 * GroupsManager.BATCH_SIZE) {
              Globals.currentGroupResponse.group
                  .getEventsFromBatchType(batchType)
                  .removeWhere((k, v) {
                return this
                    .eventTypesToBatchEventIds[batchType][batchIndex - 3]
                    .contains(k);
              });

              //delete the history
              this.eventTypesToBatchEventIds[batchType].remove(batchIndex - 3);
            }
          } else {
            //we didn't get anything so set the limit and go back to the last
            // batch index that had events
            this.eventTypesToBatchLimits[batchType] =
                this.eventTypesToCurrentHighestBatchIndex[batchType];
            this.eventTypesToCurrentHighestBatchIndex[batchType]--;
          }

          populateEventStages();
        } else {
          print(resultStatus.errorMessage);
        }
      });
    } else {
      //we've just tried to get the 'limit' batch again, go back to the last
      // batch index that had events
      this.eventTypesToCurrentHighestBatchIndex[batchType]--;
    }
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
    this.currentTab = this.occurringTab;
    this.tabController.animateTo(this.currentTab);
    populateEventStages();
  }
}
