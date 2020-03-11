package models;

import imports.UsersManager;
import java.math.BigDecimal;
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
  private Integer muted;

  public static AppSettings defaultSettings() {
    return AppSettings.builder()
        .darkTheme(UsersManager.DEFAULT_DARK_THEME)
        .groupSort(UsersManager.DEFAULT_GROUP_SORT)
        .muted(UsersManager.DEFAULT_MUTED)
        .build();
  }

  public AppSettings(final Map<String, Object> jsonMap) {
    this.setDarkTheme(
        this.getIntFromBigDec((BigDecimal) jsonMap.get(UsersManager.APP_SETTINGS_DARK_THEME)));
    this.setGroupSort(
        this.getIntFromBigDec((BigDecimal) jsonMap.get(UsersManager.APP_SETTINGS_GROUP_SORT)));
    this.setMuted(this.getIntFromBigDec((BigDecimal) jsonMap.get(UsersManager.APP_SETTINGS_MUTED)));
  }

  private Integer getIntFromBigDec(final BigDecimal input) {
    if (input != null) {
      return input.intValue();
    }
    return null;
  }

  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.putIfAbsent(UsersManager.APP_SETTINGS_DARK_THEME, this.darkTheme);
    modelAsMap.putIfAbsent(UsersManager.APP_SETTINGS_GROUP_SORT, this.groupSort);
    modelAsMap.putIfAbsent(UsersManager.APP_SETTINGS_MUTED, this.muted);
    return modelAsMap;
  }
}
