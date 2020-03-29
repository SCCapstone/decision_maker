package models;

import imports.CategoriesManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class EventWithCategoryChoices extends Event {

  private Map<String, String> categoryChoices;

  public EventWithCategoryChoices(final Map<String, Object> jsonMap) {
    super(jsonMap);
    this.setCategoryChoicesRawMap((Map<String, Object>) jsonMap.get(CategoriesManager.CHOICES));
  }

  public void setCategoryChoicesRawMap(final Map<String, Object> jsonMap) {
    this.categoryChoices = null;
    if (jsonMap != null) {
      this.categoryChoices = jsonMap.entrySet().stream().collect(Collectors
          .toMap(Entry::getKey, (e) -> (String) e.getValue(), (e1, e2) -> e2, HashMap::new));
    }
  }

  @Override
  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = super.asMap();
    modelAsMap.putIfAbsent(CategoriesManager.CHOICES, this.categoryChoices);
    return modelAsMap;
  }

  @Override
  public void setCategoryFields(final Category category) {
    super.setCategoryFields(category);
    this.setCategoryChoices(category.getChoices());
  }
}
