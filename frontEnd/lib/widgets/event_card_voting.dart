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
  DateTime pollBegin;
  DateTime pollFinished;
  DateTime proposedTime;
  String createTimeFormatted;
  String pollBeginFormatted;
  String pollFinishedFormatted;
  String proposedTimeFormatted;
  String buttonText;
  bool optIn = false;
  bool voting = false;
  bool finished = false;
  bool closed = false;

  @override
  void initState() {
    proposedTime = widget.event.eventStartDateTime;
    proposedTimeFormatted = Globals.formatter.format(proposedTime);
    getFormattedTimes();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    widget.event.mode = EventsManager.updateEventMode(
        widget.event); // needed here for the refresh
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
          Visibility(
            visible: (voting || finished),
            child: Text("Chosen Choice: ${widget.event.selectedChoice}"),
          ),
          Text(
            "Total attendees: ${widget.event.optedIn.length}",
            style: TextStyle(fontSize: 20),
          ),
          Text((voting || optIn || finished)
              ? "Proposed Date: $proposedTimeFormatted"
              : "Occurred: $proposedTimeFormatted"),
          Visibility(
            visible: (optIn),
            child: Text("Opt in in time ends: $pollBeginFormatted"),
          ),
          Visibility(
            visible: (voting),
            child: Text("Voting time ends: $pollFinishedFormatted"),
          ),
          RaisedButton(
            child: Text(buttonText),
            color: (voting || finished || optIn)
                ? Colors.lightGreenAccent
                : Colors.grey,
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
    createTimeFormatted =
        Globals.formatter.format(widget.event.createdDateTime);
    pollBegin = widget.event.createdDateTime
        .add(new Duration(minutes: widget.event.pollDuration));
    pollFinished = widget.event.createdDateTime
        .add(new Duration(minutes: (widget.event.pollDuration) * 2));
    pollBeginFormatted = Globals.formatter.format(pollBegin);
    pollFinishedFormatted = Globals.formatter.format(pollFinished);
  }
}
