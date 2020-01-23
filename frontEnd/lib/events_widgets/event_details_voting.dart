import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_proposed_choice.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/widgets/user_row_events.dart';
import 'package:smooth_page_indicator/smooth_page_indicator.dart';

class EventDetailsVoting extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;
  final List<Widget> userRows = new List<Widget>();

  EventDetailsVoting({Key key, this.groupId, this.event, this.eventId})
      : super(key: key);

  @override
  _EventDetailsVotingState createState() => new _EventDetailsVotingState();
}

class _EventDetailsVotingState extends State<EventDetailsVoting> {
  final PageController controller = new PageController();
  DateTime createTime;
  DateTime pollFinished;
  DateTime proposedTime;
  String pollFinishedFormatted;
  String eventStartFormatted;
  String eventCreator = "";

  @override
  void initState() {
    createTime = widget.event.createdDateTime;
    proposedTime = widget.event.eventStartDateTime;
    for (String username in widget.event.optedIn.keys) {
      widget.userRows.add(UserRow(widget.event.optedIn[username]));
    }
    for (String username in widget.event.eventCreator.keys) {
      eventCreator = widget.event.eventCreator[username];
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    List<String> exampleChoices = new List();
    for (int i = 0; i < 5; i++) {
      exampleChoices.add("Choice Number $i");
    }
    getFormattedTimes();
    return Scaffold(
      appBar: new AppBar(
        centerTitle: true,
        title: Text(
          widget.event.eventName,
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
                      eventStartFormatted,
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
                    Text(pollFinishedFormatted,
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
                      widget.event.categoryName,
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
                      title: Text("Attendees (${widget.event.optedIn.length})"),
                      children: <Widget>[
                        SizedBox(
                          height: MediaQuery.of(context).size.height * .2,
                          child: ListView(
                            shrinkWrap: true,
                            children: widget.userRows,
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
                        itemCount: exampleChoices.length,
                        itemBuilder: (context, index) {
                          return EventProposedChoice(
                            groupId: widget.groupId,
                            eventId: widget.eventId,
                            event: widget.event,
                            choiceName: exampleChoices[index],
                          );
                        },
                        scrollDirection: Axis.horizontal,
                      ),
                    ),
                    SmoothPageIndicator(
                      controller: controller,
                      count: exampleChoices.length,
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

  Future<Null> refreshList() async {
    await Future.delayed(
        // required to remove the loading animation
        Duration(milliseconds: 70));
    setState(() {});
  }

  void getFormattedTimes() {
    pollFinished =
        createTime.add(new Duration(minutes: (widget.event.pollDuration) * 2));
    pollFinishedFormatted = Globals.formatter.format(pollFinished);
    eventStartFormatted =
        Globals.formatter.format(widget.event.eventStartDateTime);
  }
}
