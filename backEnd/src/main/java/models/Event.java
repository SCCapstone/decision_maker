package models;

import java.util.Map;
import lombok.Data;

@Data
public class Event {
  private String categoryId;
  private String categoryName;
  private String eventName;
  private String createdDateTime;
  private String eventStartDateTime;
  private Integer rsvpDuration;
  private Integer votingDuration;
  private Map<String>

  public Event(final Map<String, Object> jsonMap) {

  }
}
