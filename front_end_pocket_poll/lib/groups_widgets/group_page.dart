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

  static final int maxEventBatchesInMemory = 3;

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

  //this map is setup in the initState, it gives a map from the tab index to the
  // event list types. This is helpful when building the event lists from the
  // list builder
  final Map<int, int> listIndexesToEventTypes = new Map<int, int>();

  //set up all of the variables needed for tracking the event lists
  final Map<int, int> eventTypesToLargestBatchIndexLoaded = {
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

  //this keeps track of which event ids are in which batches. This is important
  // when we need to delete batches so that we don't have to do calculations for
  // sorting and iterating to figure whats where
  final Map<int, Map<int, List<String>>> eventTypesToBatchEventIds = {
    EventsList.eventsTypeNew: new Map<int, List<String>>(),
    EventsList.eventsTypeVoting: new Map<int, List<String>>(),
    EventsList.eventsTypeConsider: new Map<int, List<String>>(),
    EventsList.eventsTypeClosed: new Map<int, List<String>>(),
    EventsList.eventsTypeOccurring: new Map<int, List<String>>()
  };

  final Map<int, double> eventTypesToPreviousMaxScrollExtents = {
    EventsList.eventsTypeNew: null,
    EventsList.eventsTypeVoting: null,
    EventsList.eventsTypeConsider: null,
    EventsList.eventsTypeClosed: null,
    EventsList.eventsTypeOccurring: null
  };

  final Map<int, double> eventTypesToMaxScrollExtentOfMaxBatches = {
    EventsList.eventsTypeNew: null,
    EventsList.eventsTypeVoting: null,
    EventsList.eventsTypeConsider: null,
    EventsList.eventsTypeClosed: null,
    EventsList.eventsTypeOccurring: null
  };

  //Each event list needs to know where to load upon rebuild or reinit. This map
  // will keep that data of what position the list should build at.
  final Map<int, double> eventListScrollPositions = {};

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
    Globals.refreshGroupPage = refreshLoadedBatches;
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
      Globals.refreshGroupPage = refreshLoadedBatches;
      // app was recently resumed with this state being active, so refresh the group in case changes happened
      refreshLoadedBatches();
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
                ).then((_) => refreshLoadedBatches());
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
                    int eventsType = this.listIndexesToEventTypes[index];

                    Widget retWidget = EventsList(
                      events: this.eventCards[index],
                      eventsType: this.listIndexesToEventTypes[index],
                      markAllEventsSeen: markAllEventsSeen,
                      getNextBatch: getNextBatch,
                      getPreviousBatch: getPreviousBatch,
                      eventListScrollPositions: this.eventListScrollPositions,
                    );

                    //if we're at the top, wrap the list in a refresh indicator
                    if (this.eventTypesToLargestBatchIndexLoaded[eventsType] <
                        GroupPage.maxEventBatchesInMemory) {
                      retWidget = RefreshIndicator(
                        child: retWidget,
                        onRefresh: refreshLoadedBatches,
                      );
                    }

                    return retWidget;
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
                  refreshLoadedBatches();
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

  void getBatchOfEventsError() {
    showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
              title: Text("Error getting events data"),
              actions: <Widget>[
                FlatButton(
                  child: Text("OK"),
                  onPressed: () {
                    Navigator.of(context).pop();
                  },
                ),
              ],
              content: Text("Please try again later."));
        });
  }

  // attempts to get group from DB. If success then display all events. Else show error
  Future<void> getGroup() async {
    ResultStatus<GetGroupResponse> status =
        await GroupsManager.getGroup(widget.groupId);
    this.loading = false;
    if (status.success) {
      this.errorLoading = false;
      Globals.currentGroupResponse = status.data;

      for (int eventsType in this.eventTypesToBatchEventIds.keys) {
        this.eventTypesToBatchEventIds[eventsType].clear();

        //log the first batch event ids
        this.eventTypesToBatchEventIds[eventsType].putIfAbsent(
            0,
            () => Globals.currentGroupResponse.group
                .getEventsFromBatchType(eventsType)
                .keys
                .toList());

        //if the zeroth batch doesn't have enough events, we know 1 is the
        // limiting batch index
        if (this.eventTypesToBatchEventIds[eventsType][0].length <
            GroupsManager.BATCH_SIZE) {
          this.eventTypesToBatchLimits[eventsType] = 1;
        }
      }

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
          refreshLoadedBatches);
      this.eventCards[votingTab].add(eventCard);
    }

    for (String eventId
        in Globals.currentGroupResponse.group.considerEvents.keys) {
      EventCardInterface eventCard = new EventCardConsider(
          Globals.currentGroupResponse.group.groupId,
          Globals.currentGroupResponse.group.considerEvents[eventId],
          eventId,
          populateEventStages,
          refreshLoadedBatches);
      this.eventCards[considerTab].add(eventCard);
    }

    for (String eventId
        in Globals.currentGroupResponse.group.occurringEvents.keys) {
      EventCardInterface eventCard = new EventCardOccurring(
          Globals.currentGroupResponse.group.groupId,
          Globals.currentGroupResponse.group.occurringEvents[eventId],
          eventId,
          populateEventStages,
          refreshLoadedBatches);
      this.eventCards[occurringTab].add(eventCard);
    }

    for (String eventId
        in Globals.currentGroupResponse.group.closedEvents.keys) {
      EventCardInterface eventCard = new EventCardClosed(
          Globals.currentGroupResponse.group.groupId,
          Globals.currentGroupResponse.group.closedEvents[eventId],
          eventId,
          populateEventStages,
          refreshLoadedBatches);
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
            refreshLoadedBatches);
      } else if (eventMode == EventsManager.votingMode) {
        eventCard = new EventCardVoting(
            Globals.currentGroupResponse.group.groupId,
            Globals.currentGroupResponse.group.newEvents[eventId],
            eventId,
            populateEventStages,
            refreshLoadedBatches);
      } else if (eventMode == EventsManager.occurringMode) {
        eventCard = new EventCardOccurring(
            Globals.currentGroupResponse.group.groupId,
            Globals.currentGroupResponse.group.newEvents[eventId],
            eventId,
            populateEventStages,
            refreshLoadedBatches);
      } else if (eventMode == EventsManager.closedMode) {
        eventCard = new EventCardClosed(
            Globals.currentGroupResponse.group.groupId,
            Globals.currentGroupResponse.group.newEvents[eventId],
            eventId,
            populateEventStages,
            refreshLoadedBatches);
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
            onRefresh: getGroup,
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

  //attempts to get latest event info for the batch windows being viewed
  Future<void> refreshLoadedBatches() async {
    if (ModalRoute.of(context).isCurrent) {
      final Map<String, int> eventTypesLabelsToLargestBatchIndexLoaded = {
        GroupsManager.NEW_EVENTS:
            this.eventTypesToLargestBatchIndexLoaded[EventsList.eventsTypeNew],
        GroupsManager.OCCURRING_EVENTS:
            this.eventTypesToLargestBatchIndexLoaded[
                EventsList.eventsTypeOccurring],
        GroupsManager.VOTING_EVENTS: this
            .eventTypesToLargestBatchIndexLoaded[EventsList.eventsTypeVoting],
        GroupsManager.CONSIDER_EVENTS: this
            .eventTypesToLargestBatchIndexLoaded[EventsList.eventsTypeConsider],
        GroupsManager.CLOSED_EVENTS: this
            .eventTypesToLargestBatchIndexLoaded[EventsList.eventsTypeClosed],
      };

      // only refresh if this page is actually visible
      final ResultStatus<GetGroupResponse> resultStatus =
          await GroupsManager.getAllBatchesOfEvents(
              widget.groupId, eventTypesLabelsToLargestBatchIndexLoaded);

      if (resultStatus.success) {
        final GetGroupResponse apiResponse = resultStatus.data;

        Globals.currentGroupResponse = apiResponse;

        //assume this is a refresh after a lot of time of being on the page, we
        // no longer know if our found limits are valid so clear them
        for (final int batchType in this.eventTypesToBatchLimits.keys) {
          this.eventTypesToBatchLimits[batchType] = null;
        }

        this.populateEventStages();
      } else {
        this.getBatchOfEventsError();
      }
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

  // called when the user scrolls to the bottom of the page
  Future<void> getNextBatch(
      final int batchType, final double maxScrollExtent) async {
    final int batchIndex =
        this.eventTypesToLargestBatchIndexLoaded[batchType] + 1;

    //we only query the db when we haven't hit the batch index limit
    bool queryDb = (this.eventTypesToBatchLimits[batchType] == null ||
        this.eventTypesToBatchLimits[batchType] > batchIndex);

    if (queryDb) {
      //only track while the max list size has the capacity to grow
      if (batchIndex < GroupPage.maxEventBatchesInMemory) {
        this.eventTypesToPreviousMaxScrollExtents[batchType] = maxScrollExtent;
      } else {
        this.eventListScrollPositions[batchType] =
            this.eventTypesToPreviousMaxScrollExtents[batchType];
      }

      //batchIndex starts at 0, batchIndex equal to max means that we're loading
      // the first batch after the max in memory. To make scrolling back up work
      // we save the max scroll extent of the page will full batches loaded
      if (batchIndex == GroupPage.maxEventBatchesInMemory) {
        this.eventTypesToMaxScrollExtentOfMaxBatches[batchType] =
            maxScrollExtent;
      }

      final ResultStatus<GetGroupResponse> resultStatus =
          await GroupsManager.getBatchOfEvents(
              widget.groupId, batchIndex, batchType);

      if (resultStatus.success) {
        final GetGroupResponse apiResponse = resultStatus.data;

        if (apiResponse.group.getEventsFromBatchType(batchType).length > 0) {
          Globals.currentGroupResponse.group.addEvents(
              apiResponse.group.getEventsFromBatchType(batchType), batchType);
          Globals.currentGroupResponse.eventsUnseen
              .addAll(apiResponse.eventsUnseen);
          Globals.currentGroupResponse.eventsWithoutRatings
              .addAll(apiResponse.eventsWithoutRatings);

          //add the event ids to their map
          this.eventTypesToBatchEventIds[batchType].putIfAbsent(
              batchIndex,
              () => apiResponse.group
                  .getEventsFromBatchType(batchType)
                  .keys
                  .toList());

          //if this batch didn't have a full batch, the next number up is the
          // limiting index
          if (this.eventTypesToBatchEventIds[batchType][batchIndex].length <
              GroupsManager.BATCH_SIZE) {
            this.eventTypesToBatchLimits[batchType] = batchIndex + 1;
          }

          //remove the events at the top based on history
          if (Globals.currentGroupResponse.group
                  .getEventsFromBatchType(batchType)
                  .length >
              GroupPage.maxEventBatchesInMemory * GroupsManager.BATCH_SIZE) {
            Globals.currentGroupResponse.group
                .getEventsFromBatchType(batchType)
                .removeWhere((k, v) {
              return this
                  .eventTypesToBatchEventIds[batchType]
                      [batchIndex - GroupPage.maxEventBatchesInMemory]
                  .contains(k);
            });
          }

          //indicate that we've gotten the next batch
          this.eventTypesToLargestBatchIndexLoaded[batchType]++;

          this.populateEventStages();
        } else {
          //we didn't get anything so set the limit and go back to the last
          // batch index that had events
          this.eventTypesToBatchLimits[batchType] = batchIndex;

          //set the scroll position to the bottom of the page
          this.eventListScrollPositions[batchType] = maxScrollExtent;
        }
      } else {
        this.getBatchOfEventsError();
      }
    }
  }

  // called when the user scrolls to the top of the page
  Future<void> getPreviousBatch(
      final int batchType, final double maxScrollExtent) async {
    //the index we need is the max number allowed in memory before the max one
    // currently loaded
    final int batchIndex = this.eventTypesToLargestBatchIndexLoaded[batchType] -
        GroupPage.maxEventBatchesInMemory;

    //we only query the db when we haven't hit the batch index limit
    bool queryDb = (batchIndex >= 0);

    if (queryDb) {
      //the previous maxScrollExtent should be the bottom of the 2/3 mark, so
      // subtracting that from the max gives you the length to the bottom of
      // the top 1/3
      this.eventListScrollPositions[batchType] =
          this.eventTypesToMaxScrollExtentOfMaxBatches[batchType] -
              this.eventTypesToPreviousMaxScrollExtents[batchType];

      final ResultStatus<GetGroupResponse> resultStatus =
          await GroupsManager.getBatchOfEvents(
              widget.groupId, batchIndex, batchType);

      if (resultStatus.success) {
        final GetGroupResponse apiResponse = resultStatus.data;

        Globals.currentGroupResponse.group.addEvents(
            apiResponse.group.getEventsFromBatchType(batchType), batchType);
        Globals.currentGroupResponse.eventsUnseen
            .addAll(apiResponse.eventsUnseen);
        Globals.currentGroupResponse.eventsWithoutRatings
            .addAll(apiResponse.eventsWithoutRatings);

        //add the event ids to their map
        this.eventTypesToBatchEventIds[batchType].putIfAbsent(
            batchIndex,
            () => apiResponse.group
                .getEventsFromBatchType(batchType)
                .keys
                .toList());

        //remove the events at the bottom based on history
        Globals.currentGroupResponse.group
            .getEventsFromBatchType(batchType)
            .removeWhere((k, v) {
          return this
              .eventTypesToBatchEventIds[batchType]
                  [batchIndex + GroupPage.maxEventBatchesInMemory]
              .contains(k);
        });

        //indicate that the largest batch currently loaded just got removed
        this.eventTypesToLargestBatchIndexLoaded[batchType]--;

        this.populateEventStages();
      } else {
        this.getBatchOfEventsError();
      }
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
    Globals.currentGroupResponse.group.newEvents.clear();
    this.currentTab = this.occurringTab;
    this.tabController.animateTo(this.currentTab);
    populateEventStages();
  }
}
