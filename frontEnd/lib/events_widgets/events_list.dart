import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_details_closed.dart';
import 'package:frontEnd/events_widgets/event_details_occurring.dart';
import 'package:frontEnd/events_widgets/event_details_rsvp.dart';
import 'package:frontEnd/events_widgets/event_details_voting.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/events_widgets/event_card_closed.dart';
import 'package:frontEnd/events_widgets/event_card_occurring.dart';
import 'package:frontEnd/events_widgets/event_card_rsvp.dart';
import 'package:frontEnd/events_widgets/event_card_voting.dart';
import 'package:frontEnd/groups_widgets/group_page.dart';

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
      return Center(
        child: Text("No events found! Click the button below to create one!",
            style: TextStyle(fontSize: 30)),
      );
    } else {
      List<Widget> eventCards = new List<Widget>();
      for (String eventId in widget.events.keys) {
        Widget eventCard;
        String eventMode = EventsManager.getEventMode(widget.events[eventId]);
        if (eventMode == EventsManager.optInMode) {
          eventCard = new EventCardRsvp(
              widget.group.groupId, widget.events[eventId], eventId,
              callback: (String groupId, Event event, String eventId) =>
                  selectEvent(groupId, event, eventId));
        } else if (eventMode == EventsManager.votingMode) {
          eventCard = new EventCardVoting(
              widget.group.groupId, widget.events[eventId], eventId,
              callback: (String groupId, Event event, String eventId) =>
                  selectEvent(groupId, event, eventId));
        } else if (eventMode == EventsManager.pollFinishedMode) {
          eventCard = new EventCardOccurring(
              widget.group.groupId, widget.events[eventId], eventId,
              callback: (String groupId, Event event, String eventId) =>
                  selectEvent(groupId, event, eventId));
        } else if (eventMode == EventsManager.eventClosedMode) {
          eventCard = new EventCardClosed(
              widget.group.groupId, widget.events[eventId], eventId,
              callback: (String groupId, Event event, String eventId) =>
                  selectEvent(groupId, event, eventId));
        }
        eventCards.add(eventCard);
      }
      return Scrollbar(child: ListView(shrinkWrap: true, children: eventCards));
    }
  }

  void selectEvent(String groupId, Event event, String eventId) {
    String eventMode = EventsManager.getEventMode(widget.events[eventId]);
    Widget newPage;
    if (eventMode == EventsManager.optInMode) {
      newPage = new EventDetailsRsvp(
          groupId: groupId, event: event, eventId: eventId);
    } else if (eventMode == EventsManager.votingMode) {
      newPage = new EventDetailsVoting(
          groupId: groupId, event: event, eventId: eventId);
    } else if (eventMode == EventsManager.pollFinishedMode) {
      newPage = new EventDetailsOccurring(
          groupId: groupId, event: event, eventId: eventId);
    } else if (eventMode == EventsManager.eventClosedMode) {
      newPage = new EventDetailsClosed(
          groupId: groupId, event: event, eventId: eventId);
    }
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => newPage),
    ).then((_) => GroupPage(
          events: widget.events,
        ));
  }
}
