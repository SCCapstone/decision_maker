package models;

import com.google.common.collect.ImmutableList;
import exceptions.InvalidAttributeValueException;
import imports.UsersManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

@Data
@Builder
@AllArgsConstructor
public class AppSettings {

  //TODO at some point update the ints to bools
  private boolean darkTheme;
  private boolean muted;

  @Setter(AccessLevel.NONE)
  private Integer groupSort;
  @Setter(AccessLevel.NONE)
  private Integer categorySort;

  public static AppSettings defaultSettings() throws InvalidAttributeValueException {
    return AppSettings.builder()
        .darkTheme(UsersManager.DEFAULT_DARK_THEME)
        .groupSort(UsersManager.DEFAULT_GROUP_SORT)
        .categorySort(UsersManager.DEFAULT_CATEGORY_SORT)
        .muted(UsersManager.DEFAULT_MUTED)
        .build();
  }

  public AppSettings(final Map<String, Object> jsonMap) throws InvalidAttributeValueException {
    if (jsonMap != null) {
      this.setDarkTheme(this.getBoolFromObject(jsonMap.get(UsersManager.APP_SETTINGS_DARK_THEME)));
      this.setGroupSort(this.getIntFromObject(jsonMap.get(UsersManager.APP_SETTINGS_GROUP_SORT)));
      this.setCategorySort(
          this.getIntFromObject(jsonMap.get(UsersManager.APP_SETTINGS_CATEGORY_SORT)));
      this.setMuted(this.getBoolFromObject(jsonMap.get(UsersManager.APP_SETTINGS_MUTED)));
    }
  }

  private void setGroupSort(final Integer groupSort) throws InvalidAttributeValueException {
    // these values decode to have meaning in globals.dart on the front end
    final List<Integer> validGroupSortValues = ImmutableList.of(0, 1, 2, 3);
    if (validGroupSortValues.contains(groupSort)) {
      this.groupSort = groupSort;
    } else {
      throw new InvalidAttributeValueException(UsersManager.APP_SETTINGS_GROUP_SORT,
          validGroupSortValues, groupSort);
    }
  }

  private void setCategorySort(final Integer categorySort) throws InvalidAttributeValueException {
    // these values decode to have meaning in globals.dart on the front end
    final List<Integer> validCategorySortValues = ImmutableList.of(1, 2);
    if (validCategorySortValues.contains(categorySort)) {
      this.categorySort = categorySort;
    } else {
      throw new InvalidAttributeValueException(UsersManager.APP_SETTINGS_CATEGORY_SORT,
          validCategorySortValues, categorySort);
    }
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

  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.putIfAbsent(UsersManager.APP_SETTINGS_DARK_THEME, this.darkTheme);
    modelAsMap.putIfAbsent(UsersManager.APP_SETTINGS_GROUP_SORT, this.groupSort);
    modelAsMap.putIfAbsent(UsersManager.APP_SETTINGS_CATEGORY_SORT, this.categorySort);
    modelAsMap.putIfAbsent(UsersManager.APP_SETTINGS_MUTED, this.muted);
    return modelAsMap;
  }
}
