package models;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import lombok.Data;

@Data
public class EventForSorting extends Event {

  private LocalDateTime eventStart;
  private LocalDateTime votingStarts;
  private LocalDateTime votingEnds;
  private Integer priority;

  public static final Integer PRIORITY_VOTING = 4;
  public static final Integer PRIORITY_CONSIDERING = 3;
  public static final Integer PRIORITY_OCCURRING = 2;
  public static final Integer PRIORITY_FUTURE = 1;
  public static final Integer PRIORITY_PAST = 0;

  //we pass in 'now' so that if numerous of these are being created, that time doesn't change
  public EventForSorting(final Event event, final LocalDateTime now) {
    super(event.asMap());

    Integer eventStartSeconds = this.getUtcEventStartSeconds();
    this.eventStart = this.getLocalDateTimeFromEpochSeconds(eventStartSeconds);

    eventStartSeconds += this.getRsvpDuration() * 60; // 60 seconds in a minute
    this.votingStarts = this.getLocalDateTimeFromEpochSeconds(eventStartSeconds);

    eventStartSeconds += this.getVotingDuration() * 60; // 60 seconds in a minute
    this.votingEnds = this.getLocalDateTimeFromEpochSeconds(eventStartSeconds);

    this.setPriorityFromOwnProperties(now);
  }

  private LocalDateTime getLocalDateTimeFromEpochSeconds(final Integer seconds) {
    return LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.UTC);
  }

  /**
   * We define priority first by precedence of action and second by the required action time Voting
   * is the most urgent action so it has the highest precedence Between two events in voting,
   * whichever vote ends first is more pressing Considers is the next most urgent action so it has
   * the second highest precedence Same between two considering as with voting Next we have two
   * items that have finished polling - they have a selected choice If 'now' is within 24hrs of the
   * event, the event is "occurring" and should come before those events that are past 24hrs and are
   * "closed"
   * <p>
   * In addition, events that are behind 'now' come after those that are before 'now'.
   * <p>
   * Events are ordered by their closeness to 'now' so and event occurring tomorrow comes before one
   * occurring in two days, and both of these come before one that happened yesterday based on the
   * above rule
   */
  public int compareTo(final EventForSorting other) {
    if (this.priority > other.getPriority()) {
      return -1; // we want this at the front of the list
    } else if (other.getPriority() > this.priority) {
      return 1;
    } else {
      if (this.priority.equals(PRIORITY_VOTING)) {
        return this.votingEnds.isBefore(other.getVotingEnds()) ? -1 : 1;
      } else if (this.priority.equals(PRIORITY_CONSIDERING)) {
        return this.votingStarts.isBefore(other.getVotingStarts()) ? -1 : 1;
      } else if (this.priority.equals(PRIORITY_PAST)) {
        //NOTE: this one uses isAfter, we want the oldest ones on top
        return this.eventStart.isAfter(other.getEventStart()) ? -1 : 1;
      } else { // occurring and future
        return this.eventStart.isBefore(other.getVotingStarts()) ? -1 : 1;
      }
    }
  }

  /**
   * Priority values are as follows: 4 - voting 3 - considering 2 - occurring 1 - closed and starts
   * after 'now' 0 - closed and started before 'now'
   *
   * @param now - The current time that we are determining this priority at.
   */
  private void setPriorityFromOwnProperties(final LocalDateTime now) {
    if (this.getSelectedChoice() != null) {
      //we are either in 0, 1, or 2
      final LocalDateTime yesterday = now.minus(1, ChronoUnit.DAYS);
      if (now.isBefore(now) && now.isAfter(yesterday)) { // is occurring?
        this.priority = PRIORITY_OCCURRING;
      } else if (this.eventStart.isAfter(now)) {
        this.priority = PRIORITY_FUTURE;
      } else {
        this.priority = PRIORITY_PAST;
      }
    } else if (this.getTentativeAlgorithmChoices().isEmpty()) {
      // selected choice null and tentative empty = considering
      this.priority = PRIORITY_CONSIDERING;
    } else {
      // selected choice null and tentative not empty = voting
      this.priority = PRIORITY_VOTING;
    }
  }


}
