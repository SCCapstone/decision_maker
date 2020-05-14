package models;

import com.amazonaws.services.dynamodbv2.document.Item;
import exceptions.InvalidAttributeValueException;
import java.util.HashMap;
import java.util.Map;
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
  public static final String FIRST_LOGIN = "FirstLogin";

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
  private Map<String, Map<String, Integer>> categoryRatings;
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
        final Map<String, Object> choiceRatings = (Map<String, Object>) jsonMap.get(categoryId);
        final Map<String, Integer> choiceRatingsConverted = new HashMap<>();
        for (String choiceId : choiceRatings.keySet()) {
          choiceRatingsConverted
              .putIfAbsent(choiceId, getIntFromObject(choiceRatings.get(choiceId)));
        }
        this.categoryRatings.putIfAbsent(categoryId, choiceRatingsConverted);
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

  private Integer getIntFromObject(final Object input) {
    if (input != null) {
      return Integer.parseInt(input.toString());
    }
    return null;
  }

  public boolean pushEndpointArnIsSet() {
    return this.pushEndpointArn != null;
  }
}
