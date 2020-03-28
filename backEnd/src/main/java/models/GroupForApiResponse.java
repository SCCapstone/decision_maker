package models;

import java.util.Map;
import lombok.Data;

@Data
public class GroupForApiResponse extends Group {
  private Integer totalNumberOfEvents;

  public static final String TOTAL_NUMBER_OF_EVENTS = "TotalNumberOfEvents";

  public GroupForApiResponse(final Group group) {
    super(group.asMap());
    this.totalNumberOfEvents = group.getEvents().size();
  }

  @Override
  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = super.asMap();
    modelAsMap.putIfAbsent(TOTAL_NUMBER_OF_EVENTS, this.totalNumberOfEvents);
    return modelAsMap;
  }
}
