import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/events_widgets/event_card_closed.dart';
import 'package:front_end_pocket_poll/events_widgets/event_card_consider.dart';
import 'package:front_end_pocket_poll/events_widgets/event_card_occurring.dart';
import 'package:front_end_pocket_poll/events_widgets/event_card_voting.dart';
import 'package:front_end_pocket_poll/imports/events_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/models/event.dart';
import 'package:front_end_pocket_poll/models/event_card_interface.dart';
import 'package:front_end_pocket_poll/models/group.dart';

class EventsList extends StatefulWidget {
  final Map<String, Event> events;
  final Group group;
  final Function refreshEventsUnseen;
  final Function refreshPage;
  final Function getNextBatch;

  EventsList(
      {Key key,
      this.group,
      this.events,
      this.refreshEventsUnseen,
      this.refreshPage,
      this.getNextBatch})
      : super(key: key);

  @override
  _EventsListState createState() => _EventsListState();
}

class _EventsListState extends State<EventsList> {
  @override
  Widget build(BuildContext context) {
    if (widget.events.isEmpty) {
      return ListView(
        children: <Widget>[
          Padding(
            padding: EdgeInsets.all(20.0),
            child: Center(
              child: Text(
                  "No events found! Click the button below to create one!",
                  style: TextStyle(fontSize: 30)),
            ),
          )
        ],
      );
    } else {
      List<EventCardInterface> eventCards = new List<EventCardInterface>();
      for (String eventId in widget.events.keys) {
        EventCardInterface eventCard;
        int eventMode = EventsManager.getEventMode(widget.events[eventId]);
        if (eventMode == EventsManager.considerMode) {
          eventCard = new EventCardConsider(
              widget.group.groupId,
              widget.events[eventId],
              eventId,
              widget.refreshEventsUnseen,
              widget.refreshPage);
        } else if (eventMode == EventsManager.votingMode) {
          eventCard = new EventCardVoting(
              widget.group.groupId,
              widget.events[eventId],
              eventId,
              widget.refreshEventsUnseen,
              widget.refreshPage);
        } else if (eventMode == EventsManager.occurringMode) {
          eventCard = new EventCardOccurring(
              widget.group.groupId,
              widget.events[eventId],
              eventId,
              widget.refreshEventsUnseen,
              widget.refreshPage);
        } else if (eventMode == EventsManager.closedMode) {
          eventCard = new EventCardClosed(
              widget.group.groupId,
              widget.events[eventId],
              eventId,
              widget.refreshEventsUnseen,
              widget.refreshPage);
        }
        eventCards.add(eventCard);
      }

      eventCards.sort((a, b) {
        if (a.getEventMode() > b.getEventMode()) {
          return -1;
        } else if (b.getEventMode() > a.getEventMode()) {
          return 1;
        } else {
          // cards are the same priority, so sort accordingly (most recent time first)
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

      List<Widget> widgetList = new List<Widget>.from(eventCards);
      int numEvents = (Globals.currentGroupResponse.group.currentBatchNum + 1) *
          GroupsManager.BATCH_SIZE;
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
              padding: EdgeInsets.all(MediaQuery.of(context).size.width * .02),
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

      return Scrollbar(
          child: ListView.builder(
              shrinkWrap: true,
              itemCount: widgetList.length,
              key: Key("events_list:event_list"),
              itemBuilder: (context, index) {
                return widgetList[index];
              }));
    }
  }
}
