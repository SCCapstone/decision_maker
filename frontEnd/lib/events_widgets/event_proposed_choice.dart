import 'package:flutter/material.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/models/event.dart';

class EventProposedChoice extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;
  final String choiceName;

  EventProposedChoice(
      {Key key, this.groupId, this.event, this.eventId, this.choiceName})
      : super(key: key);

  @override
  _EventProposedChoiceState createState() => new _EventProposedChoiceState();
}

class _EventProposedChoiceState extends State<EventProposedChoice> {
  int vote;
  static final int voteYes = 1;
  static final int voteNo = 0;
  static final int voteEmpty = -1;

  @override
  void initState() {
    // find the vote from the DB, assuming true for now
    vote = voteEmpty;
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: MediaQuery.of(context).size.width * .90,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: <Widget>[
          Text(widget.choiceName,
              style: TextStyle(
                  fontSize: DefaultTextStyle.of(context).style.fontSize * 2.5)),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: <Widget>[
              Container(
                color: (vote == voteNo)
                    ? Colors.orangeAccent
                    : Theme.of(context).scaffoldBackgroundColor,
                child: IconButton(
                  icon: Icon(Icons.thumb_down),
                  color: Colors.red,
                  onPressed: () {
                    GroupsManager.optInOutOfEvent(
                        widget.groupId, widget.eventId, false, context);
                    setState(() {
                      vote = voteNo;
                    });
                  },
                ),
              ),
              Container(
                color: (vote == voteYes)
                    ? Colors.greenAccent
                    : Theme.of(context).scaffoldBackgroundColor,
                child: IconButton(
                  icon: Icon(Icons.thumb_up),
                  color: Colors.green,
                  onPressed: () {
                    GroupsManager.optInOutOfEvent(
                        widget.groupId, widget.eventId, true, context);
                    setState(() {
                      vote = voteYes;
                    });
                  },
                ),
              )
            ],
          )
        ],
      ),
    );
  }
}
