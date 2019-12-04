import 'package:flutter/material.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/models/group.dart';
import 'event_card.dart';
import 'package:frontEnd/event_details.dart';
import 'package:frontEnd/group_page.dart';

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
      List<EventCard> eventCards = new List<EventCard>();
      for (String eventId in widget.events.keys) {
        eventCards.add(new EventCard(
            widget.group.groupId, widget.events[eventId], eventId,
            callback: (String groupId, Event event, String eventId) =>
                selectEvent(groupId, event, eventId)));
      }
      return Scrollbar(child: ListView(shrinkWrap: true, children: eventCards));
    }
  }

  void selectEvent(String groupId, Event event, String eventId) {
    Navigator.push(
      context,
      MaterialPageRoute(
          builder: (context) => EventDetails(
                groupId: groupId,
                event: event,
                eventId: eventId,
              )),
    ).then((_) => GroupPage(
          events: widget.events,
        ));
  }
}
