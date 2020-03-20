import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_details_voting.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/utilities/utilities.dart';

class EventCardVoting extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;
  final Function refreshPage;

  EventCardVoting(this.groupId, this.event, this.eventId, this.refreshPage);

  @override
  _EventCardVotingState createState() => new _EventCardVotingState();
}

class _EventCardVotingState extends State<EventCardVoting> {
  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * .27,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(8.0, 0, 8.0, 0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: <Widget>[
            Container(
              // height has to be here otherwise it shits the bed
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
                    visible: Globals.user.groups[widget.groupId].eventsUnseen
                        .containsKey(widget.eventId),
                    child: Align(
                      alignment: Alignment.centerRight,
                      child: Container(
                        child: IconButton(
                          icon: Icon(Icons.notification_important),
                          color: Color(0xff5ce080),
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
              "Event Starts: ${widget.event.eventStartDateTimeFormatted}",
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            AutoSizeText(
              "Voting Ends: ${widget.event.pollEndFormatted}",
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            RaisedButton(
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
                );
              },
            )
          ],
        ),
      ),
      decoration: BoxDecoration(
          border: Globals.user.groups[widget.groupId].eventsUnseen
                  .containsKey(widget.eventId)
              ? Border.all(width: 3.0, color: const Color(0xff5ce080))
              : Border(bottom: BorderSide(color: getBorderColor()))),
    );
  }

  void markEventRead() {
    if (Globals.user.groups[widget.groupId].eventsUnseen[widget.eventId] ==
        true) {
      UsersManager.markEventAsSeen(widget.groupId, widget.eventId);
      Globals.user.groups[widget.groupId].eventsUnseen.remove(widget.eventId);
      setState(() {
        widget.refreshPage();
      });
    }
  }
}
