package models;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GroupCategory {

  private String categoryName;
  private String owner;

  public GroupCategory(final Map<String, Object> jsonMap) {
    this.categoryName = (String) jsonMap.get(Category.CATEGORY_NAME);
    this.owner = (String) jsonMap.get(Category.OWNER);
  }

  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.putIfAbsent(Category.CATEGORY_NAME, this.categoryName);
    modelAsMap.putIfAbsent(Category.OWNER, this.owner);
    return modelAsMap;
  }
}
