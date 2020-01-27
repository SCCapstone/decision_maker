import 'package:frontEnd/models/event.dart';

class EventsManager {
  static final String votingMode = "Voting";
  static final String rsvpMode = "Rsvp";
  static final String occurringMode = "Ocurring";
  static final String closedMode = "Closed";
  static final String EVENT_ID = "EventId";
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

  static String getEventMode(Event event) {
    DateTime timeNow = DateTime.now();
    DateTime eventClosed = event.eventStartDateTime.add(new Duration(
        hours: 24)); // assumption that after 24 hours the event is done
    // TODO until backend handlers are complete, alter the below string to test the different stages (https://github.com/SCCapstone/decision_maker/issues/178)
    String retVal = votingMode;
//    if (event.tentativeAlgorithmChoices == null) {
//      retVal = rsvpMode;
//    } else if (event.selectedChoice == null) {
//      retVal = votingMode;
//    } else if (event.selectedChoice.isNotEmpty &&
//        timeNow.isBefore(eventClosed)) {
//      retVal = occurringMode;
//    } else if (timeNow.isAfter(eventClosed)) {
//      retVal = closedMode;
//    }
    return retVal;
  }
}
