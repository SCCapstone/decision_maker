import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_details_voting.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/utilities/utilities.dart';

class EventCardVoting extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;

  EventCardVoting(this.groupId, this.event, this.eventId);

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
            AutoSizeText(
              widget.event.eventName,
              minFontSize: 12,
              maxLines: 1,
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
            AutoSizeText(
              "Event Starts: ${widget.event.eventStartDateTimeFormatted}",
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              maxLines: 1,
            ),
            AutoSizeText(
              "Voting Ends: ${widget.event.pollEndFormatted}",
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              maxLines: 1,
            ),
            AutoSizeText(
              "Total considered: ${widget.event.optedIn.length}",
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              maxLines: 1,
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
          border: Border(bottom: BorderSide(color: getBorderColor()))),
    );
  }
}
