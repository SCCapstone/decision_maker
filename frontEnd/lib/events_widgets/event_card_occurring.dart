import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_details_occurring.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/utilities/utilities.dart';

class EventCardOccurring extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;
  final Function refreshEventsUnseen;
  final Function refreshPage;

  EventCardOccurring(this.groupId, this.event, this.eventId,
      this.refreshEventsUnseen, this.refreshPage);

  @override
  _EventCardOccurringState createState() => new _EventCardOccurringState();
}

class _EventCardOccurringState extends State<EventCardOccurring> {
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
                        Globals.user.groups[widget.groupId].eventsUnseen
                            .containsKey(widget.eventId)),
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
              (DateTime.now().isBefore(widget.event.eventStartDateTime))
                  ? "Event Starts\n${widget.event.eventStartDateTimeFormatted}"
                  : "Started At\n${widget.event.eventStartDateTimeFormatted}",
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              textAlign: TextAlign.center,
              overflow: TextOverflow.ellipsis,
            ),
            Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .006),
            ),
            AutoSizeText(
              widget.event.selectedChoice,
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              maxLines: 1,
              textAlign: TextAlign.center,
              overflow: TextOverflow.ellipsis,
            ),
            Padding(
              padding:
              EdgeInsets.all(MediaQuery.of(context).size.height * .006),
            ),
            Center(
              child: RaisedButton(
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
                  ).then((_) {
                    widget.refreshPage();
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
    if (Globals.user.groups[widget.groupId].eventsUnseen[widget.eventId] ==
        true) {
      UsersManager.markEventAsSeen(widget.groupId, widget.eventId);
      Globals.user.groups[widget.groupId].eventsUnseen.remove(widget.eventId);
      setState(() {
        widget.refreshEventsUnseen();
      });
    }
  }
}
