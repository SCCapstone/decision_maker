package imports;

import com.amazonaws.regions.Regions;

public class EventsManager extends DatabaseAccessManager {

  public EventsManager() {
    super("events", "EventId", Regions.US_EAST_2);
  }
}
