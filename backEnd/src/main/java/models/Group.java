package models;

import com.amazonaws.services.dynamodbv2.document.Item;
import imports.GroupsManager;
import java.math.BigDecimal;
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
public class Group {

  private String groupId;
  private String groupName;
  private String icon;
  private String groupCreator;
  private Integer defaultRsvpDuration;
  private Integer defaultVotingDuration;
  private Integer nextEventId;
  private String lastActivity;

  @Setter(AccessLevel.NONE)
  private Map<String, Member> members;
  @Setter(AccessLevel.NONE)
  private Map<String, String> categories;
  @Setter(AccessLevel.NONE)
  private Map<String, Event> events;

  public Group(final Map<String, Object> jsonMap) {
    this.setGroupId((String) jsonMap.get(GroupsManager.GROUP_ID));
    this.setGroupName((String) jsonMap.get(GroupsManager.GROUP_NAME));
    this.setIcon((String) jsonMap.get(GroupsManager.ICON));
    this.setGroupCreator((String) jsonMap.get(GroupsManager.GROUP_CREATOR));
    this.setDefaultRsvpDuration(
        this.getIntFromObject(jsonMap.get(GroupsManager.DEFAULT_RSVP_DURATION)));
    this.setDefaultVotingDuration(
        this.getIntFromObject(jsonMap.get(GroupsManager.DEFAULT_VOTING_DURATION)));
    this.setNextEventId(this.getIntFromObject(jsonMap.get(GroupsManager.NEXT_EVENT_ID)));
    this.setLastActivity((String) jsonMap.get(GroupsManager.LAST_ACTIVITY));

    this.setMembers((Map<String, Object>) jsonMap.get(GroupsManager.MEMBERS));
    this.setCategories((Map<String, Object>) jsonMap.get(GroupsManager.CATEGORIES));
    this.setEvents((Map<String, Object>) jsonMap.get(GroupsManager.EVENTS));
  }

  public Item asItem() {
    Item modelAsItem = Item.fromMap(this.asMap());

    //change the group id to be the primary key
    modelAsItem.removeAttribute(GroupsManager.GROUP_ID);
    modelAsItem.withPrimaryKey(GroupsManager.GROUP_ID, this.groupId);

    return modelAsItem;
  }

  public Map<String, Object> asMap() {
    Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.putIfAbsent(GroupsManager.GROUP_ID, this.groupId);
    modelAsMap.putIfAbsent(GroupsManager.GROUP_NAME, this.groupName);
    modelAsMap.putIfAbsent(GroupsManager.ICON, this.icon);
    modelAsMap.putIfAbsent(GroupsManager.GROUP_CREATOR, this.groupCreator);
    modelAsMap.putIfAbsent(GroupsManager.DEFAULT_RSVP_DURATION, this.defaultRsvpDuration);
    modelAsMap.putIfAbsent(GroupsManager.DEFAULT_VOTING_DURATION, this.defaultVotingDuration);
    modelAsMap.putIfAbsent(GroupsManager.NEXT_EVENT_ID, this.nextEventId);
    modelAsMap.putIfAbsent(GroupsManager.LAST_ACTIVITY, this.lastActivity);
    modelAsMap.putIfAbsent(GroupsManager.MEMBERS, this.getMembersMap());
    modelAsMap.putIfAbsent(GroupsManager.CATEGORIES, this.categories);
    modelAsMap.putIfAbsent(GroupsManager.EVENTS, this.getEventsMap());
    return modelAsMap;
  }

  public void setMembers(final Map<String, Object> jsonMap) {
    this.members = null;
    if (jsonMap != null) {
      this.members = new HashMap<>();
      for (String username : jsonMap.keySet()) {
        this.members.putIfAbsent(username, new Member((Map<String, Object>) jsonMap.get(username)));
      }
    }
  }

  public Map<String, Map<String, String>> getMembersMap() {
    final Map<String, Map<String, String>> membersMapped = new HashMap<>();
    for (String username : this.members.keySet()) {
      membersMapped.putIfAbsent(username, this.members.get(username).asMap());
    }
    return membersMapped;
  }

  public void setCategories(final Map<String, Object> jsonMap) {
    this.categories = null;
    if (jsonMap != null) {
      this.categories = new HashMap<>();
      for (String categoryId : jsonMap.keySet()) {
        this.categories.putIfAbsent(categoryId, (String) jsonMap.get(categoryId));
      }
    }
  }

  public void setEvents(final Map<String, Object> jsonMap) {
    this.events = null;
    if (jsonMap != null) {
      this.events = new HashMap<>();
      for (String username : jsonMap.keySet()) {
        this.events.putIfAbsent(username, new Event((Map<String, Object>) jsonMap.get(username)));
      }
    }
  }

  public Map<String, Map<String, Object>> getEventsMap() {
    Map<String, Map<String, Object>> eventsMapped = new HashMap<>();
    for (String eventId : this.events.keySet()) {
      eventsMapped.putIfAbsent(eventId, this.events.get(eventId).asMap());
    }
    return eventsMapped;
  }

  public boolean groupNameIsSet() {
    return this.groupName != null;
  }

  public boolean iconIsSet() {
    return this.icon != null;
  }

  public boolean lastActivityIsSet() {
    return this.lastActivity != null;
  }

  public Group clone() {
    return this.toBuilder()
        .groupId(this.groupId)
        .groupName(this.groupName)
        .icon(this.icon)
        .groupCreator(this.groupCreator)
        .defaultRsvpDuration(this.defaultRsvpDuration)
        .defaultVotingDuration(this.defaultVotingDuration)
        .nextEventId(this.nextEventId)
        .lastActivity(this.lastActivity)
        .members(this.members)
        .categories(this.categories)
        .events(this.events)
        .build();
  }

  private Integer getIntFromObject(final Object input) {
    if (input != null) {
      return Integer.parseInt(input.toString());
    }
    return null;
  }
}
