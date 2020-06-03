import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/groups_widgets/group_page.dart';
import 'package:front_end_pocket_poll/imports/events_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/models/event_card_interface.dart';
import 'package:front_end_pocket_poll/models/group.dart';

class EventsList extends StatefulWidget {
  static final int eventsTypeNew = 0;
  static final int eventsTypeClosed = 1;
  static final int eventsTypeConsider = 2;
  static final int eventsTypeVoting = 3;
  static final int eventsTypeOccurring = 4;

  final List<EventCardInterface> events;
  final int eventsType;
  final bool isUnseenTab;
  final Group group;
  final Function refreshEventsUnseen;
  final Function refreshPage;
  final Function getNextBatch;
  final Function getPreviousBatch;
  final Function markAllEventsSeen;
  final int largestBatchIndexLoaded;
  final bool limitBatchIsLoaded;
  final double previousMaxScrollExtent;

  EventsList(
      {Key key,
      this.group,
      this.events,
      this.eventsType,
      this.refreshEventsUnseen,
      this.markAllEventsSeen,
      this.refreshPage,
      this.getNextBatch,
      this.getPreviousBatch,
      this.largestBatchIndexLoaded,
      this.limitBatchIsLoaded,
      this.previousMaxScrollExtent})
      : this.isUnseenTab = (eventsType == 0),
        super(key: key);

  @override
  _EventsListState createState() => _EventsListState();
}

class _EventsListState extends State<EventsList> {
  final ScrollController scrollController = new ScrollController();

  bool inTopDelta;
  bool inBottomDelta;
  bool isInitializingBuild;
  bool trackGlobalScrollPosition;

  @override
  void dispose() {
    print("event list dispose " + widget.eventsType.toString());
    this.scrollController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    print("event list init " + widget.eventsType.toString());
    this.scrollController.addListener(scrollListener);
    this.inBottomDelta = false;
    this.inTopDelta = false;
    this.isInitializingBuild = true;
    this.trackGlobalScrollPosition = true;

    super.initState();
  }

  afterBuild(BuildContext context) {
    //print("after build call back");

    //check if this build is the initial build or a build after getting a new batch
//    if (widget.largestBatchIndex > 0) {
//      //TODO this doesn't technically work, imagine if there was only the 0 batch, then there's still the excess call to getBatchOfEvents and the hop up the page.
//      // Also when you get to the last one, there needs to
//      int listSize = widget.events.length;
//      double listPixelSize = this.scrollController.position.maxScrollExtent;
//
//      int newEventsSize = (listSize % GroupsManager.BATCH_SIZE);
//      if (newEventsSize == 0) {
//        newEventsSize = GroupsManager.BATCH_SIZE;
//      }
//
//      //we subtract 1 to not over scroll
//      double scrollPercentage = (listSize - newEventsSize - 1) / listSize;
//      this.scrollController.jumpTo(listPixelSize * scrollPercentage);
//    }

//    if (widget.limitBatchIsLoaded) {
//      //only once we've tried to load past the limiting batch will 'limitBatchIsLoaded' be set.
//      this
//          .scrollController
//          .jumpTo(this.scrollController.position.maxScrollExtent);
//    } else
//    if (this.isInitializingBuild == true) {
//      if (Globals.eventListScrollPositionsAtDispose[widget.eventsType] !=
//          null) {
//        print("scrolling from after build call back");
//        //only once we've tried to load past the limiting batch will 'limitBatchIsLoaded' be set.
//        this.scrollController.jumpTo(
//            Globals.eventListScrollPositionsAtDispose[widget.eventsType]);
//      }
    this.isInitializingBuild = false;
//    }
  }

  scrollListener() {
    if (this.scrollController.offset >=
            this.scrollController.position.maxScrollExtent &&
        !this.scrollController.position.outOfRange) {
      print("reached the bottom");
      if (widget.largestBatchIndexLoaded >=
          GroupPage.maxEventBatchesInMemory - 1) {
        Globals.eventListScrollPositionsAtDispose[widget.eventsType] =
            widget.previousMaxScrollExtent;
      } else {
        Globals.eventListScrollPositionsAtDispose[widget.eventsType] =
            this.scrollController.position.maxScrollExtent;
      }
      this.trackGlobalScrollPosition = false;
      widget.getNextBatch(
          widget.eventsType, this.scrollController.position.maxScrollExtent);
    }

//    double delta = 500.0; // any pixel size smaller than one batch height works
//    if (this.scrollController.position.maxScrollExtent -
//                this.scrollController.position.pixels <=
//            delta &&
//        !this.inBottomDelta) {
////      widget.getNextBatch(widget.eventsType);
////      print("bottom 500px");
//      this.inBottomDelta = true;
//    } else if (this.scrollController.position.maxScrollExtent -
//            scrollController.position.pixels >
//        delta) {
//      this.inBottomDelta = false;
//    }
//
//    if (scrollController.position.pixels <= delta && !this.inTopDelta) {
//      //widget.getPreviousBatch(widget.eventsType);
//      //print("top 500px");
//      this.inTopDelta = true;
//    } else if (scrollController.position.pixels > delta) {
//      this.inTopDelta = false;
//    }

    if (this.scrollController.offset <=
            this.scrollController.position.minScrollExtent &&
        !this.scrollController.position.outOfRange) {
      print("reached the top");
      widget.getPreviousBatch(
          widget.eventsType, this.scrollController.position.maxScrollExtent);
    }

    if (this.trackGlobalScrollPosition) {
      Globals.eventListScrollPositionsAtDispose[widget.eventsType] =
          this.scrollController.position.pixels;
    }
  }

  @override
  Widget build(BuildContext context) {
    print("event list build " + widget.eventsType.toString());

    WidgetsBinding.instance.addPostFrameCallback((_) => afterBuild(context));

    if (widget.events.isEmpty) {
      return ListView(
        children: <Widget>[
          Padding(
            padding: EdgeInsets.all(20.0),
            child: Center(
              child: Text(
                  (widget.isUnseenTab)
                      ? "No new events."
                      : "No events currently in this stage. Click the plus button below to create new events.",
                  style: TextStyle(fontSize: 30)),
            ),
          )
        ],
      );
    } else {
      // sort the events
      widget.events.sort((a, b) {
        // this if statement is only needed in the new tab since all of the event types can be in it
        if (a.getEventMode() > b.getEventMode()) {
          return -1;
        } else if (b.getEventMode() > a.getEventMode()) {
          return 1;
        } else {
          // cards are same priority
          if (a.getEventMode() == EventsManager.considerMode) {
            return b.getEvent().pollBegin.isBefore(a.getEvent().pollBegin)
                ? 1
                : -1;
          } else if (a.getEventMode() == EventsManager.votingMode) {
            return b.getEvent().pollEnd.isBefore(a.getEvent().pollEnd) ? 1 : -1;
          } else if (a.getEventMode() == EventsManager.occurringMode) {
            return b
                    .getEvent()
                    .eventStartDateTime
                    .isBefore(a.getEvent().eventStartDateTime)
                ? 1
                : -1;
          } else {
            // event is in closed mode. we want the most recent times here otherwise the first event would always be at the top
            return a
                    .getEvent()
                    .eventStartDateTime
                    .isBefore(b.getEvent().eventStartDateTime)
                ? 1
                : -1;
          }
        }
      });

      List<Widget> widgetList = new List<Widget>.from(widget.events);

      if (widget.isUnseenTab) {
        // if unseen events are here, then we add a mark all seen button to the top of the list
        widgetList.insert(
            0,
            Align(
              alignment: Alignment.centerRight,
              child: Container(
                height: MediaQuery.of(context).size.height * .045,
                child: IconButton(
                  icon: Icon(Icons.done_all),
                  color: Globals.pocketPollGreen,
                  tooltip: "Mark all seen",
                  onPressed: () {
                    widget.markAllEventsSeen();
                  },
                ),
              ),
            ));
      }

//      if (!this.isInitializingBuild && widget.previousMaxScrollExtent != null) {
//        print("scrolling " + widget.eventsType.toString());
//        this.scrollController.jumpTo(widget.previousMaxScrollExtent);
//      }

      if (!this.isInitializingBuild &&
          Globals.eventListScrollPositionsAtDispose[widget.eventsType] !=
              null) {
        print("scrolling " + widget.eventsType.toString());
        this.scrollController.jumpTo(
            Globals.eventListScrollPositionsAtDispose[widget.eventsType]);
      }

      this.trackGlobalScrollPosition = true;

      return Scrollbar(
          key: Key("events_list:scrollBar"),
          child: ListView.builder(
              controller: this.scrollController,
              shrinkWrap: true,
              itemCount: widgetList.length,
              itemBuilder: (context, index) {
                return widgetList[index];
              }));
    }
  }
}
