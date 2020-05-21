import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/events_widgets/event_details_consider.dart';
import 'package:front_end_pocket_poll/imports/events_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/event.dart';
import 'package:front_end_pocket_poll/models/event_card_interface.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';

class EventCardConsider extends StatefulWidget implements EventCardInterface {
  final String groupId;
  final Event event;
  final String eventId;
  final Function refreshEventsUnseen;
  final Function refreshGroupPage;

  EventCardConsider(this.groupId, this.event, this.eventId,
      this.refreshEventsUnseen, this.refreshGroupPage);

  @override
  _EventCardConsiderState createState() => new _EventCardConsiderState();

  @override
  int getEventMode() {
    return EventsManager.considerMode;
  }

  @override
  Event getEvent() {
    return this.event;
  }
}

class _EventCardConsiderState extends State<EventCardConsider> {
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
                        Globals.currentGroupResponse.eventsUnseen
                            .containsKey(widget.eventId)),
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
                key: Key("event_card_consider:${widget.event.eventName}"),
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
                    widget.refreshGroupPage();
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
