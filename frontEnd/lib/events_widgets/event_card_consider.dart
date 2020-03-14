import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_details_consider.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/utilities/utilities.dart';

class EventCardConsider extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;

  EventCardConsider(this.groupId, this.event, this.eventId);

  @override
  _EventCardConsiderState createState() => new _EventCardConsiderState();
}

class _EventCardConsiderState extends State<EventCardConsider> {
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
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
            AutoSizeText(
              "Proposed Date: ${widget.event.eventStartDateTimeFormatted}",
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            AutoSizeText(
              "Consider By: ${widget.event.pollBeginFormatted}",
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            AutoSizeText(
              "Members considered: ${widget.event.optedIn.length}",
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            RaisedButton(
              child: Text("Consider"),
              color: Colors.green,
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => EventDetailsConsider(
                          groupId: widget.groupId,
                          eventId: widget.eventId,
                          mode: EventsManager.considerMode)),
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
