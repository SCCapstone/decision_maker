import 'package:flutter/material.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/models/group.dart';
import 'event_card.dart';
import 'package:frontEnd/event_details.dart';
import 'package:frontEnd/group_page.dart';

class EventsList extends StatefulWidget {
  final List<Event> events;
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
            style: TextStyle(
                fontSize: DefaultTextStyle.of(context).style.fontSize * 0.5)),
      );
    } else {
      return Scrollbar(
        child: ListView.builder(
          shrinkWrap: true,
          itemCount: widget.events.length,
          itemBuilder: (context, index) {
            return EventCard(
              widget.events[index],
              index,
              callback: (event, mode) => selectEvent(event, mode),
            );
          },
        ),
      );
    }
  }

  void selectEvent(Event event, String mode) {
    Navigator.push(
      context,
      MaterialPageRoute(
          builder: (context) => EventDetails(
                event: event,
                mode: mode,
              )),
    ).then((_) => GroupPage(
          group: widget.group,
          events: widget.events,
        ));
  }
}
