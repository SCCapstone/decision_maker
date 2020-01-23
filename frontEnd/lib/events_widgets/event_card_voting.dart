import 'package:flutter/material.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/event.dart';

class EventCardVoting extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;
  final Function callback;

  EventCardVoting(this.groupId, this.event, this.eventId, {this.callback});

  @override
  _EventCardVotingState createState() => new _EventCardVotingState();
}

class _EventCardVotingState extends State<EventCardVoting> {
  DateTime proposedTime;
  String pollFinishedFormatted;
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
    return Container(
      height: MediaQuery.of(context).size.height * .27,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: <Widget>[
          Text(
            widget.event.eventName,
            style: TextStyle(fontSize: 20),
          ),
          Text("Selected Choices (Swipe to View)"),
          Text(
            "Total attendees: ${widget.event.optedIn.length}",
            style: TextStyle(fontSize: 20),
          ),
          Text("Proposed Date: $proposedTimeFormatted"),
          Text("Voting time ends: $pollFinishedFormatted"),
          RaisedButton(
            child: Text("Vote"),
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
    DateTime pollFinished = widget.event.createdDateTime
        .add(new Duration(minutes: (widget.event.pollDuration) * 2));
    pollFinishedFormatted = Globals.formatter.format(pollFinished);
  }
}
