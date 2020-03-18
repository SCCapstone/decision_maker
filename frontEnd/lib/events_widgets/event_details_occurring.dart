import 'package:auto_size_text/auto_size_text.dart';
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

class EventDetailsOccurring extends StatefulWidget {
  final String groupId;
  final String eventId;
  final String mode;

  EventDetailsOccurring({Key key, this.groupId, this.eventId, this.mode})
      : super(key: key);

  @override
  _EventDetailsOccurringState createState() =>
      new _EventDetailsOccurringState();
}

class _EventDetailsOccurringState extends State<EventDetailsOccurring> {
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
            Column(
              children: <Widget>[
                Column(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: <Widget>[
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                      child: AutoSizeText(
                        (DateTime.now().isBefore(this.event.eventStartDateTime))
                            ? "Event Starts"
                            : "Event Started",
                        minFontSize: 20,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                            fontWeight: FontWeight.bold, fontSize: 40),
                      ),
                    ),
                    AutoSizeText(
                      this.event.eventStartDateTimeFormatted,
                      minFontSize: 15,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(fontSize: 32),
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                      child: AutoSizeText(
                        "Category",
                        minFontSize: 20,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                            fontWeight: FontWeight.bold, fontSize: 40),
                      ),
                    ),
                    AutoSizeText(
                      this.event.categoryName,
                      minFontSize: 12,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(fontSize: 32),
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                      child: AutoSizeText("Selected Choice",
                          minFontSize: 20,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 40,
                          )),
                    ),
                    AutoSizeText(this.event.selectedChoice,
                        minFontSize: 15,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(fontSize: 32)),
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
                      title: Text("Considered (${this.event.optedIn.length})"),
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
          ],
        ),
      ),
    );
  }

  void getEvent() {
    this.event = Globals.currentGroup.events[widget.eventId];


    this.userRows.clear();
    for (String username in event.optedIn.keys) {
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
      if (EventsManager.getEventMode(this.event) != widget.mode) {
        // if while the user was here and the mode changed, take them back to the group page
        Navigator.of(context).pop();
      }
    } else {
      showErrorMessage("Error", resultStatus.errorMessage, context);
    }
    setState(() {});
  }
}
