package models;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public class GetGroupResponse implements Model {

  public static final String USER_INFO = "UserInfo";
  public static final String EVENTS_WITHOUT_RATINGS = "EventsWithoutRatings";
  public static final String GROUP_INFO = "GroupInfo";

  private final Map<String, Object> userInfo;
  private final Map<String, Boolean> eventsUnseen;
  private final Map<String, Boolean> eventsWithoutRatings;
  private GroupForApiResponse groupInfo;

  public GetGroupResponse() {
    this.eventsUnseen = new HashMap<>();
    this.eventsWithoutRatings = new HashMap<>();
    this.userInfo = ImmutableMap.of(
        EVENTS_WITHOUT_RATINGS, this.eventsWithoutRatings,
        User.EVENTS_UNSEEN, this.eventsUnseen
    );
  }

  public GetGroupResponse(final GroupForApiResponse groupForApiResponse) {
    this.eventsUnseen = new HashMap<>();
    this.eventsWithoutRatings = new HashMap<>();
    this.userInfo = ImmutableMap.of(
        EVENTS_WITHOUT_RATINGS, this.eventsWithoutRatings,
        User.EVENTS_UNSEEN, this.eventsUnseen
    );
    this.setGroupInfo(groupForApiResponse);
  }

  public GetGroupResponse(final User user,
      final GroupWithCategoryChoices groupWithCategoryChoices) {
    this.eventsUnseen = new HashMap<>();
    this.eventsWithoutRatings = new HashMap<>();
    this.userInfo = ImmutableMap.of(
        EVENTS_WITHOUT_RATINGS, this.eventsWithoutRatings,
        User.EVENTS_UNSEEN, this.eventsUnseen
    );
    this.setGroupInfo(new GroupForApiResponse(user, groupWithCategoryChoices));
    this.setUserData(user, groupWithCategoryChoices);
  }

  public void setUserData(final User user,
      final GroupWithCategoryChoices groupWithCategoryChoices) {
    //loop over the events that are being sent to the front end and mark the unseen/unrated
    for (final Map.Entry<String, Event> eventEntry : this.groupInfo.getAllEvents().entrySet()) {
      final String eventId = eventEntry.getKey();
      final EventWithCategoryChoices event = groupWithCategoryChoices.getEventsWithCategoryChoices()
          .get(eventId);

      if (user.getGroups().get(this.groupInfo.getGroupId()).getEventsUnseen()
          .containsKey(eventId)) {
        this.addEventUnseen(eventId);
      }

      //if the event no longer has choices then we don't care if the user has ratings
      if (event.getCategoryChoices() != null) {
        if (!user.getCategoryRatings().containsKey(event.getCategoryId())) {
          //first we check if the user has ratings for the category at all
          this.addEventWithoutRating(eventId);
        } else if (!user.getCategoryRatings().get(event.getCategoryId()).keySet()
            .containsAll(event.getCategoryChoices().keySet())) {
          //then we check if the user has choice ratings set for all of the event choices
          this.addEventWithoutRating(eventId);
        }
      }
    }
  }

  public void addEventUnseen(final String eventId) {
    this.eventsUnseen.put(eventId, true);
  }

  public void addEventWithoutRating(final String eventId) {
    this.eventsWithoutRatings.put(eventId, true);
  }

  public void setGroupInfo(final GroupForApiResponse groupForApiResponse) {
    this.groupInfo = groupForApiResponse;
  }

  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.putIfAbsent(USER_INFO, this.userInfo);
    modelAsMap.putIfAbsent(GROUP_INFO, this.groupInfo.asMap());
    return modelAsMap;
  }
}
