import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/imports/globals.dart';

class Event {
  final String categoryId;
  final String categoryName;
  final String eventName;
  final DateTime createdDateTime;
  final DateTime eventStartDateTime;
  final DateTime pollBegin;
  final DateTime pollEnd;
  final int type;
  final int pollDuration;
  final int pollPassPercent;
  final Map<String, dynamic> optedIn;
  final Map<String, dynamic> tentativeAlgorithmChoices;
  final Map<String, dynamic> votingNumbers;
  final Map<String, dynamic> eventCreator;
  final String selectedChoice;
  final String eventStartDateTimeFormatted;
  final String pollBeginFormatted;
  final String pollEndFormatted;

  Event(
      {this.categoryId,
      this.categoryName,
      this.eventName,
      this.createdDateTime,
      this.eventStartDateTime,
      this.type,
      this.pollDuration,
      this.pollPassPercent,
      this.optedIn,
      this.tentativeAlgorithmChoices,
      this.votingNumbers,
      this.selectedChoice,
      this.eventCreator,
      this.eventStartDateTimeFormatted,
      this.pollBegin,
      this.pollEnd,
      this.pollBeginFormatted,
      this.pollEndFormatted});

  Event.debug(
      this.categoryId,
      this.categoryName,
      this.eventName,
      this.createdDateTime,
      this.eventStartDateTime,
      this.type,
      this.pollDuration,
      this.pollPassPercent,
      this.optedIn,
      this.tentativeAlgorithmChoices,
      this.votingNumbers,
      this.selectedChoice,
      this.eventCreator,
      this.eventStartDateTimeFormatted,
      this.pollBegin,
      this.pollEnd,
      this.pollBeginFormatted,
      this.pollEndFormatted);

  factory Event.fromJson(Map<String, dynamic> json) {
    DateTime pollBeginTemp =
        DateTime.parse(json[EventsManager.CREATED_DATE_TIME]).add(new Duration(
            minutes: (int.parse(json[EventsManager.POLL_DURATION]))));
    DateTime pollEndTemp = DateTime.parse(json[EventsManager.CREATED_DATE_TIME])
        .add(new Duration(
            minutes: (int.parse(json[EventsManager.POLL_DURATION])) * 2));

    return Event(
        categoryId: json[EventsManager.CATEGORY_ID],
        categoryName: json[EventsManager.CATEGORY_NAME],
        eventName: json[EventsManager.EVENT_NAME],
        createdDateTime: DateTime.parse(json[EventsManager.CREATED_DATE_TIME]),
        eventStartDateTime:
            DateTime.parse(json[EventsManager.EVENT_START_DATE_TIME]),
        pollEnd: pollEndTemp,
        pollBegin: pollBeginTemp,
        type: int.parse(json[EventsManager.TYPE]),
        pollDuration: int.parse(json[EventsManager.POLL_DURATION]),
        pollPassPercent: int.parse(json[EventsManager.POLL_PASS_PERCENT]),
        optedIn: json[EventsManager.OPTED_IN],
        tentativeAlgorithmChoices:
            json[EventsManager.TENTATIVE_ALGORITHM_CHOICES],
        selectedChoice: json[EventsManager.SELECTED_CHOICE],
        votingNumbers: json[EventsManager.VOTING_NUMBERS],
        eventCreator: json[EventsManager.EVENT_CREATOR],
        eventStartDateTimeFormatted: Globals.formatter
            .format(DateTime.parse(json[EventsManager.EVENT_START_DATE_TIME])),
        pollEndFormatted: Globals.formatter.format(pollEndTemp),
        pollBeginFormatted: Globals.formatter.format(pollBeginTemp));
  }

  Map asMap() {
    return {
      // Need the DateTime objects to be Strings when used in the API request
      // because json.encode can't encode DateTime objects.
      EventsManager.EVENT_NAME: this.eventName,
      EventsManager.CATEGORY_ID: this.categoryId,
      EventsManager.CATEGORY_NAME: this.categoryName,
      EventsManager.CREATED_DATE_TIME:
          this.createdDateTime.toString().substring(0, 19),
      EventsManager.EVENT_START_DATE_TIME:
          this.eventStartDateTime.toString().substring(0, 19),
      EventsManager.TYPE: this.type,
      EventsManager.POLL_DURATION: this.pollDuration,
      EventsManager.POLL_PASS_PERCENT: this.pollPassPercent,
      EventsManager.OPTED_IN: this.optedIn,
      EventsManager.TENTATIVE_ALGORITHM_CHOICES: this.tentativeAlgorithmChoices,
      EventsManager.SELECTED_CHOICE: this.selectedChoice,
      EventsManager.EVENT_CREATOR: this.eventCreator,
      EventsManager.VOTING_NUMBERS: this.votingNumbers
    };
  }

  @override
  String toString() {
    return "CategoryId: $categoryId CategoryName: $categoryName EventName: $eventName CreatedDateTime: "
        "$createdDateTime EventStartDateTime: $eventStartDateTime Type: $type PollDuration: $pollDuration "
        "PollPassPercent $pollPassPercent OptedIn: $optedIn SelectedChoice: $selectedChoice "
        "VotingNumbers: $votingNumbers TentativeAlgorithmChoices $tentativeAlgorithmChoices "
        "EventCreator: $eventCreator";
  }
}
