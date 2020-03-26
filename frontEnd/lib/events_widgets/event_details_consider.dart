import 'dart:collection';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/models/member.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/widgets/user_row_events.dart';

class EventDetailsConsider extends StatefulWidget {
  final String groupId;
  final String eventId;
  final String mode;

  EventDetailsConsider({Key key, this.groupId, this.eventId, this.mode})
      : super(key: key);

  @override
  _EventDetailsConsiderState createState() => new _EventDetailsConsiderState();
}

class _EventDetailsConsiderState extends State<EventDetailsConsider> {
  Map<String, UserRowEvents> userRows = new Map<String, UserRowEvents>();
  String eventCreator = "";
  Event event;

  @override
  void initState() {
    if (Globals.user.groups[widget.groupId].eventsUnseen[widget.eventId] ==
        true) {
      UsersManager.markEventAsSeen(widget.groupId, widget.eventId);
      Globals.user.groups[widget.groupId].eventsUnseen.remove(widget.eventId);
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
                      child: AutoSizeText(
                        "Proposed Time",
                        minFontSize: 20,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                            fontWeight: FontWeight.bold, fontSize: 40),
                      ),
                    ),
                    AutoSizeText(
                      Globals.formatter.format(this.event.eventStartDateTime),
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
                        style: TextStyle(
                            fontWeight: FontWeight.bold, fontSize: 40),
                      ),
                    ),
                    AutoSizeText(
                      this.event.categoryName,
                      minFontSize: 15,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(fontSize: 32),
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
                            child: ListView(
                              shrinkWrap: true,
                              children: this.userRows.values.toList(),
                            ),
                          ),
                        ],
                      ),
                    ),
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
                        RaisedButton(
                          child: Text("No"),
                          color: Colors.red,
                          onPressed: () {
                            tryConsider(false);
                          },
                        ),
                        RaisedButton(
                          child: Text("Yes"),
                          color: Colors.green,
                          onPressed: () {
                            tryConsider(true);
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

  void tryConsider(bool considerUser) async {
    bool oldConsiderUser = !considerUser;
    // update group in local memory to reflect the change in consider value
    if (considerUser) {
      Globals.currentGroup.events[widget.eventId].optedIn.putIfAbsent(
          Globals.username, () => new Member.fromUser(Globals.user));
      this.userRows.putIfAbsent(
          Globals.username,
          () => UserRowEvents(
              Globals.user.displayName, Globals.username, Globals.user.icon));
    } else {
      Globals.currentGroup.events[widget.eventId].optedIn
          .remove(Globals.username);
      this.userRows.remove(Globals.username);
    }
    setState(() {});

    ResultStatus resultStatus = await GroupsManager.optInOutOfEvent(
        widget.groupId, widget.eventId, considerUser);
    if (!resultStatus.success) {
      // revert consider back if it failed
      if (oldConsiderUser) {
        Globals.currentGroup.events[widget.eventId].optedIn.putIfAbsent(
            Globals.username, () => new Member.fromUser(Globals.user));
        this.userRows.putIfAbsent(
            Globals.username,
            () => UserRowEvents(
                Globals.user.displayName, Globals.username, Globals.user.icon));
      } else {
        Globals.currentGroup.events[widget.eventId].optedIn
            .remove(Globals.username);
        this.userRows.remove(Globals.username);
      }
      showErrorMessage("Error", resultStatus.errorMessage, context);
      setState(() {});
    }
  }

  void getEvent() {
    this.event = Globals.currentGroup.events[widget.eventId];

    this.userRows.clear();
    for (String username in this.event.optedIn.keys) {
      this.userRows.putIfAbsent(
          username,
          () => UserRowEvents(this.event.optedIn[username].displayName,
              username, this.event.optedIn[username].icon));
    }
    // sorting by alphabetical by displayname for now
    List<String> sortedKeys = this.userRows.keys.toList(growable: false)
      ..sort((k1, k2) =>
          this.userRows[k1].displayName.compareTo(userRows[k2].displayName));
    LinkedHashMap sortedMap = new LinkedHashMap.fromIterable(sortedKeys,
        key: (k) => k, value: (k) => this.userRows[k]);
    this.userRows = sortedMap.cast();
  }

  Future<Null> refreshList() async {
    ResultStatus<Group> resultStatus =
        await GroupsManager.getGroup(widget.groupId);
    if (resultStatus.success) {
      Globals.currentGroup = resultStatus.data;
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
