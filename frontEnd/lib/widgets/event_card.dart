import 'package:flutter/material.dart';
import 'package:frontEnd/models/event.dart';
import 'package:intl/intl.dart';

class EventCard extends StatefulWidget {
  final Event event;
  final int index;
  final Function callback;
  String mode;

  EventCard(this.event, this.index, {this.callback});

  @override
  _EventCardState createState() => new _EventCardState();
}

class _EventCardState extends State<EventCard> {
  static final String votingMode = "Voting";
  static final String optInMode = "OptIn";
  static final String finishedMode = "Finished";
  static final DateFormat formatter = DateFormat('MM-dd-yyyy – hh:mm');
  DateTime createTime;
  DateTime pollBegin;
  DateTime eventFinished;
  DateTime proposedTime;
  String createTimeFormatted;
  String pollBeginFormatted;
  String eventFinishedFormatted;
  String proposedTimeFormatted;

  @override
  void initState() {
    proposedTime = DateTime.parse(widget.event.eventStartDateTime);
    proposedTimeFormatted = formatter.format(proposedTime);
    updateMode();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    updateMode();
    bool choiceChosen = true;
    return Container(
      height: MediaQuery.of(context).size.height * .27,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: <Widget>[
          Expanded(
            child: Text(
              widget.event.eventName,
              style: TextStyle(fontSize: 20),
            ),
          ),
          Expanded(
            child: Visibility(
              visible: choiceChosen,
              child: Text(
                "Chosen Choice: Sushi"
              ),
            ),
          ),
          Expanded(
            child: Text(
              "Total attendees: ${widget.event.optedIn.length}",
              style: TextStyle(fontSize: 20),
            ),
          ),
          Expanded(
            child: Text("Proposed Date: $proposedTimeFormatted"),
          ),
          Expanded(
            child: Text("Opt in in time ends: $pollBeginFormatted"),
          ),
          RaisedButton(
            child: Text("Respond"),
            color: Colors.lightGreenAccent,
            onPressed: () {
              widget.callback(widget.event, widget.mode);
            },
          )
        ],
      ),
      decoration:
      new BoxDecoration(border: new Border(bottom: new BorderSide())),
    );



    if (widget.mode == optInMode) {
      return Container(
        height: MediaQuery.of(context).size.height * .27,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: <Widget>[
            Expanded(
              child: Text(
                widget.event.eventName,
                style: TextStyle(fontSize: 20),
              ),
            ),
            Expanded(
              child: Text(
                "Total attendees: ${widget.event.optedIn.length}",
                style: TextStyle(fontSize: 20),
              ),
            ),
            Expanded(
              child: Text("Proposed Date: $proposedTimeFormatted"),
            ),
            Expanded(
              child: Text("Opt in in time ends: $pollBeginFormatted"),
            ),
            RaisedButton(
              child: Text("Respond"),
              color: Colors.lightGreenAccent,
              onPressed: () {
                widget.callback(widget.event, widget.mode);
              },
            )
          ],
        ),
        decoration:
            new BoxDecoration(border: new Border(bottom: new BorderSide())),
      );
    } else if(widget.mode == votingMode){
      return Container(
        height: MediaQuery.of(context).size.height * .27,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: <Widget>[
            Expanded(
              child: Text(
                widget.event.eventName,
                style: TextStyle(fontSize: 20),
              ),
            ),
            Expanded(
              child: Text(
                "Total attendees: ${widget.event.optedIn.length}",
                style: TextStyle(fontSize: 20),
              ),
            ),
            Expanded(
              child: Text("Proposed Date: $proposedTimeFormatted"),
            ),
            Expanded(
              child: Text("Opt in in time ends: $pollBeginFormatted"),
            ),
            RaisedButton(
              child: Text("Vote"),
              color: Colors.lightGreenAccent,
              onPressed: () {
                widget.callback(widget.event, widget.mode);
              },
            )
          ],
        ),
        decoration:
        new BoxDecoration(border: new Border(bottom: new BorderSide())),
      );
    }
    else{
      return Container(
        height: MediaQuery.of(context).size.height * .27,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: <Widget>[
            Expanded(
              child: Text(
                widget.event.eventName,
                style: TextStyle(fontSize: 20),
              ),
            ),
            Expanded(
              child: Text(
                "Total attendees: ${widget.event.optedIn.length}",
                style: TextStyle(fontSize: 20),
              ),
            ),
            Expanded(
              child: Text("Proposed Date: $proposedTimeFormatted"),
            ),
            Expanded(
              child: Text("Opt in in time ends: $pollBeginFormatted"),
            ),
            RaisedButton(
              child: Text("View Results"),
              color: Colors.lightGreenAccent,
              onPressed: () {
                widget.callback(widget.event, widget.mode);
              },
            )
          ],
        ),
        decoration:
        new BoxDecoration(border: new Border(bottom: new BorderSide())),
      );
    }
  }

  void updateMode() {
    // must be outside of init state in case the user navigates throughout the app.
    createTime = DateTime.parse(widget.event.createdDateTime);
    DateTime timeNow =
        DateTime.parse(widget.event.createdDateTime); // TODO change back to now
//    timeNow = timeNow.add(new Duration(minutes: 19));
    pollBegin =
        createTime.add(new Duration(minutes: widget.event.pollDuration));
    print(pollBegin);
    print("Current time $timeNow");
    eventFinished =
        createTime.add(new Duration(minutes: (widget.event.pollDuration) * 2));
    print("Event finished $eventFinished");
    if (timeNow.isBefore(pollBegin)) {
      widget.mode = optInMode;
    } else if (timeNow.isAfter(pollBegin) && timeNow.isBefore(eventFinished)) {
      widget.mode = votingMode;
    } else {
      widget.mode = finishedMode;
    }
    print(widget.mode);
    createTimeFormatted = formatter.format(createTime);
    pollBeginFormatted = formatter.format(pollBegin);
    DateTime eventProposed = DateTime.parse(widget.event.eventStartDateTime);
    eventFinishedFormatted = formatter.format(eventProposed);
  }
}
