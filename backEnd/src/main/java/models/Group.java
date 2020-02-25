package models;

import imports.GroupsManager;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class Group {

  private String groupId;
  private String groupName;
  private String icon;
  private Integer defaultRsvpDuration;
  private Integer defaultVotingDuration;
  private Integer nextEventId;

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
    this.setDefaultRsvpDuration((Integer) jsonMap.get(GroupsManager.DEFAULT_RSVP_DURATION));
    this.setDefaultRsvpDuration((Integer) jsonMap.get(GroupsManager.DEFAULT_VOTING_DURATION));
    this.setDefaultRsvpDuration((Integer) jsonMap.get(GroupsManager.NEXT_EVENT_ID));

    this.setMembers((Map<String, Object>) jsonMap.get(GroupsManager.MEMBERS));
    this.setCategories((Map<String, Object>) jsonMap.get(GroupsManager.CATEGORIES));
    this.setEvents((Map<String, Object>) jsonMap.get(GroupsManager.MEMBERS));
  }

  public void setMembers(Map<String, Object> jsonMap) {
    if (jsonMap != null) {
      this.members = new HashMap<>();
      for (String username : jsonMap.keySet()) {
        this.members.putIfAbsent(username, new Member((Map<String, Object>) jsonMap.get(username)));
      }
    }
  }

  public void setCategories(Map<String, Object> jsonMap) {
    if (jsonMap != null) {
      this.categories = new HashMap<>();
      for (String categoryId : jsonMap.keySet()) {
        this.categories.putIfAbsent(categoryId, (String) jsonMap.get(categoryId));
      }
    }
  }

  public void setEvents(Map<String, Object> jsonMap) {
    if (jsonMap != null) {
      this.events = new HashMap<>();
      for (String username : jsonMap.keySet()) {
        this.events.putIfAbsent(username, new Event((Map<String, Object>) jsonMap.get(username)));
      }
    }
  }
}
