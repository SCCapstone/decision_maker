import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_card_closed.dart';
import 'package:frontEnd/events_widgets/event_card_occurring.dart';
import 'package:frontEnd/events_widgets/event_card_rsvp.dart';
import 'package:frontEnd/events_widgets/event_card_voting.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/models/group.dart';

class EventsList extends StatefulWidget {
  final Map<String, Event> events;
  final Group group;

  EventsList({Key key, this.group, this.events}) : super(key: key);

  @override
  _EventsListState createState() => _EventsListState();
}

class _EventsListState extends State<EventsList> {
  @override
  Widget build(BuildContext context) {
    if (widget.events.length == 0) {
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
      List<Widget> eventCards = new List<Widget>();
      for (String eventId in widget.events.keys) {
        Widget eventCard;
        String eventMode = EventsManager.getEventMode(widget.events[eventId]);
        if (eventMode == EventsManager.rsvpMode) {
          eventCard = new EventCardRsvp(
              widget.group.groupId, widget.events[eventId], eventId);
        } else if (eventMode == EventsManager.votingMode) {
          eventCard = new EventCardVoting(
              widget.group.groupId, widget.events[eventId], eventId);
        } else if (eventMode == EventsManager.occurringMode) {
          eventCard = new EventCardOccurring(
              widget.group.groupId, widget.events[eventId], eventId);
        } else if (eventMode == EventsManager.closedMode) {
          eventCard = new EventCardClosed(
              widget.group.groupId, widget.events[eventId], eventId);
        }
        eventCards.add(eventCard);
      }
      return Scrollbar(child: ListView(shrinkWrap: true, children: eventCards));
    }
  }
}
