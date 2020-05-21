import 'dart:collection';

import 'package:add_2_calendar/add_2_calendar.dart' as calendar;
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/events_widgets/event_update_ratings.dart';
import 'package:front_end_pocket_poll/imports/events_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/event.dart';
import 'package:front_end_pocket_poll/models/member.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';

import 'event_user_row.dart';

class EventDetailsConsider extends StatefulWidget {
  final String groupId;
  final String eventId;
  final int mode;

  EventDetailsConsider({Key key, this.groupId, this.eventId, this.mode})
      : super(key: key);

  @override
  _EventDetailsConsiderState createState() => new _EventDetailsConsiderState();
}

class _EventDetailsConsiderState extends State<EventDetailsConsider> {
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

    buildUserRows(Globals.currentGroupResponse.group.events[widget.eventId]);
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
        leading: BackButton(),
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
                      "Proposed Time",
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
                    child: AutoSizeText("Consider Time Ends",
                        minFontSize: 20,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 40,
                        )),
                  ),
                  AutoSizeText(this.event.pollBeginFormatted,
                      minFontSize: 15,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(fontSize: 32)),
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
                    minFontSize: 15,
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
                  RaisedButton(
                    child: Text("Update Ratings"),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(18.0),
                    ),
                    onPressed: () {
                      Navigator.push(
                          context,
                          MaterialPageRoute(
                              builder: (context) => EventUpdateRatings(
                                  groupId: widget.groupId,
                                  eventId: widget.eventId))).then((_) {
                        refreshEvent();
                      });
                    },
                  ),
                  Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .008),
                  ),
                  AutoSizeText("Event created by: ${this.eventCreator}",
                      minFontSize: 12,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(fontSize: 16)),
                  Visibility(
                    visible: this.userRows.length > 0,
                    child: ExpansionTile(
                      title:
                          Text("Members considered (${this.userRows.length})"),
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
                        "No members currently being considered",
                        minFontSize: 12,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(fontSize: 16),
                      )),
                  Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .01),
                  ),
                  AutoSizeText("Consider Me?",
                      minFontSize: 12,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(fontSize: 20)),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: <Widget>[
                      Container(
                        child: RaisedButton(
                          child: Text("No"),
                          color: Colors.red,
                          onPressed: () {
                            tryConsider(false);
                          },
                        ),
                        decoration: BoxDecoration(
                          border: Border(
                              bottom: BorderSide(
                            width: 3,
                            color: (!this
                                    .event
                                    .optedIn
                                    .containsKey(Globals.username))
                                ? Colors.orangeAccent
                                : Theme.of(context).scaffoldBackgroundColor,
                          )),
                        ),
                      ),
                      Container(
                        child: RaisedButton(
                          child: Text("Yes"),
                          color: Colors.green,
                          onPressed: () {
                            tryConsider(true);
                          },
                        ),
                        decoration: BoxDecoration(
                          border: Border(
                              bottom: BorderSide(
                            width: 3,
                            color: (this
                                    .event
                                    .optedIn
                                    .containsKey(Globals.username))
                                ? Colors.greenAccent
                                : Theme.of(context).scaffoldBackgroundColor,
                          )),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  void tryConsider(bool considerUser) async {
    bool oldConsiderUser = !considerUser;
    // update group in local memory to reflect the change in consider value
    if (considerUser) {
      Globals.currentGroupResponse.group.events[widget.eventId].optedIn
          .putIfAbsent(
              Globals.username, () => new Member.fromUser(Globals.user));
      this.userRows.putIfAbsent(
          Globals.username,
          () => EventUserRow(
              Globals.user.displayName, Globals.username, Globals.user.icon));
    } else {
      Globals.currentGroupResponse.group.events[widget.eventId].optedIn
          .remove(Globals.username);
      this.userRows.remove(Globals.username);
    }
    setState(() {});

    ResultStatus resultStatus = await GroupsManager.optInOutOfEvent(
        widget.groupId, widget.eventId, considerUser);
    if (!resultStatus.success) {
      // revert consider back if it failed
      if (oldConsiderUser) {
        Globals.currentGroupResponse.group.events[widget.eventId].optedIn
            .putIfAbsent(
                Globals.username, () => new Member.fromUser(Globals.user));
        this.userRows.putIfAbsent(
            Globals.username,
            () => EventUserRow(
                Globals.user.displayName, Globals.username, Globals.user.icon));
      } else {
        Globals.currentGroupResponse.group.events[widget.eventId].optedIn
            .remove(Globals.username);
        this.userRows.remove(Globals.username);
      }
      showErrorMessage("Error", resultStatus.errorMessage, context);
      setState(() {});
    }
  }

  void buildUserRows(final Event event) {
    this.event = event;

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
    final ResultStatus<Event> resultStatus =
        await GroupsManager.getEvent(widget.groupId, widget.eventId);
    if (resultStatus.success) {
      this.buildUserRows(resultStatus.data);
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
