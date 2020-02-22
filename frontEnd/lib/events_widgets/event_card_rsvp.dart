import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_details_rsvp.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/utilities/utilities.dart';

class EventCardRsvp extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;

  EventCardRsvp(this.groupId, this.event, this.eventId);

  @override
  _EventCardRsvpState createState() => new _EventCardRsvpState();
}

class _EventCardRsvpState extends State<EventCardRsvp> {
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
          Text("Proposed Date: ${widget.event.eventStartDateTimeFormatted}",
              style: TextStyle(fontSize: 20)),
          Text("RSVP By: ${widget.event.pollBeginFormatted}",
              style: TextStyle(fontSize: 20)),
          Text(
            "Tenative attendees: ${widget.event.optedIn.length}",
            style: TextStyle(fontSize: 20),
          ),
          RaisedButton(
            child: Text("RSVP"),
            color: Colors.green,
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (context) => EventDetailsRsvp(
                        groupId: widget.groupId, eventId: widget.eventId)),
              );
            },
          )
        ],
      ),
      decoration: new BoxDecoration(
          border: new Border(bottom: new BorderSide(color: getBorderColor()))),
    );
  }
}
