import 'package:auto_size_text/auto_size_text.dart';
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
    for (String username in this.event.eventCreator.keys) {
      this.eventCreator =
          "${this.event.eventCreator[username][UsersManager.DISPLAY_NAME]} (@$username)";
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    // check what stage the event is in to display appropriate widgets
    return Scaffold(
      appBar: AppBar(
        centerTitle: true,
        title: AutoSizeText(
          this.event.eventName,
          maxLines: 1,
          minFontSize: 12,
          overflow: TextOverflow.ellipsis,
          style: TextStyle(fontSize: 36),
        ),
      ),
      body: RefreshIndicator(
        onRefresh: refreshList,
        child: ListView(
          shrinkWrap: true,
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: Column(
                children: <Widget>[
                  Column(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: <Widget>[
                      Padding(
                        padding: EdgeInsets.all(
                            MediaQuery.of(context).size.height * .01),
                        child: AutoSizeText(
                          "Occurred",
                          style: TextStyle(
                              fontWeight: FontWeight.bold, fontSize: 40),
                          minFontSize: 20,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      AutoSizeText(
                        this.event.eventStartDateTimeFormatted,
                        style: TextStyle(fontSize: 32),
                        minFontSize: 15,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      Padding(
                        padding: EdgeInsets.all(
                            MediaQuery.of(context).size.height * .01),
                        child: AutoSizeText(
                          "Category",
                          style: TextStyle(
                              fontWeight: FontWeight.bold, fontSize: 40),
                          minFontSize: 20,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      AutoSizeText(
                        this.event.categoryName,
                        style: TextStyle(fontSize: 32),
                        minFontSize: 15,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      Padding(
                        padding: EdgeInsets.all(
                            MediaQuery.of(context).size.height * .01),
                        child: AutoSizeText("Selected Choice",
                            maxLines: 1,
                            minFontSize: 20,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 40,
                            )),
                      ),
                      AutoSizeText(
                        this.event.selectedChoice,
                        style: TextStyle(fontSize: 32),
                        maxLines: 1,
                        minFontSize: 15,
                      ),
                      Padding(
                        padding: EdgeInsets.all(
                            MediaQuery.of(context).size.height * .01),
                      ),
                      AutoSizeText("Event created by: ${this.eventCreator}",
                          minFontSize: 12,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(fontSize: 16)),
                      ExpansionTile(
                        title:
                            Text("Considered (${this.event.optedIn.length})"),
                        children: <Widget>[
                          SizedBox(
                            height: MediaQuery.of(context).size.height * .2,
                            child: ListView(
                              shrinkWrap: true,
                              children: this.userRows,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  void getEvent() {
    Map<String, Event> events =
        GroupsManager.getGroupEvents(Globals.currentGroup);
    this.event = events[widget.eventId];

    this.userRows.clear();
    for (String username in this.event.optedIn.keys) {
      this.userRows.add(UserRowEvents(
          this.event.optedIn[username][UsersManager.DISPLAY_NAME],
          username,
          this.event.optedIn[username][UsersManager.ICON]));
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
    } else {
      showErrorMessage("Error", resultStatus.errorMessage, context);
    }
    setState(() {});
  }
}
