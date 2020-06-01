package models;

import handlers.GetBatchOfEventsHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Data;

@Data
public class GroupForApiResponse extends Group implements Model {
  public static final String TOTAL_NUMBER_OF_EVENTS = "TotalNumberOfEvents";
  public static final String NEW_EVENTS = "NewEvents";
  public static final String VOTING_EVENTS = "VotingEvents";
  public static final String CONSIDER_EVENTS = "ConsiderEvents";
  public static final String CLOSED_EVENTS = "ClosedEvents";
  public static final String OCCURRING_EVENTS = "OccurringEvents";

  private Integer totalNumberOfEvents;
  private Map<String, Event> newEvents;
  private Map<String, Event> votingEvents;
  private Map<String, Event> considerEvents;
  private Map<String, Event> closedEvents;
  private Map<String, Event> occurringEvents;

  //This is used to keep info on all the events that are on this group when creating other group
  //objects. It should never go into the asMap output as that would defeat the purpose.
  private Map<String, Event> allEvents;

  public GroupForApiResponse(final User user, final Group group) {
    super(group.asMap());
    this.totalNumberOfEvents = group.getEvents().size();
    this.setAllEvents(GetBatchOfEventsHandler.handle(user, this));
  }

  @Override
  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = super.asMap();
    modelAsMap.putIfAbsent(TOTAL_NUMBER_OF_EVENTS, this.totalNumberOfEvents);
    modelAsMap.putIfAbsent(NEW_EVENTS, this.eventsMapAsObjectMap(this.newEvents));
    modelAsMap.putIfAbsent(VOTING_EVENTS, this.eventsMapAsObjectMap(this.votingEvents));
    modelAsMap.putIfAbsent(CONSIDER_EVENTS, this.eventsMapAsObjectMap(this.considerEvents));
    modelAsMap.putIfAbsent(CLOSED_EVENTS, this.eventsMapAsObjectMap(this.closedEvents));
    modelAsMap.putIfAbsent(OCCURRING_EVENTS, this.eventsMapAsObjectMap(this.occurringEvents));
    return modelAsMap;
  }

  private Map<String, Object> eventsMapAsObjectMap(final Map<String, Event> eventMap) {
    final Map<String, Object> modelAsMap = new HashMap<>();
    for (final Entry<String, Event> eventEntry : eventMap.entrySet()) {
      modelAsMap.putIfAbsent(eventEntry.getKey(), eventEntry.getValue());
    }
    return modelAsMap;
  }

  private void setAllEvents(final Map<String, Map<String, Event>> eventBatches) {
    this.newEvents = eventBatches.get(NEW_EVENTS);
    this.votingEvents = eventBatches.get(VOTING_EVENTS);
    this.considerEvents = eventBatches.get(CONSIDER_EVENTS);
    this.closedEvents = eventBatches.get(CLOSED_EVENTS);
    this.occurringEvents = eventBatches.get(OCCURRING_EVENTS);

    this.allEvents = new HashMap<>();
    this.allEvents.putAll(this.newEvents);
    this.allEvents.putAll(this.votingEvents);
    this.allEvents.putAll(this.considerEvents);
    this.allEvents.putAll(this.closedEvents);
    this.allEvents.putAll(this.occurringEvents);
  }
}
