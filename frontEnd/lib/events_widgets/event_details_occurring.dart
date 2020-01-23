import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/widgets/user_row_events.dart';

class EventDetailsOccurring extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;
  final List<Widget> userRows = new List<Widget>();

  EventDetailsOccurring({Key key, this.groupId, this.event, this.eventId})
      : super(key: key);

  @override
  _EventDetailsOccurringState createState() =>
      new _EventDetailsOccurringState();
}

class _EventDetailsOccurringState extends State<EventDetailsOccurring> {
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

  @override
  void initState() {
    createTime = widget.event.createdDateTime;
    proposedTime = widget.event.eventStartDateTime;
    for (String username in widget.event.optedIn.keys) {
      widget.userRows.add(UserRow(widget.event.optedIn[username]));
    }
    for (String username in widget.event.eventCreator.keys) {
      eventCreator = widget.event.eventCreator[username];
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    // check what stage the event is in to display appropriate widgets
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
      body: RefreshIndicator(
        onRefresh: refreshList,
        child: ListView(
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
                        "Event Time",
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
                              DefaultTextStyle.of(context).style.fontSize *
                                  0.7),
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                      child: Text(
                        "Category",
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
                              DefaultTextStyle.of(context).style.fontSize *
                                  0.7),
                    ),
                    Padding(
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
                    Text(widget.event.selectedChoice,
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.7)),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                    ),
                    Text("Event created by: $eventCreator",
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.3)),
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
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Future<Null> refreshList() async {
    await Future.delayed(
        Duration(milliseconds: 70)); // required to remove the loading animation
    setState(() {});
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