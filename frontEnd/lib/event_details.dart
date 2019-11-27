import 'package:flutter/material.dart';
import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/event.dart';

class EventDetails extends StatefulWidget {
  final Event event;
  final String mode;

  EventDetails({Key key, this.event, this.mode}) : super(key: key);

  @override
  _EventDetailsState createState() => new _EventDetailsState();
}

class _EventDetailsState extends State<EventDetails> {
  @override
  void initState() {
    EventsManager.updateEventMode(widget
        .event); // make this call in case now the event is in a different stage
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return new Scaffold(
      appBar: new AppBar(
        centerTitle: true,
        title: Text(
          widget.event.eventName,
          style: TextStyle(
              fontSize: DefaultTextStyle.of(context).style.fontSize * 0.8),
        ),
        leading: BackButton(),
      ),
      body: Center(
        child: Column(
          children: <Widget>[
            Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .015),
            ),
            Container(
              height: MediaQuery.of(context).size.height * .70,
              child: Text(
                  Globals.formatter.format(widget.event.eventStartDateTime)),
            ),
          ],
        ),
      ),
    );
  }
}
