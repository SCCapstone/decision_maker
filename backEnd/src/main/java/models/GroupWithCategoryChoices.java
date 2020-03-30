package models;

import imports.GroupsManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
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
      this.eventsWithCategoryChoices = jsonMap.entrySet().stream().collect(Collectors
          .toMap(Entry::getKey,
              (e) -> new EventWithCategoryChoices((Map<String, Object>) e.getValue()),
              (e1, e2) -> e2, HashMap::new));
    }
  }
}
