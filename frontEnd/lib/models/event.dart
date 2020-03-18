import 'package:frontEnd/imports/events_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/users_manager.dart';

class Event {
  final String categoryId;
  final String categoryName;
  final String eventName;
  final DateTime createdDateTime;
  final DateTime eventStartDateTime;
  final DateTime pollBegin;
  final DateTime pollEnd;
  final int votingDuration;
  final int considerDuration;
  final Map<String, Map<String, String>> optedIn;
  final Map<String, String> tentativeAlgorithmChoices;
  final Map<String, Map<String, int>> votingNumbers;
  final Map<String, Map<String, String>> eventCreator;
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
      this.votingDuration,
      this.considerDuration,
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
      this.votingDuration,
      this.considerDuration,
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
    // we always add 1 to the end times since the chron job runs on the minute
    DateTime pollBeginTemp =
        DateTime.parse(json[EventsManager.CREATED_DATE_TIME]).toLocal().add(
            new Duration(minutes: (json[EventsManager.CONSIDER_DURATION] + 1)));
    /*
      Time in DB is in UTC, but dart parses by default as a local time. 
      So convert this local to UTC, then back to local to get the real local time.
     */
    DateTime pollBeginUTC = DateTime.utc(
        pollBeginTemp.year,
        pollBeginTemp.month,
        pollBeginTemp.day,
        pollBeginTemp.hour,
        pollBeginTemp.minute,
        pollBeginTemp.millisecond,
        pollBeginTemp.microsecond);
    DateTime pollEndTemp = pollBeginTemp
        .toLocal()
        .add(new Duration(minutes: (json[EventsManager.VOTING_DURATION] + 1)));
    DateTime pollEndUTC = DateTime.utc(
        pollEndTemp.year,
        pollEndTemp.month,
        pollEndTemp.day,
        pollEndTemp.hour,
        pollEndTemp.minute,
        pollEndTemp.millisecond,
        pollEndTemp.microsecond);
    // map of username to user info (icon, displayName)
    Map<String, Map<String, String>> optInMap =
        new Map<String, Map<String, String>>();
    for (String username in json[EventsManager.OPTED_IN].keys) {
      Map<String, String> memberInfo = new Map<String, String>();
      memberInfo.putIfAbsent(
          UsersManager.DISPLAY_NAME,
          () => json[EventsManager.OPTED_IN][username]
              [UsersManager.DISPLAY_NAME]);
      memberInfo.putIfAbsent(UsersManager.ICON,
          () => json[EventsManager.OPTED_IN][username][UsersManager.ICON]);
      optInMap.putIfAbsent(username, () => memberInfo);
    }
    Map<String, String> choicesMap = new Map<String, String>();
    for (String choiceId
        in json[EventsManager.TENTATIVE_ALGORITHM_CHOICES].keys) {
      choicesMap.putIfAbsent(
          choiceId,
          () => json[EventsManager.TENTATIVE_ALGORITHM_CHOICES][choiceId]
              .toString());
    }
    // map of username to user info (icon, displayName)
    Map<String, Map<String, String>> eventCreatorMap =
        new Map<String, Map<String, String>>();
    for (String username in json[EventsManager.EVENT_CREATOR].keys) {
      Map<String, String> memberInfo = new Map<String, String>();
      memberInfo.putIfAbsent(
          UsersManager.DISPLAY_NAME,
          () => json[EventsManager.EVENT_CREATOR][username]
              [UsersManager.DISPLAY_NAME]);
      memberInfo.putIfAbsent(UsersManager.ICON,
          () => json[EventsManager.EVENT_CREATOR][username][UsersManager.ICON]);
      eventCreatorMap.putIfAbsent(username, () => memberInfo);
    }
    // map of choiceId -> map of username and corresponding vote of said user
    Map<String, Map<String, int>> votingNumMap =
        new Map<String, Map<String, int>>();
    for (String choiceNum in json[EventsManager.VOTING_NUMBERS].keys) {
      for (String username
          in json[EventsManager.VOTING_NUMBERS][choiceNum].keys) {
        Map<String, int> voteInfo = new Map<String, int>();
        voteInfo.putIfAbsent(username,
            () => json[EventsManager.VOTING_NUMBERS][choiceNum][username]);
        votingNumMap.putIfAbsent(choiceNum, () => voteInfo);
      }
    }

    return Event(
        categoryId: json[EventsManager.CATEGORY_ID],
        categoryName: json[EventsManager.CATEGORY_NAME],
        eventName: json[EventsManager.EVENT_NAME],
        createdDateTime: DateTime.parse(json[EventsManager.CREATED_DATE_TIME]),
        eventStartDateTime:
            DateTime.parse(json[EventsManager.EVENT_START_DATE_TIME]),
        pollEnd: pollEndUTC.toLocal(),
        pollBegin: pollBeginUTC.toLocal(),
        votingDuration: json[EventsManager.VOTING_DURATION],
        considerDuration: json[EventsManager.CONSIDER_DURATION],
        optedIn: optInMap,
        tentativeAlgorithmChoices: choicesMap,
        selectedChoice: json[EventsManager.SELECTED_CHOICE],
        votingNumbers: votingNumMap,
        eventCreator: eventCreatorMap,
        eventStartDateTimeFormatted: Globals.formatter
            .format(DateTime.parse(json[EventsManager.EVENT_START_DATE_TIME])),
        pollEndFormatted: Globals.formatter.format(pollEndUTC.toLocal()),
        pollBeginFormatted: Globals.formatter.format(pollBeginUTC.toLocal()));
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
      EventsManager.VOTING_DURATION: this.votingDuration,
      EventsManager.CONSIDER_DURATION: this.considerDuration,
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
        "$createdDateTime EventStartDateTime: $eventStartDateTime PollDuration: $votingDuration "
        "ConsiderDuration: $considerDuration OptedIn: $optedIn SelectedChoice: $selectedChoice VotingNumbers: $votingNumbers "
        "TentativeAlgorithmChoices $tentativeAlgorithmChoices EventCreator: $eventCreator";
  }
}
