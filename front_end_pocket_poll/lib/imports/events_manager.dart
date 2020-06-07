import 'package:front_end_pocket_poll/models/event.dart';

class EventsManager {
  static final int votingMode = 4; // highest priority for sorting
  static final int considerMode = 3;
  static final int occurringMode = 2;
  static final int closedMode = 1; // lowest priority for sorting
  static final String EVENT_ID = "EventId";
  static final String CATEGORY_ID = "CategoryId";
  static final String CATEGORY_NAME = "CategoryName";
  static final String EVENT_NAME = "EventName";
  static final String CREATED_DATE_TIME = "CreatedDateTime";
  static final String EVENT_START_DATE_TIME = "EventStartDateTime";
  static final String UTC_EVENT_START_SECONDS = "UtcEventStartSeconds";
  static final String TYPE = "Type";
  static final String VOTING_DURATION = "VotingDuration";
  static final String CONSIDER_DURATION = "RsvpDuration";
  static final String POLL_PASS_PERCENT = "PollPassPercent";
  static final String OPTED_IN = "OptedIn";
  static final String TENTATIVE_ALGORITHM_CHOICES = "TentativeAlgorithmChoices";
  static final String SELECTED_CHOICE = "SelectedChoice";
  static final String VOTING_NUMBERS = "VotingNumbers";
  static final String EVENT_CREATOR = "EventCreator";
  static final String CATEGORY_VERSION = "Version";

  // returns what mode an event is currently in
  static int getEventMode(Event event) {
    DateTime timeNow = DateTime.now();
    DateTime eventClosed = event.eventStartDateTime.add(new Duration(
        hours: 12)); // assumption that after 12 hours the event is done
    int retVal;
    if (event.tentativeAlgorithmChoices.isEmpty) {
      retVal = considerMode;
    } else if (event.selectedChoice == null) {
      retVal = votingMode;
    } else if (event.selectedChoice.isNotEmpty &&
        timeNow.isBefore(eventClosed)) {
      retVal = occurringMode;
    } else if (timeNow.isAfter(eventClosed)) {
      retVal = closedMode;
    }
    return retVal;
  }
}
