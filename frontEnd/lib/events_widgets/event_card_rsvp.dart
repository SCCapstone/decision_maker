import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/event.dart';

class EventCardRsvp extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;
  final Function callback;

  EventCardRsvp(this.groupId, this.event, this.eventId, {this.callback});

  @override
  _EventCardRsvpState createState() => new _EventCardRsvpState();
}

class _EventCardRsvpState extends State<EventCardRsvp> {
  DateTime proposedTime;
  String pollBeginFormatted;
  String proposedTimeFormatted;

  @override
  void initState() {
    proposedTime = widget.event.eventStartDateTime;
    proposedTimeFormatted = Globals.formatter.format(proposedTime);
    getFormattedTimes();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    getFormattedTimes();
    // TODO if they click the button and it's in a different stage, just refresh
    return Container(
      height: MediaQuery.of(context).size.height * .27,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: <Widget>[
          Text(
            widget.event.eventName,
            style: TextStyle(fontSize: 20),
          ),
          Text(
            "Tenative attendees: ${widget.event.optedIn.length}",
            style: TextStyle(fontSize: 20),
          ),
          Text("Proposed Date: $proposedTimeFormatted"),
          Text("RSVP time ends: $pollBeginFormatted"),
          RaisedButton(
            child: Text("RSVP"),
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

  void getFormattedTimes() {
    DateTime pollBegin = widget.event.createdDateTime
        .add(new Duration(minutes: widget.event.pollDuration));
    pollBeginFormatted = Globals.formatter.format(pollBegin);
  }
}
