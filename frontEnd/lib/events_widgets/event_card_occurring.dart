import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_details_occurring.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/utilities/utilities.dart';

class EventCardOccurring extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;

  EventCardOccurring(this.groupId, this.event, this.eventId);

  @override
  _EventCardOccurringState createState() => new _EventCardOccurringState();
}

class _EventCardOccurringState extends State<EventCardOccurring> {
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
              (DateTime.now().isBefore(widget.event.eventStartDateTime))
                  ? "Event starts: ${widget.event.eventStartDateTimeFormatted}"
                  : "Started at: ${widget.event.eventStartDateTimeFormatted}",
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            AutoSizeText(
              "Selected Choice: ${widget.event.selectedChoice}",
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            AutoSizeText(
              "Total considered: ${widget.event.optedIn.length}",
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              maxLines: 1,
            ),
            RaisedButton(
              child: Text("View Results"),
              color: Colors.green,
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => EventDetailsOccurring(
                          groupId: widget.groupId,
                          eventId: widget.eventId,
                          mode: EventsManager.occurringMode)),
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
