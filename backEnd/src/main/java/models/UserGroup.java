package models;

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
  private boolean muted;
  private Map<String, Boolean> eventsUnseen;

  public UserGroup(final Map<String, Object> jsonMap) {
    this.setGroupName((String) jsonMap.get(Group.GROUP_NAME));
    this.setIcon((String) jsonMap.get(Group.ICON));
    this.setLastActivity((String) jsonMap.get(Group.LAST_ACTIVITY));
    this.setMuted(this.getBoolFromObject(jsonMap.get(AppSettings.MUTED)));
    this.setEventsUnseenRawMap((Map<String, Object>) jsonMap.get(User.EVENTS_UNSEEN));
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
    modelAsMap.putIfAbsent(Group.GROUP_NAME, this.groupName);
    modelAsMap.putIfAbsent(Group.ICON, this.icon);
    modelAsMap.putIfAbsent(Group.LAST_ACTIVITY, this.lastActivity);
    modelAsMap.putIfAbsent(AppSettings.MUTED, this.muted);
    modelAsMap.putIfAbsent(User.EVENTS_UNSEEN, this.eventsUnseen);
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

  private boolean getBoolFromObject(final Object input) {
    if (input != null) {
      return Boolean.parseBoolean(input.toString());
    }
    return false;
  }
}
