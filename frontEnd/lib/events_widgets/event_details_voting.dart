import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_proposed_choice.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/widgets/user_row_events.dart';
import 'package:smooth_page_indicator/smooth_page_indicator.dart';

class EventDetailsVoting extends StatefulWidget {
  final String groupId;
  final String eventId;
  final String mode;

  EventDetailsVoting({Key key, this.groupId, this.eventId, this.mode}) : super(key: key);

  @override
  _EventDetailsVotingState createState() => new _EventDetailsVotingState();
}

class _EventDetailsVotingState extends State<EventDetailsVoting> {
  final PageController controller = new PageController();
  String eventCreator = "";
  List<Widget> userRows = new List<Widget>();
  Map<String, String> choices = new Map<String, String>();
  Event event;

  @override
  void initState() {
    getEvent();
    for (String username in event.eventCreator.keys) {
      eventCreator = event.eventCreator[username];
    }
    for (String choiceId in event.tentativeAlgorithmChoices.keys) {
      choices.putIfAbsent(
          choiceId, () => event.tentativeAlgorithmChoices[choiceId]);
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
                        "Event Starts",
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
                      child: Text("Voting Ends",
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize:
                                DefaultTextStyle.of(context).style.fontSize *
                                    0.8,
                          )),
                    ),
                    Text(event.pollEndFormatted,
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
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                    ),
                    Container(
                      height: MediaQuery.of(context).size.height * .27,
                      width: MediaQuery.of(context).size.width * .95,
                      child: PageView.builder(
                        controller: controller,
                        itemCount: choices.length,
                        itemBuilder: (context, index) {
                          String key = choices.keys.elementAt(index);
                          return EventProposedChoice(
                            groupId: widget.groupId,
                            eventId: widget.eventId,
                            event: event,
                            choiceName: choices[key],
                            choiceId: key,
                          );
                        },
                        scrollDirection: Axis.horizontal,
                      ),
                    ),
                    SmoothPageIndicator(
                      controller: controller,
                      count: choices.length,
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
    Globals.currentGroup =
        (await GroupsManager.getGroups(groupIds: groupId)).first;
    getEvent();
    if(EventsManager.getEventMode(event)!=widget.mode){
      // if while the user was here and the mode changed, take them back to the group page
      Navigator.of(context).pop();
    }
    setState(() {});
  }
}
