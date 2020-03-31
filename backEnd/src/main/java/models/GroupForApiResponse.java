package models;

import imports.DatabaseManagers;
import java.util.Map;
import lombok.Data;

@Data
public class GroupForApiResponse extends Group {
  private Integer totalNumberOfEvents;

  public static final String TOTAL_NUMBER_OF_EVENTS = "TotalNumberOfEvents";

  public GroupForApiResponse(final Group group, final Integer batchNumber) {
    super(group.asMap());
    this.totalNumberOfEvents = group.getEvents().size();
    this.setEvents(DatabaseManagers.GROUPS_MANAGER.getBatchOfEvents(this, batchNumber));
  }

  public GroupForApiResponse(final Group group) {
    super(group.asMap());
    this.totalNumberOfEvents = group.getEvents().size();
    this.setEvents(DatabaseManagers.GROUPS_MANAGER.getBatchOfEvents(this, 0));
  }

  @Override
  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = super.asMap();
    modelAsMap.putIfAbsent(TOTAL_NUMBER_OF_EVENTS, this.totalNumberOfEvents);
    return modelAsMap;
  }
}