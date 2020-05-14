package models;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EventWithCategoryChoices extends Event {

  private Map<String, String> categoryChoices;

  public EventWithCategoryChoices(final Map<String, Object> jsonMap) {
    super(jsonMap);
    this.setCategoryChoicesRawMap((Map<String, Object>) jsonMap.get(Category.CHOICES));
  }

  public void setCategoryChoicesRawMap(final Map<String, Object> jsonMap) {
    this.categoryChoices = null;
    if (jsonMap != null) {
      this.categoryChoices = jsonMap.entrySet().stream().collect(collectingAndThen(
          toMap(Entry::getKey, (Map.Entry e) -> (String) e.getValue()), HashMap::new));
    }
  }

  @Override
  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = super.asMap();
    modelAsMap.putIfAbsent(Category.CHOICES, this.categoryChoices);
    return modelAsMap;
  }

  @Override
  public void setCategoryFields(final Category category) {
    super.setCategoryFields(category);
    this.setCategoryChoices(category.getChoices());
  }

  public Map<String, Object> asEventMap() {
    return super.asMap();
  }
}
