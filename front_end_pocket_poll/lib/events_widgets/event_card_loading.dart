import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/events_widgets/events_list.dart';
import 'package:front_end_pocket_poll/imports/events_manager.dart';
import 'package:front_end_pocket_poll/models/event.dart';
import 'package:front_end_pocket_poll/models/event_card_interface.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';

class EventCardLoading extends StatefulWidget implements EventCardInterface {
  final String eventId;
  final int eventListType;

  EventCardLoading(this.eventId, this.eventListType);

  @override
  _EventCardLoadingState createState() => new _EventCardLoadingState();

  @override
  int getEventMode() {
    return EventsManager.loadingMode;
  }

  @override
  Event getEvent() {
    return null;
  }

  @override
  String getEventId() {
    return this.eventId;
  }
}

class _EventCardLoadingState extends State<EventCardLoading> {
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
                      "Loading...",
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
                ],
              ),
            ),
            Visibility(
                visible: (widget.eventListType == EventsList.eventsTypeVoting || widget.eventListType == EventsList.eventsTypeClosed),
                child: AutoSizeText(
                  "Loading\nEvent",
                  style: TextStyle(fontSize: 20),
                  minFontSize: 12,
                  textAlign: TextAlign.center,
                  overflow: TextOverflow.ellipsis,
                )),
            Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .006),
            ),
            Visibility(
                visible: (widget.eventListType == EventsList.eventsTypeClosed),
                child: AutoSizeText(
                  "...",
                  style: TextStyle(fontSize: 20),
                  minFontSize: 12,
                  maxLines: 1,
                  textAlign: TextAlign.center,
                  overflow: TextOverflow.ellipsis,
                )),
            Visibility(
                visible: (widget.eventListType == EventsList.eventsTypeClosed),
                child: Padding(
                  padding:
                      EdgeInsets.all(MediaQuery.of(context).size.height * .006),
                )),
            Center(
              child: RaisedButton(
                child: Text("Loading..."),
                color: Colors.green,
                onPressed: () {
                  //widget.refreshPagePage();
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
}
