package models;

import handlers.UsersManager;
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
    this.setDisplayName((String) jsonMap.get(UsersManager.DISPLAY_NAME));
    this.setIcon((String) jsonMap.get(UsersManager.ICON));
  }
}
