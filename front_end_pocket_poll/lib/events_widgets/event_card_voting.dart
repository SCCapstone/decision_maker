import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/events_widgets/event_details_voting.dart';
import 'package:front_end_pocket_poll/imports/events_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/event.dart';
import 'package:front_end_pocket_poll/models/event_card_interface.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';

class EventCardVoting extends StatefulWidget implements EventCardInterface {
  final String groupId;
  final Event event;
  final String eventId;
  final Function refreshEventsUnseen;
  final Function refreshPagePage;

  EventCardVoting(this.groupId, this.event, this.eventId,
      this.refreshEventsUnseen, this.refreshPagePage);

  @override
  _EventCardVotingState createState() => new _EventCardVotingState();

  @override
  int getEventMode() {
    return EventsManager.votingMode;
  }

  @override
  Event getEvent() {
    return this.event;
  }
}

class _EventCardVotingState extends State<EventCardVoting> {
  @override
  Widget build(BuildContext context) {
    return ConstrainedBox(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * .6,
      ),
      child: Container(
        child: ListView(
          physics: ClampingScrollPhysics(),
          shrinkWrap: true,
          children: <Widget>[
            Container(
              // height has to be here otherwise it overflows
              height: 45,
              child: Stack(
                children: <Widget>[
                  Align(
                    alignment: Alignment.center,
                    child: AutoSizeText(
                      widget.event.eventName,
                      minFontSize: 12,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  Visibility(
                    visible: (Globals.user.groups[widget.groupId] != null &&
                        Globals.currentGroupResponse.eventsUnseen.containsKey(widget.eventId)),
                    child: Align(
                      alignment: Alignment.centerRight,
                      child: Container(
                        child: IconButton(
                          icon: Icon(Icons.notification_important),
                          color: Globals.pocketPollGreen,
                          tooltip: "Mark seen",
                          onPressed: () {
                            markEventRead();
                          },
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            AutoSizeText(
              "Voting Ends\n${widget.event.pollEndFormatted}",
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              overflow: TextOverflow.ellipsis,
              textAlign: TextAlign.center,
            ),
            Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .006),
            ),
            Center(
              child: RaisedButton(
                child: Text("Vote"),
                color: Colors.green,
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (context) => EventDetailsVoting(
                              groupId: widget.groupId,
                              eventId: widget.eventId,
                              mode: EventsManager.votingMode,
                            )),
                  ).then((_) {
                    widget.refreshPagePage();
                  });
                },
              ),
            ),
            Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .006),
            ),
          ],
        ),
        decoration: BoxDecoration(
            border: Border(bottom: BorderSide(color: getBorderColor()))),
      ),
    );
  }

  void markEventRead() {
    if (Globals.currentGroupResponse.eventsUnseen.containsKey(widget.eventId)) {
      // blind send, not critical to catch errors
      UsersManager.markEventAsSeen(widget.groupId, widget.eventId);
      Globals.currentGroupResponse.eventsUnseen.remove(widget.eventId);
      Globals.user.groups[widget.groupId].eventsUnseen--;
      setState(() {
        widget.refreshEventsUnseen();
      });
    }
  }
}
