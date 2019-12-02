import 'package:flutter/material.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/widgets/user_row_events.dart';

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
  bool closed = false;

  @override
  void initState() {
    print(widget.event);
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
      buttonQuestion = "Vote for the proposed choice";
      buttonConfirm = "Yes";
      buttonDenial = "No";
    } else if (widget.event.mode == EventsManager.pollFinishedMode) {
      finished = true;
      optIn = false;
      voting = false;
      buttonQuestion = "Will you be attending this event?";
      buttonConfirm = "Yes";
      buttonDenial = "No";
    } else {
      closed = true;
      finished = false;
      optIn = false;
      voting = false;
    }
    getFormattedTimes();
    return Scaffold(
      appBar: new AppBar(
        centerTitle: true,
        title: Text(
          widget.event.eventName,
          style: TextStyle(
              fontSize: DefaultTextStyle.of(context).style.fontSize * 0.6),
        ),
        leading: BackButton(),
      ),
      body: ListView(
        shrinkWrap: true,
        children: <Widget>[
          Column(
            children: <Widget>[
              Column(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: <Widget>[
                  Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .01),
                    child: Text(
                      closed ? "Occurred" : "Date and Time",
                      style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize:
                              DefaultTextStyle.of(context).style.fontSize *
                                  0.8),
                    ),
                  ),
                  Text(
                    Globals.formatter.format(widget.event.eventStartDateTime),
                    style: TextStyle(
                        fontSize:
                            DefaultTextStyle.of(context).style.fontSize * 0.7),
                  ),
                  Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .01),
                    child: Text(
                      "Chosen Category",
                      style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize:
                              DefaultTextStyle.of(context).style.fontSize *
                                  0.8),
                    ),
                  ),
                  Text(
                    widget.event.categoryName,
                    style: TextStyle(
                        fontSize:
                            DefaultTextStyle.of(context).style.fontSize * 0.7),
                  ),
                  Visibility(
                    visible: optIn,
                    child: Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                      child: Text("Opt-In Time Ends",
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.8,
                          )),
                    ),
                  ),
                  Visibility(
                    visible: optIn,
                    child: Text(pollBeginFormatted,
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.7)),
                  ),
                  Visibility(
                    visible: voting,
                    child: Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                      child: Text("Poll Ends",
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.8,
                          )),
                    ),
                  ),
                  Visibility(
                    visible: voting,
                    child: Text(pollFinishedFormatted,
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.7)),
                  ),
                  Visibility(
                    visible: voting,
                    child: Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                      child: Text("Proposed Choice",
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.8,
                          )),
                    ),
                  ),
                  Visibility(
                    visible: voting,
                    // for now just putting a hard coded value here
                    child: Text(widget.event.selectedChoice,
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.7)),
                  ),
                  Visibility(
                    visible: (finished || closed),
                    child: Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                      child: Text("Selected Choice",
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.8,
                          )),
                    ),
                  ),
                  Visibility(
                    visible: (finished || closed),
                    child: Text(widget.event.selectedChoice,
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.7)),
                  ),
                  Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .01),
                  ),
                  Text("Event created by: $eventCreator",
                      style: TextStyle(
                          fontSize:
                              DefaultTextStyle.of(context).style.fontSize *
                                  0.5)),
                  ExpansionTile(
                    title: Text("Attendees (${widget.event.optedIn.length})"),
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
                        MediaQuery.of(context).size.height * .01),
                  ),
                  Visibility(
                    visible: (optIn || finished || voting),
                    child: Text(buttonQuestion,
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.4)),
                  ),
                  Visibility(
                    visible: (optIn || finished || voting),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: <Widget>[
                        RaisedButton(
                          child: Text(buttonDenial),
                          color: Colors.red,
                          onPressed: () {
                            // TODO opt in if in that stage (https://github.com/SCCapstone/decision_maker/issues/45)
                          },
                        ),
                        RaisedButton(
                          child: Text(buttonConfirm),
                          color: Colors.green,
                          onPressed: () {
                            // TODO opt in if in that stage (https://github.com/SCCapstone/decision_maker/issues/45)
                          },
                        )
                      ],
                    ),
                  )
                ],
              ),
            ],
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
