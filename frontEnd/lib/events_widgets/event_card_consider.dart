import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_details_consider.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/utilities/utilities.dart';

class EventCardConsider extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;

  EventCardConsider(this.groupId, this.event, this.eventId);

  @override
  _EventCardConsiderState createState() => new _EventCardConsiderState();
}

class _EventCardConsiderState extends State<EventCardConsider> {
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
          Text("Consider By: ${widget.event.pollBeginFormatted}",
              style: TextStyle(fontSize: 20)),
          Text(
            "Members considered: ${widget.event.optedIn.length}",
            style: TextStyle(fontSize: 20),
          ),
          RaisedButton(
            child: Text("Consider"),
            color: Colors.green,
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (context) => EventDetailsConsider(
                        groupId: widget.groupId,
                        eventId: widget.eventId,
                        mode: EventsManager.considerMode)),
              );
            },
          )
        ],
      ),
      decoration: BoxDecoration(
          border: Border(bottom: BorderSide(color: getBorderColor()))),
    );
  }
}
