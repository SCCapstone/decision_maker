package models;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {

  private String displayName;
  private String icon;

  public Favorite(final Map<String, Object> jsonMap) {
    this.setDisplayName((String) jsonMap.get(User.DISPLAY_NAME));
    this.setIcon((String) jsonMap.get(User.ICON));
  }

  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.putIfAbsent(User.DISPLAY_NAME, this.displayName);
    modelAsMap.putIfAbsent(User.ICON, this.icon);
    return modelAsMap;
  }
}
