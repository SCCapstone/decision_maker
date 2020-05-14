package models;

import com.google.common.collect.ImmutableList;
import exceptions.InvalidAttributeValueException;
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

  public static final String DARK_THEME = "DarkTheme";
  public static final String MUTED = "Muted";
  public static final String GROUP_SORT = "GroupSort";
  public static final String CATEGORY_SORT = "CategorySort";

  private static final boolean DEFAULT_DARK_THEME = true;
  private static final boolean DEFAULT_MUTED = true;
  private static final Integer DEFAULT_GROUP_SORT = 1;
  private static final Integer DEFAULT_CATEGORY_SORT = 1;

  private boolean darkTheme;
  private boolean muted;

  @Setter(AccessLevel.NONE)
  private Integer groupSort;
  @Setter(AccessLevel.NONE)
  private Integer categorySort;

  public static AppSettings defaultSettings() throws InvalidAttributeValueException {
    return AppSettings.builder()
        .darkTheme(DEFAULT_DARK_THEME)
        .groupSort(DEFAULT_GROUP_SORT)
        .categorySort(DEFAULT_CATEGORY_SORT)
        .muted(DEFAULT_MUTED)
        .build();
  }

  public AppSettings(final Map<String, Object> jsonMap) throws InvalidAttributeValueException {
    if (jsonMap != null) {
      this.setDarkTheme(this.getBoolFromObject(jsonMap.get(DARK_THEME)));
      this.setGroupSort(this.getIntFromObject(jsonMap.get(GROUP_SORT)));
      this.setCategorySort(
          this.getIntFromObject(jsonMap.get(CATEGORY_SORT)));
      this.setMuted(this.getBoolFromObject(jsonMap.get(MUTED)));
    }
  }

  public void setGroupSort(final Integer groupSort) throws InvalidAttributeValueException {
    // these values decode to have meaning in globals.dart on the front end
    final List<Integer> validGroupSortValues = ImmutableList.of(0, 1, 2, 3);
    if (validGroupSortValues.contains(groupSort)) {
      this.groupSort = groupSort;
    } else {
      throw new InvalidAttributeValueException(GROUP_SORT, validGroupSortValues, groupSort);
    }
  }

  public void setCategorySort(final Integer categorySort) throws InvalidAttributeValueException {
    // these values decode to have meaning in globals.dart on the front end
    final List<Integer> validCategorySortValues = ImmutableList.of(1, 2);
    if (validCategorySortValues.contains(categorySort)) {
      this.categorySort = categorySort;
    } else {
      throw new InvalidAttributeValueException(CATEGORY_SORT, validCategorySortValues,
          categorySort);
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
    modelAsMap.putIfAbsent(DARK_THEME, this.darkTheme);
    modelAsMap.putIfAbsent(GROUP_SORT, this.groupSort);
    modelAsMap.putIfAbsent(CATEGORY_SORT, this.categorySort);
    modelAsMap.putIfAbsent(MUTED, this.muted);
    return modelAsMap;
  }
}
