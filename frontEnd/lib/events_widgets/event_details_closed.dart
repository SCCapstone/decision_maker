import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/widgets/user_row_events.dart';

class EventDetailsClosed extends StatefulWidget {
  final String groupId;
  final String eventId;

  EventDetailsClosed({Key key, this.groupId, this.eventId}) : super(key: key);

  @override
  _EventDetailsClosedState createState() => new _EventDetailsClosedState();
}

class _EventDetailsClosedState extends State<EventDetailsClosed> {
  String eventCreator = "";
  List<Widget> userRows = new List<Widget>();
  Event event;

  @override
  void initState() {
    getEvent();
    for (String username in event.eventCreator.keys) {
      eventCreator = event.eventCreator[username];
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    // check what stage the event is in to display appropriate widgets
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
                        "Occurred",
                        style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.8),
                      ),
                    ),
                    Text(
                      event.eventStartDateTimeFormatted,
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
                      event.categoryName,
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
                    Text(event.selectedChoice,
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
                      title: Text("Attendees (${event.optedIn.length})"),
                      children: <Widget>[
                        SizedBox(
                          height: MediaQuery.of(context).size.height * .2,
                          child: ListView(
                            shrinkWrap: true,
                            children: userRows,
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

  void getEvent() {
    Map<String, Event> events =
        GroupsManager.getGroupEvents(Globals.currentGroup);
    event = events[widget.eventId];

    userRows.clear();
    for (String username in event.optedIn.keys) {
      userRows.add(UserRowEvents(
          event.optedIn[username][UsersManager.DISPLAY_NAME],
          username,
          event.optedIn[username][UsersManager.ICON]));
    }
  }

  Future<Null> refreshList() async {
    List<String> groupId = new List<String>();
    groupId.add(widget.groupId);
    ResultStatus<List<Group>> result =
        await GroupsManager.getGroups(groupIds: groupId);
    if (result.success) {
      Globals.currentGroup = result.data.first;
      getEvent();
    } else {
      showErrorMessage("Error", result.errorMessage, context);
    }
    setState(() {});
  }
}
