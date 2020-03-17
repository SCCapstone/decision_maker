package models;

import imports.GroupsManager;
import imports.UsersManager;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import utilities.RequestFields;

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
  private Map<String, Member> optedIn;

  @Setter(AccessLevel.NONE)
  private Map<String, String> tentativeAlgorithmChoices;
  @Setter(AccessLevel.NONE)
  private Map<String, Map<String, Integer>> votingNumbers;
  @Setter(AccessLevel.NONE)
  private Map<String, Map<String, String>> eventCreator;

  public Event(final Map<String, Object> jsonMap) {
    this.setCategoryId((String) jsonMap.get(GroupsManager.CATEGORY_ID));
    this.setCategoryName((String) jsonMap.get(GroupsManager.CATEGORY_NAME));
    this.setEventName((String) jsonMap.get(GroupsManager.EVENT_NAME));
    this.setCreatedDateTime((String) jsonMap.get(GroupsManager.CREATED_DATE_TIME));
    this.setEventStartDateTime((String) jsonMap.get(GroupsManager.EVENT_START_DATE_TIME));
    this.setRsvpDuration(this.getIntFromObject(jsonMap.get(GroupsManager.RSVP_DURATION)));
    this.setVotingDuration(this.getIntFromObject(jsonMap.get(GroupsManager.VOTING_DURATION)));
    this.setSelectedChoice((String) jsonMap.get(GroupsManager.SELECTED_CHOICE));

    this.setOptedInRawMap((Map<String, Object>) jsonMap.get(GroupsManager.OPTED_IN));
    this.setTentativeAlgorithmChoices(
        (Map<String, Object>) jsonMap.get(GroupsManager.TENTATIVE_CHOICES));
    this.setVotingNumbers((Map<String, Object>) jsonMap.get(GroupsManager.VOTING_NUMBERS));
    this.setEventCreator(jsonMap);
  }

  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.putIfAbsent(GroupsManager.CATEGORY_ID, this.categoryId);
    modelAsMap.putIfAbsent(GroupsManager.CATEGORY_NAME, this.categoryName);
    modelAsMap.putIfAbsent(GroupsManager.EVENT_NAME, this.eventName);
    modelAsMap.putIfAbsent(GroupsManager.CREATED_DATE_TIME, this.createdDateTime);
    modelAsMap.putIfAbsent(GroupsManager.EVENT_START_DATE_TIME, this.eventStartDateTime);
    modelAsMap.putIfAbsent(GroupsManager.RSVP_DURATION, this.rsvpDuration);
    modelAsMap.putIfAbsent(GroupsManager.VOTING_DURATION, this.votingDuration);
    modelAsMap.putIfAbsent(GroupsManager.SELECTED_CHOICE, this.selectedChoice);
    modelAsMap.putIfAbsent(GroupsManager.OPTED_IN, this.getOptedInMap());
    modelAsMap.putIfAbsent(GroupsManager.TENTATIVE_CHOICES, this.tentativeAlgorithmChoices);
    modelAsMap.putIfAbsent(GroupsManager.VOTING_NUMBERS, this.votingNumbers);
    modelAsMap.putIfAbsent(GroupsManager.EVENT_CREATOR, this.eventCreator);
    return modelAsMap;
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

  public Map<String, Map<String, String>> getOptedInMap() {
    final Map<String, Map<String, String>> membersMapped = new HashMap<>();
    for (String username : this.optedIn.keySet()) {
      membersMapped.putIfAbsent(username, this.optedIn.get(username).asMap());
    }
    return membersMapped;
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
              this.getIntFromObject(votingPairsRaw.get(username)));
        }

        this.votingNumbers.putIfAbsent(choiceId, votingPairs);
      }
    }
  }

  public void setEventCreator(final Map<String, Object> jsonMap) {
    this.eventCreator = null;
    if (jsonMap != null && jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
      this.eventCreator = new HashMap<>();
      this.eventCreator
          .putIfAbsent(activeUser, (Map<String, String>) jsonMap.get(activeUser));
    }
  }

  private Integer getIntFromObject(final Object input) {
    if (input != null) {
      return Integer.parseInt(input.toString());
    }
    return null;
  }
}
