import 'package:frontEnd/models/event.dart';

class EventsManager {
  static final String votingMode = "Voting";
  static final String optInMode = "OptIn";
  static final String pollFinishedMode = "Finished";
  static final String eventClosedMode = "Closed";
  static final String CATEGORY_ID = "CategoryId";
  static final String CATEGORY_NAME = "CategoryName";
  static final String EVENT_NAME = "EventName";
  static final String CREATED_DATE_TIME = "CreatedDateTime";
  static final String EVENT_START_DATE_TIME = "EventStartDateTime";
  static final String TYPE = "Type";
  static final String POLL_DURATION = "PollDuration";
  static final String POLL_PASS_PERCENT = "PollPassPercent";
  static final String OPTED_IN = "OptedIn";
  static final String TENTATIVE_ALGORITHM_CHOICES = "TentativeAlgorithmChoices";
  static final String SELECTED_CHOICE = "SelectedChoice";
  static final String VOTING_NUMBERS = "VotingNumbers";
  static final String EVENT_CREATOR = "EventCreator";

  static void updateEventMode(Event event) {
    DateTime timeNow = DateTime.now();
    DateTime createTime = event.createdDateTime;
    DateTime pollBegin =
        createTime.add(new Duration(minutes: event.pollDuration));
    DateTime pollFinished =
        createTime.add(new Duration(minutes: (event.pollDuration) * 2));
    DateTime eventClosed =
        event.eventStartDateTime.add(new Duration(hours: 24)); // assumption that after 24 hours the event is done
    if (timeNow.isBefore(pollBegin)) {
      event.mode = optInMode;
    } else if (timeNow.isAfter(pollBegin) && timeNow.isBefore(pollFinished)) {
      event.mode = votingMode;
    } else if (timeNow.isBefore(eventClosed)) {
      event.mode = pollFinishedMode;
    } else {
      event.mode = eventClosedMode;
    }
  }
}
