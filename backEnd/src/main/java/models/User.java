package models;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import com.amazonaws.services.dynamodbv2.document.Item;
import exceptions.InvalidAttributeValueException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class User {

  public static final String USERNAME = "Username";
  public static final String DISPLAY_NAME = "DisplayName";
  public static final String ICON = "Icon";
  public static final String APP_SETTINGS = "AppSettings";
  public static final String GROUPS = "Groups";
  public static final String GROUPS_LEFT = "GroupsLeft";
  public static final String CATEGORY_RATINGS = "CategoryRatings";
  public static final String OWNED_CATEGORIES = "OwnedCategories";
  public static final String FAVORITES = "Favorites";
  public static final String FAVORITE_OF = "FavoriteOf";
  public static final String PUSH_ENDPOINT_ARN = "PushEndpointArn";
  public static final String EVENTS_UNSEEN = "EventsUnseen";

  private String username;
  private String displayName;
  private String icon;
  private String pushEndpointArn;
  private AppSettings appSettings;

  @Setter(AccessLevel.NONE)
  private Map<String, UserGroup> groups;
  @Setter(AccessLevel.NONE)
  private Map<String, Group> groupsLeft;
  @Setter(AccessLevel.NONE)
  private Map<String, UserRatings> categoryRatings;
  @Setter(AccessLevel.NONE)
  private Map<String, String> ownedCategories;
  @Setter(AccessLevel.NONE)
  private Map<String, Boolean> favoriteOf;
  @Setter(AccessLevel.NONE)
  private Map<String, Favorite> favorites;

  public User(final Item userItem) throws InvalidAttributeValueException {
    this(userItem.asMap());
  }

  public User(final Map<String, Object> jsonMap) throws InvalidAttributeValueException {
    this.setUsername((String) jsonMap.get(USERNAME));
    this.setDisplayName((String) jsonMap.get(DISPLAY_NAME));
    this.setIcon((String) jsonMap.get(ICON));
    this.setPushEndpointArn((String) jsonMap.get(PUSH_ENDPOINT_ARN));
    this.setAppSettings(
        new AppSettings((Map<String, Object>) jsonMap.get(APP_SETTINGS)));
    this.setGroups((Map<String, Object>) jsonMap.get(GROUPS));
    this.setGroupsLeft((Map<String, Object>) jsonMap.get(GROUPS_LEFT));
    this.setCategoryRatings((Map<String, Object>) jsonMap.get(CATEGORY_RATINGS));
    this.setOwnedCategories((Map<String, Object>) jsonMap.get(OWNED_CATEGORIES));
    this.setFavoriteOf((Map<String, Object>) jsonMap.get(FAVORITE_OF));
    this.setFavorites((Map<String, Object>) jsonMap.get(FAVORITES));
  }

  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.putIfAbsent(USERNAME, this.username);
    modelAsMap.putIfAbsent(DISPLAY_NAME, this.displayName);
    modelAsMap.putIfAbsent(ICON, this.icon);
    modelAsMap.putIfAbsent(PUSH_ENDPOINT_ARN, this.pushEndpointArn);
    modelAsMap.putIfAbsent(APP_SETTINGS, this.appSettings.asMap());
    modelAsMap.putIfAbsent(GROUPS, this.getGroupsMap());
    modelAsMap.putIfAbsent(GROUPS_LEFT, this.getGroupsLeftMap());
    modelAsMap.putIfAbsent(CATEGORY_RATINGS, this.getCategoryRatingsMap());
    modelAsMap.putIfAbsent(OWNED_CATEGORIES, this.ownedCategories);
    modelAsMap.putIfAbsent(FAVORITE_OF, this.favoriteOf);
    modelAsMap.putIfAbsent(FAVORITES, this.getFavoritesMap());
    return modelAsMap;
  }

  public Map<String, Map<String, Object>> getGroupsMap() {
    if (this.groups == null) {
      return null;
    }

    return this.groups.entrySet().stream().collect(
        collectingAndThen(
            toMap(Entry::getKey, (Map.Entry<String, UserGroup> e) -> e.getValue().asMap()),
            HashMap::new));
  }

  public Map<String, Map<String, Object>> getGroupsLeftMap() {
    if (this.groupsLeft == null) {
      return null;
    }

    return this.groupsLeft.entrySet().stream().collect(
        collectingAndThen(
            toMap(Entry::getKey, (Map.Entry<String, Group> e) -> e.getValue().asGroupLeftMap()),
            HashMap::new));
  }

  public Map<String, Map<String, Object>> getFavoritesMap() {
    if (this.favorites == null) {
      return null;
    }

    return this.favorites.entrySet().stream().collect(
        collectingAndThen(
            toMap(Entry::getKey, (Map.Entry<String, Favorite> e) -> e.getValue().asMap()),
            HashMap::new));
  }

  public Map<String, Map<String, Object>> getCategoryRatingsMap() {
    if (this.categoryRatings == null) {
      return null;
    }

    return this.categoryRatings.entrySet().stream().collect(
        collectingAndThen(
            toMap(Entry::getKey, (Map.Entry<String, UserRatings> e) -> e.getValue().asMap()),
            HashMap::new));
  }

  public Member asMember() {
    return new Member(this.displayName, this.icon);
  }

  public void setGroups(final Map<String, Object> jsonMap) {
    this.groups = null;
    if (jsonMap != null) {
      this.groups = new HashMap<>();
      for (String groupId : jsonMap.keySet()) {
        this.groups.putIfAbsent(groupId, new UserGroup((Map<String, Object>) jsonMap.get(groupId)));
      }
    }
  }

  public void setGroupsLeft(final Map<String, Object> jsonMap) {
    this.groupsLeft = null;
    if (jsonMap != null) {
      this.groupsLeft = new HashMap<>();
      for (String groupId : jsonMap.keySet()) {
        this.groupsLeft.putIfAbsent(groupId, new Group((Map<String, Object>) jsonMap.get(groupId)));
      }
    }
  }

  public void setCategoryRatings(final Map<String, Object> jsonMap) {
    this.categoryRatings = null;
    if (jsonMap != null) {
      this.categoryRatings = new HashMap<>();
      for (String categoryId : jsonMap.keySet()) {
        this.categoryRatings.putIfAbsent(categoryId,
            new UserRatings((Map<String, Object>) jsonMap.get(categoryId)));
      }
    }
  }

  public void setOwnedCategories(final Map<String, Object> jsonMap) {
    this.ownedCategories = null;
    if (jsonMap != null) {
      this.ownedCategories = new HashMap<>();
      for (String categoryId : jsonMap.keySet()) {
        this.ownedCategories.putIfAbsent(categoryId, (String) jsonMap.get(categoryId));
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
        this.favorites
            .putIfAbsent(username, new Favorite((Map<String, Object>) jsonMap.get(username)));
      }
    }
  }

  public boolean pushEndpointArnIsSet() {
    return this.pushEndpointArn != null;
  }
}
