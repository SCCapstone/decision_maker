package models;

import imports.GroupsManager;
import imports.UsersManager;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class UserGroup {

  private String name;
  private String icon;
  private Boolean muted;
  private Map<String, Boolean> eventsUnseen;

  public UserGroup(final Map<String, Object> jsonMap) {
    this.setName((String) jsonMap.get(GroupsManager.GROUP_NAME));
    this.setIcon((String) jsonMap.get(GroupsManager.ICON));
    this.setMuted(this.getBoolFromObject(jsonMap.get(UsersManager.APP_SETTINGS_MUTED)));
    this.setEventsUnseenRawMap((Map<String, Object>) jsonMap.get(UsersManager.EVENTS_UNSEEN));
  }

  public void setEventsUnseenRawMap(final Map<String, Object> jsonMap) {
    this.eventsUnseen = null;
    if (jsonMap != null) {
      this.eventsUnseen = new HashMap<>();
      for (String eventId : jsonMap.keySet()) {
        this.eventsUnseen.put(eventId, this.getBoolFromObject(jsonMap.get(eventId)));
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
