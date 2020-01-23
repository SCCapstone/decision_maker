import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/widgets/user_row_events.dart';

class EventDetailsVoting extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;
  final List<Widget> userRows = new List<Widget>();

  EventDetailsVoting({Key key, this.groupId, this.event, this.eventId})
      : super(key: key);

  @override
  _EventDetailsVotingState createState() => new _EventDetailsVotingState();
}

class _EventDetailsVotingState extends State<EventDetailsVoting> {
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
                        "Date and Time",
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
                      child: Text("Poll Time Ends",
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.8,
                          )),
                    ),
                    Text(pollFinishedFormatted,
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.7)),
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
                              DefaultTextStyle.of(context).style.fontSize *
                                  0.7),
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                    ),
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
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                    ),
                    Container(
                      height: MediaQuery.of(context).size.height * .27,
                      width: MediaQuery.of(context).size.width * .95,
                      child: Scrollbar(
                        child: PageView(
                          scrollDirection: Axis.horizontal,
                          children: <Widget>[
                            Container(
                              width: MediaQuery.of(context).size.width * .90,
                              color: Colors.red,
                            ),
                            Container(
                              width: MediaQuery.of(context).size.width,
                              color: Colors.blue,
                            )
                          ],
                        ),
                      ),
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: <Widget>[
                        IconButton(
                          icon: Icon(Icons.thumb_down),
                          color: Colors.red,
                          onPressed: () {
                            GroupsManager.optInOutOfEvent(
                                widget.groupId, widget.eventId, false, context);
                          },
                        ),
                        IconButton(
                          icon: Icon(Icons.thumb_up),
                          color: Colors.green,
                          onPressed: () {
                            GroupsManager.optInOutOfEvent(
                                widget.groupId, widget.eventId, true, context);
                          },
                        )
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
