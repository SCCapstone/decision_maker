import 'package:flutter/material.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/widgets/user_row_events.dart';

class EventDetailsRsvp extends StatefulWidget {
  final String groupId;
  final String eventId;
  final String mode;

  EventDetailsRsvp({Key key, this.groupId, this.eventId, this.mode})
      : super(key: key);

  @override
  _EventDetailsRsvpState createState() => new _EventDetailsRsvpState();
}

class _EventDetailsRsvpState extends State<EventDetailsRsvp> {
  Map<String, UserRowEvents> userRows = new Map<String, UserRowEvents>();
  String eventCreator = "";
  Event event;

  @override
  void initState() {
    getEvent();
    for (String displayname in event.eventCreator.keys) {
      eventCreator = event.eventCreator[displayname];
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        centerTitle: true,
        title: Text(
          event.eventName,
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
                        "Proposed Time",
                        style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.8),
                      ),
                    ),
                    Text(
                      Globals.formatter.format(event.eventStartDateTime),
                      style: TextStyle(
                          fontSize:
                              DefaultTextStyle.of(context).style.fontSize *
                                  0.7),
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                      child: Text("RSVP Time Ends",
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.8,
                          )),
                    ),
                    Text(event.pollBeginFormatted,
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.7)),
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
                      event.categoryName,
                      style: TextStyle(
                          fontSize:
                              DefaultTextStyle.of(context).style.fontSize *
                                  0.7),
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
                      title: Text("Attendees (${userRows.length})"),
                      children: <Widget>[
                        SizedBox(
                          height: MediaQuery.of(context).size.height * .2,
                          child: ListView(
                            shrinkWrap: true,
                            children: userRows.values.toList(),
                          ),
                        ),
                      ],
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                    ),
                    Text("RSVP",
                        style: TextStyle(
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.4)),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: <Widget>[
                        RaisedButton(
                          child: Text("Not going"),
                          color: Colors.red,
                          onPressed: () {
                            tryRsvp(false);
                            userRows.remove(Globals.username);
                            setState(() {});
                          },
                        ),
                        RaisedButton(
                          child: Text("Going"),
                          color: Colors.green,
                          onPressed: () {
                            tryRsvp(true);
                            userRows.putIfAbsent(
                                Globals.username,
                                () => UserRowEvents(Globals.user.displayName,
                                    Globals.username, Globals.user.icon));
                            setState(() {});
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

  void tryRsvp(bool going) async {
    // not sure if we want a loading dialog for this as that would be annoying for the user. Only show error for now
    ResultStatus resultStatus = await GroupsManager.optInOutOfEvent(
        widget.groupId, widget.eventId, going);
    if (!resultStatus.success) {
      showErrorMessage("Error", resultStatus.errorMessage, context);
    }
  }

  void getEvent() {
    Map<String, Event> events =
        GroupsManager.getGroupEvents(Globals.currentGroup);
    event = events[widget.eventId];

    userRows.clear();
    for (String username in event.optedIn.keys) {
      userRows.putIfAbsent(
          username,
          () => UserRowEvents(
              event.optedIn[username][UsersManager.DISPLAY_NAME],
              username,
              event.optedIn[username][UsersManager.ICON]));
    }
  }

  Future<Null> refreshList() async {
    List<String> groupId = new List<String>();
    groupId.add(widget.groupId);
    ResultStatus<List<Group>> resultStatus =
        await GroupsManager.getGroups(groupIds: groupId);
    if (resultStatus.success) {
      Globals.currentGroup = resultStatus.data.first;
      getEvent();
      if (EventsManager.getEventMode(event) != widget.mode) {
        // if while the user was here and the mode changed, take them back to the group page
        Navigator.of(context).pop();
      }
    } else {
      showErrorMessage("Error", resultStatus.errorMessage, context);
    }
    setState(() {});
  }
}
