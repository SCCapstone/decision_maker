package models;

import imports.GroupsManager;
import imports.UsersManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserGroup {

  private String groupName;
  private String icon;
  private String lastActivity;
  private Boolean muted;
  private Map<String, Boolean> eventsUnseen;

  public UserGroup(final Map<String, Object> jsonMap) {
    this.setGroupName((String) jsonMap.get(GroupsManager.GROUP_NAME));
    this.setIcon((String) jsonMap.get(GroupsManager.ICON));
    this.setLastActivity((String) jsonMap.get(GroupsManager.LAST_ACTIVITY));
    this.setMuted(this.getBoolFromObject(jsonMap.get(UsersManager.APP_SETTINGS_MUTED)));
    this.setEventsUnseenRawMap((Map<String, Object>) jsonMap.get(UsersManager.EVENTS_UNSEEN));
  }

  public static UserGroup fromNewGroup(final Group newGroup) {
    return new UserGroup(
        newGroup.getGroupName(),
        newGroup.getIcon(),
        newGroup.getLastActivity(),
        false,
        Collections.emptyMap()
    );
  }

  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.putIfAbsent(GroupsManager.GROUP_NAME, this.groupName);
    modelAsMap.putIfAbsent(GroupsManager.ICON, this.icon);
    modelAsMap.putIfAbsent(GroupsManager.LAST_ACTIVITY, this.lastActivity);
    modelAsMap.putIfAbsent(UsersManager.APP_SETTINGS_MUTED, this.muted);
    modelAsMap.putIfAbsent(UsersManager.EVENTS_UNSEEN, this.eventsUnseen);
    return modelAsMap;
  }

  public void setEventsUnseenRawMap(final Map<String, Object> jsonMap) {
    this.eventsUnseen = null;
    if (jsonMap != null) {
      this.eventsUnseen = new HashMap<>();
      for (String eventId : jsonMap.keySet()) {
        this.eventsUnseen.put(eventId, true);
      }
    }
  }

  private Boolean getBoolFromObject(final Object input) {
    if (input != null) {
      return Boolean.parseBoolean(input.toString());
    }
    return null;
  }
}
