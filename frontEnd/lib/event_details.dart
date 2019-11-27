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
  @override
  void initState() {
    EventsManager.updateEventMode(widget
        .event); // make this call in case now the event is in a different stage
    for (String username in widget.event.optedIn.keys) {
      widget.userRows.add(UserRow(widget.event.optedIn[username], username));
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
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
      body: Scrollbar(
        child: Center(
          child: Column(
            children: <Widget>[
              Padding(
                padding:
                    EdgeInsets.all(MediaQuery.of(context).size.height * .015),
              ),
              Container(
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
                      child: Text("Poll Ends:",
                          style: TextStyle(
                              fontSize:
                                  DefaultTextStyle.of(context).style.fontSize *
                                      0.7,
                              backgroundColor:
                                  Colors.lightGreen.withOpacity(0.7))),
                    ),
                    Visibility(
                      child: Text("Date here",
                          style: TextStyle(
                              fontSize:
                                  DefaultTextStyle.of(context).style.fontSize *
                                      0.8)),
                    ),
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
                    )
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
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
