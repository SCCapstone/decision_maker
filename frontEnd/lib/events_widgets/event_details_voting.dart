import 'dart:collection';

import 'package:add_2_calendar/add_2_calendar.dart' as calendar;
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_proposed_choice.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/widgets/user_row_events.dart';
import 'package:smooth_page_indicator/smooth_page_indicator.dart';

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
  String eventCreator = "";
  Map<String, UserRowEvents> userRows = new Map<String, UserRowEvents>();
  Map<String, String> choices = new Map<String, String>();
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
                        "Event Starts",
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
                        child: AutoSizeText("No members considered",
                          minFontSize: 12,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(fontSize: 16),
                        )
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                    ),
                    Container(
                      height: MediaQuery.of(context).size.height * .27,
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
                          dotColor: Colors.grey, activeDotColor: Colors.green),
                    )
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
      bottomNavigationBar: BottomAppBar(
        color: Theme.of(context).scaffoldBackgroundColor,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            RaisedButton.icon(
              icon: Icon(Icons.date_range),
              label: Text("Add to My Calendar"),
              onPressed: () {
                calendar.Event calendarEvent = calendar.Event(
                  title: event.eventName,
                  startDate: event.eventStartDateTime,
                  endDate: event.eventStartDateTime.add(Duration(hours: 1)),
                  allDay: false,
                );
                calendar.Add2Calendar.addEvent2Cal(calendarEvent);
              },
            ),
          ],
        ),
      ),
    );
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
