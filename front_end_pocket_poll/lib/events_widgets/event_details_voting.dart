import 'dart:collection';

import 'package:add_2_calendar/add_2_calendar.dart' as calendar;
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/events_widgets/event_proposed_choice.dart';
import 'package:front_end_pocket_poll/imports/events_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/event.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'package:smooth_page_indicator/smooth_page_indicator.dart';

import 'event_user_row.dart';

class EventDetailsVoting extends StatefulWidget {
  final String groupId;
  final String eventId;
  final int mode;

  EventDetailsVoting({Key key, this.groupId, this.eventId, this.mode})
      : super(key: key);

  @override
  _EventDetailsVotingState createState() => new _EventDetailsVotingState();
}

class _EventDetailsVotingState extends State<EventDetailsVoting> {
  final PageController pageController = new PageController();
  String eventCreator;
  Map<String, EventUserRow> userRows; // username -> widget
  Map<String, String> choices; // choice id -> name
  Event event;

  @override
  void initState() {
    this.eventCreator = "";
    this.userRows = new Map<String, EventUserRow>();
    this.choices = new Map<String, String>();
    if (Globals.currentGroupResponse.eventsUnseen.containsKey(widget.eventId)) {
      UsersManager.markEventAsSeen(widget.groupId, widget.eventId);
      Globals.currentGroupResponse.eventsUnseen.remove(widget.eventId);
      Globals.currentGroupResponse.group.newEvents.remove(widget.eventId);
      Globals.user.groups[widget.groupId].eventsUnseen--;
    }

    buildUserRows(Globals.currentGroupResponse.group.votingEvents[widget.eventId]);
    for (String username in this.event.eventCreator.keys) {
      this.eventCreator =
          "${this.event.eventCreator[username].displayName} (@$username)";
    }
    for (String choiceId in this.event.tentativeAlgorithmChoices.keys) {
      this.choices.putIfAbsent(
          choiceId, () => this.event.tentativeAlgorithmChoices[choiceId]);
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
                      "Event Starts",
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
                    child: AutoSizeText("Voting Ends",
                        minFontSize: 20,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 40,
                        )),
                  ),
                  AutoSizeText(this.event.pollEndFormatted,
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
                  Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .001),
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
                  Container(
                    height: MediaQuery.of(context).size.height * .22,
                    width: MediaQuery.of(context).size.width * .95,
                    child: PageView.builder(
                      controller: this.pageController,
                      itemCount: this.choices.length,
                      itemBuilder: (context, index) {
                        String key = this.choices.keys.elementAt(index);
                        return EventProposedChoice(
                          groupId: widget.groupId,
                          eventId: widget.eventId,
                          event: this.event,
                          choiceName: this.choices[key],
                          choiceId: key,
                        );
                      },
                      scrollDirection: Axis.horizontal,
                    ),
                  ),
                  SmoothPageIndicator(
                    controller: this.pageController,
                    count: this.choices.length,
                    effect: SlideEffect(
                        dotColor: Colors.grey, activeDotColor: Colors.blueAccent),
                  )
                ],
              ),
            ],
          ),
        ),
      ),
    );
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
    // sorting alphabetical by displayname for now
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
