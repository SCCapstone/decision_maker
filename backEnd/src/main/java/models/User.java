package models;

import imports.UsersManager;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class User {

  private String username;
  private String displayName;
  private String icon;
  private String pushEndpointArn;
  private AppSettings appSettings;

  @Setter(AccessLevel.NONE)
  private Map<String, Group> groups;
  //TODO add categories and owned categories once we add Categories model https://github.com/SCCapstone/decision_maker/issues/306
  @Setter(AccessLevel.NONE)
  private Map<String, Boolean> favoriteOf;
  @Setter(AccessLevel.NONE)
  private Map<String, Favorite> favorites;

  public User(final Map<String, Object> jsonMap) {
    this.setUsername((String) jsonMap.get(UsersManager.USERNAME));
    this.setDisplayName((String) jsonMap.get(UsersManager.DISPLAY_NAME));
    this.setIcon((String) jsonMap.get(UsersManager.ICON));
    this.setPushEndpointArn((String) jsonMap.get(UsersManager.PUSH_ENDPOINT_ARN));
    this.setAppSettings(
        new AppSettings((Map<String, Object>) jsonMap.get(UsersManager.APP_SETTINGS)));
    this.setGroups((Map<String, Object>) jsonMap.get(UsersManager.GROUPS));
    this.setFavoriteOf((Map<String, Object>) jsonMap.get(UsersManager.FAVORITE_OF));
    this.setFavorites((Map<String, Object>) jsonMap.get(UsersManager.FAVORITES));
  }

  public void setGroups(final Map<String, Object> jsonMap) {
    this.groups = null;
    if (!jsonMap.isEmpty()) {
      this.groups = new HashMap<>();
      for (String groupId: jsonMap.keySet()) {
        this.groups.putIfAbsent(groupId, new Group((Map<String, Object>) jsonMap.get(groupId)));
      }
    }
  }

  public void setFavoriteOf(final Map<String, Object> jsonMap) {
    this.favoriteOf = null;
    if (jsonMap != null) {
      this.favoriteOf = new HashMap<>();
      for (String username : jsonMap.keySet()) {
        this.favoriteOf.putIfAbsent(username, true);
      }
    }
  }

  public void setFavorites(final Map<String, Object> jsonMap) {
    this.favorites = null;
    if (jsonMap != null) {
      this.favorites = new HashMap<>();
      for (String username : jsonMap.keySet()) {
        this.favorites.putIfAbsent(username, new Favorite((Map<String, Object>) jsonMap.get(username)));
      }
    }
  }

  public boolean pushEndpointArnIsSet() {
    return this.pushEndpointArn != null;
  }
}
