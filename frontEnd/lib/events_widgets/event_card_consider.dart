import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/events_widgets/event_details_consider.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/utilities/utilities.dart';

class EventCardConsider extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;
  final Function refreshEventsUnseen;
  final Function refreshPage;

  EventCardConsider(this.groupId, this.event, this.eventId,
      this.refreshEventsUnseen, this.refreshPage);

  @override
  _EventCardConsiderState createState() => new _EventCardConsiderState();
}

class _EventCardConsiderState extends State<EventCardConsider> {
  @override
  Widget build(BuildContext context) {
    return ConstrainedBox(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * .35,
      ),
      child: Container(
        child: ListView(
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
              "Consider By\n${widget.event.pollBeginFormatted}",
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
              "Current Considered: ${widget.event.optedIn.length}",
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
