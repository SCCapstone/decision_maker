import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_details_closed.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/utilities/utilities.dart';

class EventCardClosed extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;
  final Function refreshNotifications;
  final Function refreshPage;

  EventCardClosed(this.groupId, this.event, this.eventId,
      this.refreshNotifications, this.refreshPage);

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
              "Occurred\n${widget.event.eventStartDateTimeFormatted}",
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              textAlign: TextAlign.center,
              overflow: TextOverflow.ellipsis,
            ),
            AutoSizeText(
              widget.event.selectedChoice,
              style: TextStyle(fontSize: 20),
              minFontSize: 12,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
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
                ).then((_) {
                  widget.refreshPage();
                });
              },
            )
          ],
        ),
      ),
      decoration: BoxDecoration(
          border: Border(bottom: BorderSide(color: getBorderColor()))),
    );
  }

  void markEventRead() {
    if (Globals.user.groups[widget.groupId].eventsUnseen[widget.eventId] ==
        true) {
      UsersManager.markEventAsSeen(widget.groupId, widget.eventId);
      Globals.user.groups[widget.groupId].eventsUnseen.remove(widget.eventId);
      setState(() {
        widget.refreshNotifications();
      });
    }
  }
}
