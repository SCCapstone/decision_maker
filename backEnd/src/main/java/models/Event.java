package models;

import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor // needed for the clone method to work
@Builder(toBuilder = true)
public class Event implements Model {

  public static final String EVENT_NAME = "EventName";
  public static final String EVENT_CREATOR = "EventCreator";
  public static final String CREATED_DATE_TIME = "CreatedDateTime";
  public static final String EVENT_START_DATE_TIME = "EventStartDateTime";
  public static final String UTC_EVENT_START_SECONDS = "UtcEventStartSeconds";
  public static final String VOTING_DURATION = "VotingDuration";
  public static final String RSVP_DURATION = "RsvpDuration";
  public static final String OPTED_IN = "OptedIn";
  public static final String VOTING_NUMBERS = "VotingNumbers";
  public static final String TENTATIVE_CHOICES = "TentativeAlgorithmChoices";
  public static final String SELECTED_CHOICE = "SelectedChoice";

  private String categoryId;
  private String categoryName;
  private String eventName;
  private String createdDateTime;
  private String eventStartDateTime;
  private Integer utcEventStartSeconds;
  private Integer rsvpDuration;
  private Integer votingDuration;
  private String selectedChoice;
  private Map<String, Member> optedIn;
  private Map<String, String> tentativeAlgorithmChoices;

  @Setter(AccessLevel.NONE)
  private Map<String, Map<String, Integer>> votingNumbers;
  @Setter(AccessLevel.NONE)
  private Map<String, Member> eventCreator;

  public Event(final Map<String, Object> jsonMap) {
    this.setCategoryId((String) jsonMap.get(Category.CATEGORY_ID));
    this.setCategoryName((String) jsonMap.get(Category.CATEGORY_NAME));
    this.setEventName((String) jsonMap.get(EVENT_NAME));
    this.setCreatedDateTime((String) jsonMap.get(CREATED_DATE_TIME));
    this.setEventStartDateTime((String) jsonMap.get(EVENT_START_DATE_TIME));
    this.setUtcEventStartSeconds(
        this.getIntFromObject(jsonMap.get(UTC_EVENT_START_SECONDS)));
    this.setRsvpDuration(this.getIntFromObject(jsonMap.get(RSVP_DURATION)));
    this.setVotingDuration(this.getIntFromObject(jsonMap.get(VOTING_DURATION)));
    this.setSelectedChoice((String) jsonMap.get(SELECTED_CHOICE));

    this.setOptedInRawMap((Map<String, Object>) jsonMap.get(OPTED_IN));
    this.setTentativeAlgorithmChoicesRawMap(
        (Map<String, Object>) jsonMap.get(TENTATIVE_CHOICES));
    this.setVotingNumbers((Map<String, Object>) jsonMap.get(VOTING_NUMBERS));
    this.setEventCreatorRawMap((Map<String, Object>) jsonMap.get(EVENT_CREATOR));
  }

  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.putIfAbsent(Category.CATEGORY_ID, this.categoryId);
    modelAsMap.putIfAbsent(Category.CATEGORY_NAME, this.categoryName);
    modelAsMap.putIfAbsent(EVENT_NAME, this.eventName);
    modelAsMap.putIfAbsent(CREATED_DATE_TIME, this.createdDateTime);
    modelAsMap.putIfAbsent(EVENT_START_DATE_TIME, this.eventStartDateTime);
    modelAsMap.putIfAbsent(UTC_EVENT_START_SECONDS, this.utcEventStartSeconds);
    modelAsMap.putIfAbsent(RSVP_DURATION, this.rsvpDuration);
    modelAsMap.putIfAbsent(VOTING_DURATION, this.votingDuration);
    modelAsMap.putIfAbsent(SELECTED_CHOICE, this.selectedChoice);
    modelAsMap.putIfAbsent(OPTED_IN, this.getOptedInMap());
    modelAsMap.putIfAbsent(TENTATIVE_CHOICES, this.tentativeAlgorithmChoices);
    modelAsMap.putIfAbsent(VOTING_NUMBERS, this.votingNumbers);
    modelAsMap.putIfAbsent(EVENT_CREATOR, this.getEventCreatorMap());
    return modelAsMap;
  }

  public Event clone() {
    return this.toBuilder()
        .categoryId(this.categoryId)
        .categoryName(this.categoryName)
        .eventName(this.eventName)
        .createdDateTime(this.createdDateTime)
        .eventStartDateTime(this.eventStartDateTime)
        .utcEventStartSeconds(this.utcEventStartSeconds)
        .rsvpDuration(this.rsvpDuration)
        .votingDuration(this.votingDuration)
        .selectedChoice(this.selectedChoice)
        .optedIn(this.optedIn)
        .tentativeAlgorithmChoices(this.tentativeAlgorithmChoices)
        .votingNumbers(this.votingNumbers)
        .eventCreator(this.eventCreator)
        .build();
  }

  public void setCategoryFields(final Category category) {
    this.categoryId = category.getCategoryId();
    this.categoryName = category.getCategoryName();
  }

  public void setOptedInRawMap(final Map<String, Object> jsonMap) {
    this.optedIn = null;
    if (jsonMap != null) {
      this.optedIn = new HashMap<>();
      for (String username : jsonMap.keySet()) {
        this.optedIn.putIfAbsent(username, new Member((Map<String, Object>) jsonMap.get(username)));
      }
    }
  }

  public Map<String, Map<String, Object>> getOptedInMap() {
    final Map<String, Map<String, Object>> membersMapped = new HashMap<>();
    for (String username : this.optedIn.keySet()) {
      membersMapped.putIfAbsent(username, this.optedIn.get(username).asMap());
    }
    return membersMapped;
  }

  public void setTentativeAlgorithmChoicesRawMap(final Map<String, Object> jsonMap) {
    this.tentativeAlgorithmChoices = null;
    if (jsonMap != null) {
      this.tentativeAlgorithmChoices = new HashMap<>();
      for (String choiceId : jsonMap.keySet()) {
        this.tentativeAlgorithmChoices.putIfAbsent(choiceId, (String) jsonMap.get(choiceId));
      }
    }
  }

  public void setVotingNumbers(final Map<String, Object> jsonMap) {
    this.votingNumbers = null;
    if (jsonMap != null) {
      this.votingNumbers = new HashMap<>();
      for (String choiceId : jsonMap.keySet()) {
        Map<String, Object> votingPairsRaw = (Map<String, Object>) jsonMap.get(choiceId);
        Map<String, Integer> votingPairs = new HashMap<>();

        for (String username : votingPairsRaw.keySet()) {
          votingPairs.putIfAbsent(username,
              this.getIntFromObject(votingPairsRaw.get(username)));
        }

        this.votingNumbers.putIfAbsent(choiceId, votingPairs);
      }
    }
  }

  public void setEventCreatorRawMap(final Map<String, Object> jsonMap) {
    this.eventCreator = null;
    if (jsonMap != null) {
      this.eventCreator = new HashMap<>();
      for (String username : jsonMap.keySet()) {
        this.eventCreator
            .putIfAbsent(username, new Member((Map<String, Object>) jsonMap.get(username)));
      }
    }
  }

  public void setEventCreator(final Map<String, Member> memberMap) {
    this.eventCreator = memberMap;
  }

  public Map<String, Map<String, Object>> getEventCreatorMap() {
    final Map<String, Map<String, Object>> eventCreatorMap = new HashMap<>();
    for (String username : this.eventCreator.keySet()) {
      eventCreatorMap.putIfAbsent(username, this.eventCreator.get(username).asMap());
    }
    return eventCreatorMap;
  }

  //assuming that event creator is always a single element map
  public String getEventCreatorUsername() {
    String username = null;

    if (this.eventCreator != null) {
      for (final String key : this.eventCreator.keySet()) {
        username = key;
      }
    }

    return username;
  }

  //assuming that event creator is always a single element map
  public String getEventCreatorDisplayName() {
    String displayName = null;

    if (this.eventCreator != null) {
      for (final String key : this.eventCreator.keySet()) {
        displayName = this.eventCreator.get(key).getDisplayName();
      }
    }

    return displayName;
  }

  private Integer getIntFromObject(final Object input) {
    if (input != null) {
      return Integer.parseInt(input.toString());
    }
    return null;
  }
}
