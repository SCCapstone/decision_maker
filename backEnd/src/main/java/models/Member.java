package models;

import imports.UsersManager;
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
}
