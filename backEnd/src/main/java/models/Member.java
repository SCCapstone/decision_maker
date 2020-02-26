package models;

import imports.UsersManager;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Member {
  private String displayName;
  private String icon;

  public Member(final Map<String, Object> jsonMap) {
    this.setDisplayName((String) jsonMap.get(UsersManager.DISPLAY_NAME));
    this.setIcon((String) jsonMap.get(UsersManager.ICON));
  }
}
