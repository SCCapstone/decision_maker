import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/imports/users_manager.dart';
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
      : super(key: key) {
    if (Globals.user.groups[this.groupId].eventsUnseen[this.eventId] == true) {
      UsersManager.markEventAsSeen(this.groupId, this.eventId);
    }
  }

  @override
  _EventProposedChoiceState createState() => new _EventProposedChoiceState();
}

class _EventProposedChoiceState extends State<EventProposedChoice> {
  int currentVote;
  Map<String, int> voteMap;
  static final int voteYes = 1;
  static final int voteNo = 0;
  static final int voteEmpty = -1;

  @override
  void initState() {
    this.currentVote = voteEmpty;
    this.voteMap = widget.event.votingNumbers[widget.choiceId];
    if (this.voteMap != null && this.voteMap.containsKey(Globals.username)) {
      this.currentVote = this.voteMap[Globals.username];
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
          AutoSizeText(widget.choiceName,
              minFontSize: 15,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontSize: 32)),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: <Widget>[
              Container(
                decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: (this.currentVote == voteNo)
                        ? Colors.orangeAccent
                        : Theme.of(context).scaffoldBackgroundColor),
                child: IconButton(
                  icon: Icon(Icons.thumb_down),
                  color: Colors.red,
                  onPressed: () {
                    if (this.currentVote != voteNo) {
                      // prevents wasteful API calls
                      tryVote(voteNo);
                    }
                  },
                ),
              ),
              Container(
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: (this.currentVote == voteYes)
                      ? Colors.greenAccent
                      : Theme.of(context).scaffoldBackgroundColor,
                ),
                child: IconButton(
                  icon: Icon(Icons.thumb_up),
                  color: Colors.green,
                  onPressed: () {
                    if (this.currentVote != voteYes) {
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
    int previousVote = this.currentVote;
    // update changes locally so user doesn't have to fetch from DB to see new vote reflected
    if (widget.event.votingNumbers[widget.choiceId] == null) {
      widget.event.votingNumbers
          .putIfAbsent(widget.choiceId, () => new Map<String, int>());
    }

    widget.event.votingNumbers[widget.choiceId].update(
        Globals.username, (existing) => voteVal,
        ifAbsent: () => voteVal);

    setState(() {
      this.currentVote = voteVal;
    });

    ResultStatus resultStatus = await GroupsManager.voteForChoice(
        widget.groupId, widget.eventId, widget.choiceId, voteVal);

    if (!resultStatus.success) {
      showErrorMessage("Error", resultStatus.errorMessage, context);
      setState(() {
        // if error, put vote back to what it was
        this.currentVote = previousVote;
        // update changes locally so user doesn't have to fetch from DB to see new vote
        widget.event.votingNumbers[widget.choiceId].update(
            Globals.username, (existing) => this.currentVote,
            ifAbsent: () => this.currentVote);
      });
    }
  }
}
