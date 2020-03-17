package models;

import imports.UsersManager;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AppSettings {

  //TODO at some point update the ints to bools
  private Integer darkTheme;
  private Integer groupSort;
  private Integer categorySort;
  private Integer muted;

  public static AppSettings defaultSettings() {
    return AppSettings.builder()
        .darkTheme(UsersManager.DEFAULT_DARK_THEME)
        .groupSort(UsersManager.DEFAULT_GROUP_SORT)
        .categorySort(UsersManager.DEFAULT_CATEGORY_SORT)
        .muted(UsersManager.DEFAULT_MUTED)
        .build();
  }

  public AppSettings(final Map<String, Object> jsonMap) {
    if (jsonMap != null) {
      this.setDarkTheme(this.getIntFromObject(jsonMap.get(UsersManager.APP_SETTINGS_DARK_THEME)));
      this.setGroupSort(this.getIntFromObject(jsonMap.get(UsersManager.APP_SETTINGS_GROUP_SORT)));
      this.setCategorySort(
          this.getIntFromObject(jsonMap.get(UsersManager.APP_SETTINGS_CATEGORY_SORT)));
      this.setMuted(this.getIntFromObject(jsonMap.get(UsersManager.APP_SETTINGS_MUTED)));
    }
  }

  private Integer getIntFromObject(final Object input) {
    if (input != null) {
      return Integer.parseInt(input.toString());
    }
    return null;
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
