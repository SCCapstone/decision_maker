import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/groups_widgets/group_page.dart';
import 'package:front_end_pocket_poll/imports/events_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/models/event_card_interface.dart';

class EventsList extends StatefulWidget {
  static final int eventsTypeNew = 0;
  static final int eventsTypeClosed = 1;
  static final int eventsTypeConsider = 2;
  static final int eventsTypeVoting = 3;
  static final int eventsTypeOccurring = 4;

  final List<EventCardInterface> events;
  final int eventsType;
  final bool isUnseenTab;
  final Function getNextBatch;
  final Function getPreviousBatch;
  final Function markAllEventsSeen;
  final int largestBatchIndexLoaded;
  final int batchLimit;
  final bool batchLimitHit;
  final double previousMaxScrollExtent;
  final Map<int, double> eventListScrollPositions;

  EventsList(
      {Key key,
      this.events,
      this.eventsType,
      this.markAllEventsSeen,
      this.getNextBatch,
      this.getPreviousBatch,
      this.largestBatchIndexLoaded,
      this.batchLimit,
      this.previousMaxScrollExtent,
      this.eventListScrollPositions})
      : this.isUnseenTab = (eventsType == 0),
        this.batchLimitHit = (largestBatchIndexLoaded + 1 == batchLimit),
        super(key: key);

  @override
  _EventsListState createState() => _EventsListState();
}

class _EventsListState extends State<EventsList> {
  ScrollController scrollController = new ScrollController();

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

    super.initState();
  }

  scrollListener() {
    widget.eventListScrollPositions[widget.eventsType] =
        this.scrollController.position.pixels;

    if (this.scrollController.offset >=
            this.scrollController.position.maxScrollExtent &&
        !this.scrollController.position.outOfRange) {
      print("reached the bottom");
      if (widget.largestBatchIndexLoaded >=
          GroupPage.maxEventBatchesInMemory - 1) {
        widget.eventListScrollPositions[widget.eventsType] =
            widget.previousMaxScrollExtent;
      }

      widget.getNextBatch(
          widget.eventsType, this.scrollController.position.maxScrollExtent);
    }

    if (this.scrollController.offset <=
            this.scrollController.position.minScrollExtent &&
        !this.scrollController.position.outOfRange) {
      print("reached the top");
      //the previous maxScrollExtent should be the bottom of the 2/3 mark, so
      // subtracting that from the max gives you the length to the bottom of
      // the top 1/3
      widget.eventListScrollPositions[widget.eventsType] =
          this.scrollController.position.maxScrollExtent -
              widget.previousMaxScrollExtent;
      widget.getPreviousBatch(
          widget.eventsType, widget.previousMaxScrollExtent);
    }

    //TODO fix the last batch jump
  }

  @override
  Widget build(BuildContext context) {
    print("event list build " + widget.eventsType.toString());

    if (!widget.batchLimitHit &&
        widget.eventListScrollPositions[widget.eventsType] != null) {
      //dispose the old controller, reset the controller, add the listener
      this.scrollController.dispose();
      this.scrollController = new ScrollController(
          initialScrollOffset:
              widget.eventListScrollPositions[widget.eventsType]);
      this.scrollController.addListener(scrollListener);
    }

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

      return Scrollbar(
          key: UniqueKey(), //Key("events_list:scrollBar"),
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
