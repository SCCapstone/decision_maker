package models;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Metadata {

  private String action;
  private Map<String, Object> payload;

  public Map<String, Object> asMap() {
    final Map<String, Object> objectAsMap = new HashMap<>();
    objectAsMap.put("action", this.action);
    objectAsMap.put("payload", this.payload);
    return objectAsMap;
  }

  public void addToPayload(final String key, final Object value) {
    if (this.payload != null) {
      this.payload.putIfAbsent(key, value);
    }
  }
}
