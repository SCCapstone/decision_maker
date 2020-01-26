import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_details_closed.dart';
import 'package:frontEnd/models/event.dart';

class EventCardClosed extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;

  EventCardClosed(this.groupId, this.event, this.eventId);

  @override
  _EventCardClosedState createState() => new _EventCardClosedState();
}

class _EventCardClosedState extends State<EventCardClosed> {

  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * .27,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: <Widget>[
          Text(
            widget.event.eventName,
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
          ),
          Text("Occurred: ${widget.event.eventStartDateTimeFormatted}",
              style: TextStyle(fontSize: 20)),
          Text("Selected Choice: ${widget.event.selectedChoice}",
              style: TextStyle(fontSize: 20)),
          Text(
            "Total attendees: ${widget.event.optedIn.length}",
            style: TextStyle(fontSize: 20),
          ),
          RaisedButton(
            child: Text("View Results"),
            color: Colors.grey,
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (context) => EventDetailsClosed(
                        groupId: widget.groupId,
                        event: widget.event,
                        eventId: widget.eventId)),
              );
            },
          )
        ],
      ),
      decoration:
          new BoxDecoration(border: new Border(bottom: new BorderSide())),
    );
  }
}
