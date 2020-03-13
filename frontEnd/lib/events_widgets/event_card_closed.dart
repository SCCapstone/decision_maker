import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_details_closed.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/utilities/utilities.dart';

class EventCardClosed extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;

  EventCardClosed(this.groupId, this.event, this.eventId);

  @override
  _EventCardClosedState createState() => new _EventCardClosedState();
}

class _EventCardClosedState extends State<EventCardClosed> {
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
              "Occurred: ${widget.event.eventStartDateTimeFormatted}",
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              maxLines: 1,
            ),
            AutoSizeText(
              "Selected Choice: ${widget.event.selectedChoice}",
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
              child: Text("View Results"),
              color: Colors.grey,
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => EventDetailsClosed(
                          groupId: widget.groupId, eventId: widget.eventId)),
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
