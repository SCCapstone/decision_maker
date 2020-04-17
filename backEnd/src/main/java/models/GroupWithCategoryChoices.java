package models;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import imports.GroupsManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Data;

@Data
public class GroupWithCategoryChoices extends Group {

  private Map<String, EventWithCategoryChoices> eventsWithCategoryChoices;

  public GroupWithCategoryChoices(final Map<String, Object> jsonMap) {
    super(jsonMap);
    this.setEventsWithCategoryChoicesRawMap(
        (Map<String, Object>) jsonMap.get(GroupsManager.EVENTS));
  }

  private void setEventsWithCategoryChoicesRawMap(final Map<String, Object> jsonMap) {
    this.eventsWithCategoryChoices = null;
    if (jsonMap != null) {
      this.eventsWithCategoryChoices = jsonMap.entrySet().stream()
          .collect(collectingAndThen(toMap(Entry::getKey,
              (Map.Entry e) -> new EventWithCategoryChoices((Map<String, Object>) e.getValue())),
              HashMap::new));
    }
  }
}
