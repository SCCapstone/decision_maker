import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_details_voting.dart';
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
      child: Column(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: <Widget>[
          Text(
            widget.event.eventName,
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
          ),
          Text("Event Starts: ${widget.event.eventStartDateTimeFormatted}",
              style: TextStyle(fontSize: 20)),
          Text("Voting Ends: ${widget.event.pollEndFormatted}",
              style: TextStyle(fontSize: 20)),
          Text("Total attendees: ${widget.event.optedIn.length}",
              style: TextStyle(fontSize: 20)),
          RaisedButton(
            child: Text("Vote"),
            color: Colors.green,
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (context) => EventDetailsVoting(
                        groupId: widget.groupId, eventId: widget.eventId)),
              );
            },
          )
        ],
      ),
      decoration: new BoxDecoration(
          border: new Border(bottom: new BorderSide(color: getBorderColor()))),
    );
  }
}
