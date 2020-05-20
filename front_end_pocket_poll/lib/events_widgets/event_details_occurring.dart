import 'dart:collection';

import 'package:add_2_calendar/add_2_calendar.dart' as calendar;
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/events_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/event.dart';
import 'package:front_end_pocket_poll/models/get_group_response.dart';
import 'package:front_end_pocket_poll/models/group.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'event_user_row.dart';

class EventDetailsOccurring extends StatefulWidget {
  final String groupId;
  final String eventId;
  final int mode;

  EventDetailsOccurring({Key key, this.groupId, this.eventId, this.mode})
      : super(key: key);

  @override
  _EventDetailsOccurringState createState() =>
      new _EventDetailsOccurringState();
}

class _EventDetailsOccurringState extends State<EventDetailsOccurring> {
  String eventCreator;
  Map<String, EventUserRow> userRows; // username -> widget
  Event event;

  @override
  void initState() {
    this.eventCreator = "";
    this.userRows = new Map<String, EventUserRow>();
    // clicking on the details page marks the event unseen
    if (Globals.currentGroupResponse.eventsUnseen.containsKey(widget.eventId)) {
      UsersManager.markEventAsSeen(widget.groupId, widget.eventId);
      Globals.currentGroupResponse.eventsUnseen.remove(widget.eventId);
      Globals.user.groups[widget.groupId].eventsUnseen--;
    }

    getEvent();
    for (String username in this.event.eventCreator.keys) {
      this.eventCreator =
          "${this.event.eventCreator[username].displayName} (@$username)";
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
        actions: <Widget>[
          IconButton(
            icon: Icon(Icons.date_range),
            iconSize: 30,
            tooltip: "Add to My Calendar",
            onPressed: () {
              calendar.Event calendarEvent = calendar.Event(
                title: this.event.eventName,
                startDate: this.event.eventStartDateTime,
                endDate: this.event.eventStartDateTime.add(Duration(hours: 1)),
                allDay: false,
              );
              calendar.Add2Calendar.addEvent2Cal(calendarEvent);
            },
          )
        ],
      ),
      body: RefreshIndicator(
        onRefresh: refreshEvent,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(8.0, 0, 8.0, 0),
          child: ListView(
            shrinkWrap: true,
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
                      style:
                          TextStyle(fontWeight: FontWeight.bold, fontSize: 40),
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
                      style:
                          TextStyle(fontWeight: FontWeight.bold, fontSize: 40),
                    ),
                  ),
                  AutoSizeText(
                    this.event.categoryName,
                    minFontSize: 12,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontSize: 32),
                  ),
                  AutoSizeText(
                    "Version: ${this.event.categoryVersion.toString()}",
                    minFontSize: 12,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontSize: 16),
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
                  Visibility(
                    visible: this.event.optedIn.length > 0,
                    child: ExpansionTile(
                      title: Text("Considered (${this.event.optedIn.length})"),
                      children: <Widget>[
                        ConstrainedBox(
                          constraints: BoxConstraints(
                            maxHeight: MediaQuery.of(context).size.height * .2,
                          ),
                          child: Scrollbar(
                            child: ListView(
                              shrinkWrap: true,
                              children: this.userRows.values.toList(),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  Visibility(
                      visible: this.event.optedIn.length <= 0,
                      child: AutoSizeText(
                        "No members considered",
                        minFontSize: 12,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(fontSize: 16),
                      )),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  void getEvent() {
    this.event = Globals.currentGroupResponse.group.events[widget.eventId];

    this.userRows.clear();
    for (String username in this.event.optedIn.keys) {
      this.userRows.putIfAbsent(
          username,
          () => EventUserRow(this.event.optedIn[username].displayName, username,
              this.event.optedIn[username].icon));
    }
    // sorting by alphabetical by displayname for now
    List<String> sortedKeys = this.userRows.keys.toList(growable: false)
      ..sort((k1, k2) =>
          this.userRows[k1].displayName.compareTo(userRows[k2].displayName));
    LinkedHashMap sortedMap = new LinkedHashMap.fromIterable(sortedKeys,
        key: (k) => k, value: (k) => this.userRows[k]);
    this.userRows = sortedMap.cast();
  }

  Future<Null> refreshEvent() async {
    ResultStatus<GetGroupResponse> resultStatus = await GroupsManager.getGroup(
        widget.groupId,
        batchNumber: Globals.currentGroupResponse.group.currentBatchNum);
    if (resultStatus.success) {
      Globals.currentGroupResponse = resultStatus.data;
      getEvent();
      if (EventsManager.getEventMode(this.event) != widget.mode) {
        // if while the user was here and the mode changed, take them back to the group page
        Navigator.of(this.context).pop();
      }
    } else {
      showErrorMessage("Error", resultStatus.errorMessage, this.context);
    }
    setState(() {});
  }
}
