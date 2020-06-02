import 'package:flutter/material.dart';
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
  final Function markAllEventsSeen;

  EventsList({Key key,
    this.group,
    this.events,
    this.eventsType,
    this.refreshEventsUnseen,
    this.markAllEventsSeen,
    this.refreshPage,
    this.getNextBatch})
      : this.isUnseenTab = (eventsType == 0),
        super(key: key);

  @override
  _EventsListState createState() => _EventsListState();
}

class _EventsListState extends State<EventsList> {
  final ScrollController scrollController = new ScrollController();

  bool inTopDelta;
  bool inBottomDelta;

  @override
  void dispose() {
    this.scrollController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    print("event list init");
    this.scrollController.addListener(scrollListener);
    this.inBottomDelta = false;
    this.inTopDelta = false;

    super.initState();
  }

  scrollListener() {
    if (this.scrollController.offset >=
        this.scrollController.position.maxScrollExtent &&
        !this.scrollController.position.outOfRange) {
//      print("reach the bottom");
//      widget.getNextBatch(widget.eventsType);
    }

    double delta = this.scrollController.position.maxScrollExtent / 3;
    if (this.scrollController.position.maxScrollExtent -
        scrollController.position.pixels <=
        delta &&
        !this.inBottomDelta) {
      widget.getNextBatch(widget.eventsType);
      print("bottom third");
      this.inBottomDelta = true;
    } else if (this.scrollController.position.maxScrollExtent -
        scrollController.position.pixels >
        delta) {
      this.inBottomDelta = false;
    }

    if (this.scrollController.offset <=
        this.scrollController.position.minScrollExtent &&
        !this.scrollController.position.outOfRange) {
      print("reach the top");
      //todo
      //widget.getPreviousBatch(widget.eventsType);
    }
  }

  @override
  Widget build(BuildContext context) {
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
            return b
                .getEvent()
                .pollBegin
                .isBefore(a
                .getEvent()
                .pollBegin)
                ? 1
                : -1;
          } else if (a.getEventMode() == EventsManager.votingMode) {
            return b
                .getEvent()
                .pollEnd
                .isBefore(a
                .getEvent()
                .pollEnd) ? 1 : -1;
          } else if (a.getEventMode() == EventsManager.occurringMode) {
            return b
                .getEvent()
                .eventStartDateTime
                .isBefore(a
                .getEvent()
                .eventStartDateTime)
                ? 1
                : -1;
          } else {
            // event is in closed mode. we want the most recent times here otherwise the first event would always be at the top
            return a
                .getEvent()
                .eventStartDateTime
                .isBefore(b
                .getEvent()
                .eventStartDateTime)
                ? 1
                : -1;
          }
        }
      });

      List<Widget> widgetList = new List<Widget>.from(widget.events);
      int numEvents = (Globals.currentGroupResponse.group.currentBatchNum + 1) *
          GroupsManager.BATCH_SIZE;
      // TODO this needs to be updated with the new multiple batches.
      //  Since at this stage there is at least one event. Can just do evetcard.getEventMode()
      // and then in the currentGroupResponse have a map of modes to batchNums

      // have a button shown if more events to show
      Row buttonRow = new Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          // back button must be on every batch except the first
          Visibility(
            visible: Globals.currentGroupResponse.group.currentBatchNum > 0,
            child: RaisedButton(
              onPressed: () {
                Globals.currentGroupResponse.group.currentBatchNum -= 1;
                widget.getNextBatch();
              },
              child: Text("Back"),
            ),
          ),
          Visibility(
            visible: Globals.currentGroupResponse.group.currentBatchNum > 0 &&
                Globals.currentGroupResponse.group.totalNumberOfEvents -
                    numEvents >
                    0,
            child: Padding(
              padding: EdgeInsets.all(MediaQuery
                  .of(context)
                  .size
                  .width * .02),
            ),
          ),
          // next button only present when there are more events to show
          Visibility(
            visible: Globals.currentGroupResponse.group.totalNumberOfEvents -
                numEvents >
                0,
            child: RaisedButton(
              onPressed: () {
                Globals.currentGroupResponse.group.currentBatchNum += 1;
                widget.getNextBatch();
              },
              child: Text("Next"),
            ),
          )
        ],
      );
      widgetList.add(buttonRow);
      if (widget.isUnseenTab) {
        // if unseen events are here, then we add a mark all seen button to the top of the list
        widgetList.insert(
            0,
            Align(
              alignment: Alignment.centerRight,
              child: Container(
                height: MediaQuery
                    .of(context)
                    .size
                    .height * .045,
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
