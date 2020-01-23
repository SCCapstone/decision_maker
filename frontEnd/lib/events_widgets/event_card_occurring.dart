import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/event.dart';

class EventCardOccurring extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;
  final Function callback;

  EventCardOccurring(this.groupId, this.event, this.eventId, {this.callback});

  @override
  _EventCardOccurringState createState() => new _EventCardOccurringState();
}

class _EventCardOccurringState extends State<EventCardOccurring> {
  DateTime proposedTime;
  String pollBeginFormatted;
  String proposedTimeFormatted;

  @override
  void initState() {
    proposedTime = widget.event.eventStartDateTime;
    proposedTimeFormatted = Globals.formatter.format(proposedTime);
    super.initState();
  }

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
          Text("Time: $proposedTimeFormatted", style: TextStyle(fontSize: 20)),
          Text("Selected Choice: ${widget.event.selectedChoice}",
              style: TextStyle(fontSize: 20)),
          Text("Total attendees: ${widget.event.optedIn.length}",
              style: TextStyle(fontSize: 20)),
          RaisedButton(
            child: Text("View Results"),
            color: Colors.lightGreenAccent,
            onPressed: () {
              widget.callback(widget.groupId, widget.event, widget.eventId);
            },
          )
        ],
      ),
      decoration:
          new BoxDecoration(border: new Border(bottom: new BorderSide())),
    );
  }
}
