import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/utilities/utilities.dart';

class EventProposedChoice extends StatefulWidget {
  final String groupId;
  final Event event;
  final String eventId;
  final String choiceName;
  final String choiceId;

  EventProposedChoice(
      {Key key,
      this.groupId,
      this.event,
      this.eventId,
      this.choiceName,
      this.choiceId})
      : super(key: key);

  @override
  _EventProposedChoiceState createState() => new _EventProposedChoiceState();
}

class _EventProposedChoiceState extends State<EventProposedChoice> {
  int currentVote;
  Map<String, dynamic> voteMap = new Map<String, dynamic>();
  static final int voteYes = 1;
  static final int voteNo = 0;
  static final int voteEmpty = -1;

  @override
  void initState() {
    currentVote = voteEmpty;
    voteMap = widget.event.votingNumbers[widget.choiceId];
    if (voteMap.containsKey(Globals.username)) {
      currentVote = int.parse(voteMap[Globals.username]);
    }
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
                decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: (currentVote == voteNo)
                        ? Colors.orangeAccent
                        : Theme.of(context).scaffoldBackgroundColor),
                child: IconButton(
                  icon: Icon(Icons.thumb_down),
                  color: Colors.red,
                  onPressed: () {
                    if (currentVote != voteNo) {
                      // prevents wasteful API calls
                      tryVote(voteNo);
                    }
                  },
                ),
              ),
              Container(
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: (currentVote == voteYes)
                      ? Colors.greenAccent
                      : Theme.of(context).scaffoldBackgroundColor,
                ),
                child: IconButton(
                  icon: Icon(Icons.thumb_up),
                  color: Colors.green,
                  onPressed: () {
                    if (currentVote != voteYes) {
                      // prevents wasteful API calls
                      tryVote(voteYes);
                    }
                  },
                ),
              )
            ],
          )
        ],
      ),
    );
  }

  void tryVote(int voteVal) async {
    int previousVote = currentVote;
    // update changes locally so user doesn't have to fetch from DB to see new vote reflected
    Event event = Event.fromJson(Globals.currentGroup.events[widget.eventId]);
    event.votingNumbers[widget.choiceId].update(
        Globals.username, (existing) => voteVal.toString(),
        ifAbsent: () => voteVal.toString());
    widget.event.votingNumbers[widget.choiceId].update(
        Globals.username, (existing) => voteVal.toString(),
        ifAbsent: () => voteVal.toString());
    setState(() {
      currentVote = voteVal;
    });

    ResultStatus resultStatus = await GroupsManager.voteForChoice(
        widget.groupId, widget.eventId, widget.choiceId, voteVal);

    if (!resultStatus.success) {
      showErrorMessage("Error", resultStatus.errorMessage, context);
      setState(() {
        // if error, put vote back to what it was
        currentVote = previousVote;
        // update changes locally so user doesn't have to fetch from DB to see new vote
        Event event =
            Event.fromJson(Globals.currentGroup.events[widget.eventId]);
        event.votingNumbers[widget.choiceId].update(
            Globals.username, (existing) => currentVote.toString(),
            ifAbsent: () => currentVote.toString());
        widget.event.votingNumbers[widget.choiceId].update(
            Globals.username, (existing) => currentVote.toString(),
            ifAbsent: () => currentVote.toString());
      });
    }
  }
}
