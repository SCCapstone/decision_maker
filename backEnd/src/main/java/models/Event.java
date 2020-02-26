package models;

import imports.GroupsManager;
import imports.UsersManager;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class Event {

  private String categoryId;
  private String categoryName;
  private String eventName;
  private String createdDateTime;
  private String eventStartDateTime;
  private Integer rsvpDuration;
  private Integer votingDuration;
  private String selectedChoice;

  @Setter(AccessLevel.NONE)
  private Map<String, Member> optedIn;
  @Setter(AccessLevel.NONE)
  private Map<String, String> tentativeAlgorithmChoices;
  @Setter(AccessLevel.NONE)
  private Map<String, Map<String, Integer>> votingNumbers;
  @Setter(AccessLevel.NONE)
  private Map<String, String> eventCreator;

  public Event(final Map<String, Object> jsonMap) {
    this.setCategoryId((String) jsonMap.get(GroupsManager.CATEGORY_ID));
    this.setCategoryName((String) jsonMap.get(GroupsManager.CATEGORY_NAME));
    this.setEventName((String) jsonMap.get(GroupsManager.EVENT_NAME));
    this.setCreatedDateTime((String) jsonMap.get(GroupsManager.CREATED_DATE_TIME));
    this.setEventStartDateTime((String) jsonMap.get(GroupsManager.EVENT_START_DATE_TIME));
    this.setRsvpDuration(
        this.getIntFromBigInt((BigDecimal) jsonMap.get(GroupsManager.RSVP_DURATION)));
    this.setVotingDuration(
        this.getIntFromBigInt((BigDecimal) jsonMap.get(GroupsManager.VOTING_DURATION)));
    this.setSelectedChoice((String) jsonMap.get(GroupsManager.SELECTED_CHOICE));

    this.setOptedIn((Map<String, Object>) jsonMap.get(GroupsManager.OPTED_IN));
    this.setTentativeAlgorithmChoices(
        (Map<String, Object>) jsonMap.get(GroupsManager.TENTATIVE_CHOICES));
    this.setVotingNumbers((Map<String, Object>) jsonMap.get(GroupsManager.VOTING_NUMBERS));
    this.setEventCreator((Map<String, Object>) jsonMap.get(GroupsManager.EVENT_CREATOR));
  }

  public void setOptedIn(final Map<String, Object> jsonMap) {
    this.optedIn = null;
    if (jsonMap != null) {
      this.optedIn = new HashMap<>();
      for (String username : jsonMap.keySet()) {
        this.optedIn.putIfAbsent(username, new Member((Map<String, Object>) jsonMap.get(username)));
      }
    }
  }

  public void setTentativeAlgorithmChoices(final Map<String, Object> jsonMap) {
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
              this.getIntFromBigInt((BigDecimal) votingPairsRaw.get(username)));
        }

        this.votingNumbers.putIfAbsent(choiceId, votingPairs);
      }
    }
  }

  public void setEventCreator(final Map<String, Object> jsonMap) {
    this.eventCreator = null;
    if (jsonMap != null) {
      this.eventCreator = new HashMap<>();
      this.eventCreator
          .putIfAbsent(UsersManager.USERNAME, (String) jsonMap.get(UsersManager.USERNAME));
    }
  }

  private Integer getIntFromBigInt(final BigDecimal input) {
    if (input != null) {
      return input.intValue();
    }
    return null;
  }
}
