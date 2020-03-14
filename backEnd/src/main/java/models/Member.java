package models;

import imports.UsersManager;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Member {
  private String displayName;
  private String icon;

  public Member(final Map<String, Object> jsonMap) {
    this.setDisplayName((String) jsonMap.get(UsersManager.DISPLAY_NAME));
    this.setIcon((String) jsonMap.get(UsersManager.ICON));
  }

  public Map<String, String> asMap() {
    Map<String, String> modelAsMap = new HashMap<>();
    modelAsMap.putIfAbsent(UsersManager.DISPLAY_NAME, this.displayName);
    modelAsMap.putIfAbsent(UsersManager.ICON, this.icon);
    return modelAsMap;
  }
}
