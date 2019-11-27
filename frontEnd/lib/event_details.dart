import 'package:flutter/material.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/event.dart';

class EventDetails extends StatefulWidget {
  final Event event;
  final String mode;
  final List<Widget> userRows = new List<Widget>();

  EventDetails({Key key, this.event, this.mode}) : super(key: key);

  @override
  _EventDetailsState createState() => new _EventDetailsState();
}

class _EventDetailsState extends State<EventDetails> {
  DateTime createTime;
  DateTime pollBegin;
  DateTime pollFinished;
  DateTime proposedTime;
  String pollBeginFormatted;
  String pollFinishedFormatted;
  String eventCreator = "";
  String buttonQuestion = "";
  String buttonDenial = "";
  String buttonConfirm = "";
  bool optIn = false;
  bool voting = false;
  bool finished = false;

  @override
  void initState() {
    createTime = widget.event.createdDateTime;
    proposedTime = widget.event.eventStartDateTime;
    EventsManager.updateEventMode(widget
        .event); // make this call in case now the event is in a different stage
    for (String username in widget.event.optedIn.keys) {
      widget.userRows.add(UserRow(widget.event.optedIn[username], username));
    }
    for (String username in widget.event.eventCreator.keys) {
      eventCreator = widget.event.eventCreator[username];
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    // check what stage the event is in to display appropriate widgets
    if (widget.event.mode == EventsManager.optInMode) {
      optIn = true;
      voting = false;
      finished = false;
      buttonQuestion = "Choose to opt-in or opt-out of this event";
      buttonConfirm = "Opt-In";
      buttonDenial = "Opt-Out";
    } else if (widget.event.mode == EventsManager.votingMode) {
      voting = true;
      optIn = false;
      finished = false;
      buttonQuestion = "Vote for the proposed choice:";
      buttonConfirm = "Yes";
      buttonDenial = "No";
    } else {
      finished = true;
      optIn = false;
      voting = false;
      buttonQuestion = "Will you be attending this event?";
      buttonConfirm = "Yes";
      buttonDenial = "No";
    }
    getFormattedTimes();
    return new Scaffold(
      appBar: new AppBar(
        centerTitle: true,
        title: Text(
          widget.event.eventName,
          style: TextStyle(
              fontSize: DefaultTextStyle.of(context).style.fontSize * 0.6),
        ),
        leading: BackButton(),
      ),
      body: Column(
        children: <Widget>[
          Padding(
            padding: EdgeInsets.all(MediaQuery.of(context).size.height * .015),
          ),
          Expanded(
            child: Container(
              height: MediaQuery.of(context).size.height * .70,
              child: Column(
                children: <Widget>[
                  Text(
                    "Date and Time:",
                    style: TextStyle(
                        fontSize:
                            DefaultTextStyle.of(context).style.fontSize * 0.7,
                        backgroundColor: Colors.lightGreen.withOpacity(0.7)),
                  ),
                  Text(
                    Globals.formatter.format(widget.event.eventStartDateTime),
                    style: TextStyle(
                        fontSize:
                            DefaultTextStyle.of(context).style.fontSize * 0.8),
                  ),
                  Text(
                    "Chosen Category:",
                    style: TextStyle(
                        backgroundColor: Colors.lightGreen.withOpacity(0.7),
                        fontSize:
                            DefaultTextStyle.of(context).style.fontSize * 0.7),
                  ),
                  Text(
                    widget.event.categoryName,
                    style: TextStyle(
                        fontSize:
                            DefaultTextStyle.of(context).style.fontSize * 0.8),
                  ),
                  Visibility(
                    visible: optIn,
                    child: Text("Opt-In Time Ends:",
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.7,
                            backgroundColor:
                                Colors.lightGreen.withOpacity(0.7))),
                  ),
                  Visibility(
                    visible: optIn,
                    child: Text(pollBeginFormatted,
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.8)),
                  ),
                  Visibility(
                    visible: voting,
                    child: Text("Poll Ends:",
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.7,
                            backgroundColor:
                                Colors.lightGreen.withOpacity(0.7))),
                  ),
                  Visibility(
                    visible: voting,
                    child: Text(pollFinishedFormatted,
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.8)),
                  ),
                  Visibility(
                    visible: voting,
                    child: Text("Proposed Choice:",
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.7,
                            backgroundColor:
                                Colors.lightGreen.withOpacity(0.7))),
                  ),
                  Visibility(
                    visible: voting,
                    // for now just putting a hard coded value here
                    child: Text(widget.event.selectedChoice,
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.8)),
                  ),
                  Visibility(
                    visible: finished,
                    child: Text("Selected Choice:",
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.7,
                            backgroundColor:
                                Colors.lightGreen.withOpacity(0.7))),
                  ),
                  Visibility(
                    visible: finished,
                    child: Text(widget.event.selectedChoice,
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.8)),
                  ),
                  Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .015),
                  ),
                  Text("Event created by: $eventCreator",
                      style: TextStyle(
                          fontSize:
                              DefaultTextStyle.of(context).style.fontSize *
                                  0.5)),
                  ExpansionTile(
                    title: Text(
                        "Current Attendees (${widget.event.optedIn.length})"),
                    children: <Widget>[
                      SizedBox(
                        height: MediaQuery.of(context).size.height * .2,
                        child: ListView(
                          shrinkWrap: true,
                          children: widget.userRows,
                        ),
                      ),
                    ],
                  ),
                  Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .015),
                  ),
                  Text(buttonQuestion,
                      style: TextStyle(
                          fontSize:
                              DefaultTextStyle.of(context).style.fontSize *
                                  0.4)),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: <Widget>[
                      RaisedButton(
                        child: Text(buttonDenial),
                        color: Colors.red,
                        onPressed: () {},
                      ),
                      RaisedButton(
                        child: Text(buttonConfirm),
                        color: Colors.green,
                        onPressed: () {},
                      )
                    ],
                  )
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  void getFormattedTimes() {
    pollBegin =
        createTime.add(new Duration(minutes: widget.event.pollDuration));
    pollFinished =
        createTime.add(new Duration(minutes: (widget.event.pollDuration) * 2));
    pollBeginFormatted = Globals.formatter.format(pollBegin);
    pollFinishedFormatted = Globals.formatter.format(pollFinished);
  }
}

class UserRow extends StatelessWidget {
  final String displayName;
  final String username;

  UserRow(this.displayName, this.username);

  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * .07,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Expanded(
            child: Center(
              child: Text(
                this.displayName,
                style: TextStyle(fontSize: 20),
              ),
            ),
          ),
        ],
      ),
      decoration:
          new BoxDecoration(border: new Border(bottom: new BorderSide())),
    );
  }
}