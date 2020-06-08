package models;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import lombok.Data;

@Data
public class EventForSorting extends Event {

  private LocalDateTime eventStart;
  private LocalDateTime votingStarts;
  private LocalDateTime votingEnds;
  private Integer priority;
  private boolean isPending;
  private String eventId;

  public static final Integer PRIORITY_VOTING = 4;
  public static final Integer PRIORITY_CONSIDERING = 3;
  public static final Integer PRIORITY_OCCURRING = 2;
  public static final Integer PRIORITY_CLOSED = 1;

  //we pass in 'now' so that if numerous of these are being created, that time doesn't change
  public EventForSorting(final String eventId, final Event event, final LocalDateTime now) {
    super(event.asMap());

    this.eventId = eventId;

    Integer eventStartSeconds = this.getUtcEventStartSeconds();
    this.eventStart = this.getLocalDateTimeFromEpochSeconds(eventStartSeconds);

    eventStartSeconds += this.getRsvpDuration() * 60; // 60 seconds in a minute
    this.votingStarts = this.getLocalDateTimeFromEpochSeconds(eventStartSeconds);

    eventStartSeconds += this.getVotingDuration() * 60; // 60 seconds in a minute
    this.votingEnds = this.getLocalDateTimeFromEpochSeconds(eventStartSeconds);

    this.setPriorityFromOwnProperties(now);
  }

  //use this one when the now doesn't matter (aka when we just want to know if the event is pending)
  public EventForSorting(final String eventId, final Event event) {
    super(event.asMap());

    this.eventId = eventId;

    Integer eventStartSeconds = this.getUtcEventStartSeconds();
    this.eventStart = this.getLocalDateTimeFromEpochSeconds(eventStartSeconds);

    eventStartSeconds += this.getRsvpDuration() * 60; // 60 seconds in a minute
    this.votingStarts = this.getLocalDateTimeFromEpochSeconds(eventStartSeconds);

    eventStartSeconds += this.getVotingDuration() * 60; // 60 seconds in a minute
    this.votingEnds = this.getLocalDateTimeFromEpochSeconds(eventStartSeconds);

    this.setPriorityFromOwnProperties(LocalDateTime.now(ZoneId.of("UTC")));
  }

  private LocalDateTime getLocalDateTimeFromEpochSeconds(final Integer seconds) {
    return LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.UTC);
  }

  /**
   * We define priority first by precedence of action and second by the required action time. Voting
   * is the most urgent action so it has the highest precedence. Between two events in voting,
   * whichever vote ends first is more pressing. Consider is the next most urgent action so it has
   * the second highest precedence. Same between two considering as with voting. Next we have two
   * items that have finished polling - they have a selected choice. If 'now' is within 24hrs of the
   * event, the event is "occurring". After that we have events that are closed.
   * <p>
   * Events are ordered by their closeness to 'now', so events in the future are ordered youngest to
   * oldest. In contrast, events occurring in the past are ordered oldest to youngest.
   * <p>
   * Always return -1 when 'this' object should be ordered in front of the 'other' object (else 1)
   */
  public int compareTo(final EventForSorting other) {
    if (this.priority > other.getPriority()) {
      return -1; // we want this at the front of the list
    } else if (other.getPriority() > this.priority) {
      return 1;
    } else {
      if (this.priority.equals(PRIORITY_VOTING)) {
        if (this.votingEnds.isBefore(other.getVotingEnds())) {
          return -1;
        } else if (other.getVotingEnds().isBefore(this.votingEnds)) {
          return 1;
        } else {
          //they occur at the same time, give sort based on event id
          return this.eventId.compareTo(other.getEventId());
        }
      } else if (this.priority.equals(PRIORITY_CONSIDERING)) {
        if (this.votingStarts.isBefore(other.getVotingStarts())) {
          return -1;
        } else if (other.getVotingStarts().isBefore(this.votingStarts)) {
          return 1;
        } else {
          //they occur at the same time, give sort based on event id
          return this.eventId.compareTo(other.getEventId());
        }
      } else if (this.priority.equals(PRIORITY_CLOSED)) {
        //NOTE: this one uses isAfter, we want the oldest ones on top
        if (this.eventStart.isAfter(other.getEventStart())) {
          return -1;
        } else if (other.getEventStart().isAfter(this.eventStart)) {
          return 1;
        } else {
          //they occur at the same time, give sort based on event id
          return this.eventId.compareTo(other.getEventId());
        }
      } else { // occurring
        if (this.eventStart.isBefore(other.getEventStart())) {
          return -1;
        } else if (other.getEventStart().isBefore(this.eventStart)) {
          return 1;
        } else {
          //they occur at the same time, give sort based on event id
          return this.eventId.compareTo(other.getEventId());
        }
      }
    }
  }

  /**
   * This method looks at the settings of 'this' event and determines the priority appropriately
   *
   * @param now - The current time that we are determining this priority at.
   */
  private void setPriorityFromOwnProperties(final LocalDateTime now) {
    if (this.getSelectedChoice() != null) {
      //the event is finalized - it has a selected choice
      final LocalDateTime yesterday = now.minus(12, ChronoUnit.HOURS);
      if (this.eventStart.isAfter(yesterday)) {
        this.priority = PRIORITY_OCCURRING;
      } else {
        this.priority = PRIORITY_CLOSED;
      }
      this.isPending = false;
    } else if (this.getTentativeAlgorithmChoices().isEmpty()) {
      // selected choice null and tentative empty => considering
      this.priority = PRIORITY_CONSIDERING;
      this.isPending = true;
    } else {
      // selected choice null and tentative not empty => voting
      this.priority = PRIORITY_VOTING;
      this.isPending = true;
    }
  }
}
