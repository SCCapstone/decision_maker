import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_details_occurring.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/utilities/utilities.dart';

class EventCardOccurring extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;

  EventCardOccurring(this.groupId, this.event, this.eventId);

  @override
  _EventCardOccurringState createState() => new _EventCardOccurringState();
}

class _EventCardOccurringState extends State<EventCardOccurring> {
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
          Text(
              (DateTime.now().isBefore(widget.event.eventStartDateTime))
                  ? "Event starts: ${widget.event.eventStartDateTimeFormatted}"
                  : "Started at: ${widget.event.eventStartDateTimeFormatted}",
              style: TextStyle(fontSize: 20)),
          Text("Selected Choice: ${widget.event.selectedChoice}",
              style: TextStyle(fontSize: 20)),
          Text("Total considered: ${widget.event.optedIn.length}",
              style: TextStyle(fontSize: 20)),
          RaisedButton(
            child: Text("View Results"),
            color: Colors.green,
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (context) => EventDetailsOccurring(
                        groupId: widget.groupId,
                        eventId: widget.eventId,
                        mode: EventsManager.occurringMode)),
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
