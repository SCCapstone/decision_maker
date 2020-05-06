package models;

import com.amazonaws.services.dynamodbv2.document.Item;
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
public class Group implements Model {

  public static final String GROUP_ID = "GroupId";
  public static final String GROUP_NAME = "GroupName";
  public static final String ICON = "Icon";
  public static final String GROUP_CREATOR = "GroupCreator";
  public static final String MEMBERS = "Members";
  public static final String MEMBERS_LEFT = "MembersLeft";
  public static final String CATEGORIES = "Categories";
  public static final String DEFAULT_VOTING_DURATION = "DefaultVotingDuration";
  public static final String DEFAULT_RSVP_DURATION = "DefaultRsvpDuration";
  public static final String EVENTS = "Events";
  public static final String LAST_ACTIVITY = "LastActivity";
  public static final String IS_OPEN = "IsOpen";

  private String groupId;
  private String groupName;
  private String icon;
  private String groupCreator;
  private Integer defaultRsvpDuration;
  private Integer defaultVotingDuration;
  private String lastActivity;
  private Map<String, Event> events;
  private boolean isOpen;

  @Setter(AccessLevel.NONE)
  private Map<String, Member> members;
  @Setter(AccessLevel.NONE)
  private Map<String, Boolean> membersLeft;
  @Setter(AccessLevel.NONE)
  private Map<String, String> categories;

  public Group(final Item groupItem) {
    this(groupItem.asMap());
  }

  public Group(final Map<String, Object> jsonMap) {
    this.setGroupId((String) jsonMap.get(GROUP_ID));
    this.setGroupName((String) jsonMap.get(GROUP_NAME));
    this.setIcon((String) jsonMap.get(ICON));
    this.setGroupCreator((String) jsonMap.get(GROUP_CREATOR));
    this.setDefaultRsvpDuration(
        this.getIntFromObject(jsonMap.get(DEFAULT_RSVP_DURATION)));
    this.setDefaultVotingDuration(
        this.getIntFromObject(jsonMap.get(DEFAULT_VOTING_DURATION)));
    this.setLastActivity((String) jsonMap.get(LAST_ACTIVITY));
    this.setOpen(this.getBoolFromObject(jsonMap.get(IS_OPEN)));

    this.setMembers((Map<String, Object>) jsonMap.get(MEMBERS));
    this.setMembersLeft((Map<String, Object>) jsonMap.get(MEMBERS_LEFT));
    this.setCategories((Map<String, Object>) jsonMap.get(CATEGORIES));
    this.setEventsRawMap((Map<String, Object>) jsonMap.get(EVENTS));
  }

  public Item asItem() {
    Item modelAsItem = Item.fromMap(this.asMap());

    //change the group id to be the primary key
    modelAsItem.removeAttribute(GROUP_ID);
    modelAsItem.withPrimaryKey(GROUP_ID, this.groupId);

    return modelAsItem;
  }

  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.putIfAbsent(GROUP_ID, this.groupId);
    modelAsMap.putIfAbsent(GROUP_NAME, this.groupName);
    modelAsMap.putIfAbsent(ICON, this.icon);
    modelAsMap.putIfAbsent(GROUP_CREATOR, this.groupCreator);
    modelAsMap.putIfAbsent(DEFAULT_RSVP_DURATION, this.defaultRsvpDuration);
    modelAsMap.putIfAbsent(DEFAULT_VOTING_DURATION, this.defaultVotingDuration);
    modelAsMap.putIfAbsent(LAST_ACTIVITY, this.lastActivity);
    modelAsMap.putIfAbsent(IS_OPEN, this.isOpen);
    modelAsMap.putIfAbsent(MEMBERS, this.getMembersMap());
    modelAsMap.putIfAbsent(MEMBERS_LEFT, this.membersLeft);
    modelAsMap.putIfAbsent(CATEGORIES, this.categories);
    modelAsMap.putIfAbsent(EVENTS, this.getEventsMap());
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

  public void setMembersLeft(final Map<String, Object> jsonMap) {
    this.membersLeft = null;
    if (jsonMap != null) {
      this.membersLeft = new HashMap<>();
      for (String username : jsonMap.keySet()) {
        this.membersLeft.putIfAbsent(username, true);
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

  public void setEventsRawMap(final Map<String, Object> jsonMap) {
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

  private boolean getBoolFromObject(final Object input) {
    if (input != null) {
      return Boolean.parseBoolean(input.toString());
    }
    return false;
  }
}
