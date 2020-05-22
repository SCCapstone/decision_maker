package handlers;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.Event;
import models.EventWithCategoryChoices;
import models.Group;
import models.GroupForApiResponse;
import models.GroupWithCategoryChoices;
import models.Model;
import models.User;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class GetGroupHandler implements ApiRequestHandler {

  private final DbAccessManager dbAccessManager;
  private final Metrics metrics;

  @Inject
  public GetGroupHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method gets and returns a group item. It should be noted that returned groups only get a
   * limited number of events which is why the batch number is needed. This tells which events we
   * want information on.
   *
   * @param activeUser  This is the username of the user making the api request.
   * @param groupId     This is the id of the group being gotten.
   * @param batchNumber This is the event batch that we're limiting the returned data to.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String groupId,
      final Integer batchNumber) {
    final String classMethod = "GetGroupHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final GroupWithCategoryChoices group = new GroupWithCategoryChoices(
          this.dbAccessManager.getGroupItem(groupId).asMap());

      //the user should not be able to retrieve info from the group if they are not a member
      if (group.getMembers().containsKey(activeUser)) {
        final User user = this.dbAccessManager.getUser(activeUser);

        final GroupForApiResponse groupForApiResponse = new GroupForApiResponse(group,
            batchNumber);

        final GetGroupResponse getGroupResponse = new GetGroupResponse(groupForApiResponse);

        for (final Map.Entry<String, EventWithCategoryChoices> eventEntry : group
            .getEventsWithCategoryChoices().entrySet()) {
          final String eventId = eventEntry.getKey();
          final EventWithCategoryChoices event = eventEntry.getValue();

          if (user.getGroups().get(groupId).getEventsUnseen().containsKey(eventId)) {
            getGroupResponse.addEventUnseen(eventId);
          }

          //if the event no longer has choices then we don't care if the user has ratings
          if (event.getCategoryChoices() != null) {
            if (!user.getCategoryRatings().containsKey(event.getCategoryId())) {
              //first we check if the user has ratings for the category at all
              getGroupResponse.addEventWithoutRating(eventId);
            } else if (!user.getCategoryRatings().get(event.getCategoryId()).keySet()
                .containsAll(event.getCategoryChoices().keySet())) {
              //then we check if the user has choice ratings set for all of the event choices
              getGroupResponse.addEventWithoutRating(eventId);
            }
          }
        }

        resultStatus = ResultStatus.successful(JsonUtils.convertObjectToJson(getGroupResponse));
      } else {
        resultStatus = ResultStatus.failure("Error: user is not a member of the group.");
        this.metrics.logWithBody(new WarningDescriptor<>(classMethod, "User not in group"));
      }
    } catch (final Exception e) {
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  //static class
  private static class GetGroupResponse implements Model {

    public static final String USER_INFO = "UserInfo";
    public static final String EVENTS_WITHOUT_RATINGS = "EventsWithoutRatings";
    public static final String GROUP_INFO = "GroupInfo";

    private final Map<String, Object> userInfo;
    private final Map<String, Boolean> eventsUnseen;
    private final Map<String, Boolean> eventsWithoutRatings;
    private Map<String, Object> groupInfo;

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

    public void addEventUnseen(final String eventId) {
      this.eventsUnseen.put(eventId, true);
    }

    public void addEventWithoutRating(final String eventId) {
      this.eventsWithoutRatings.put(eventId, true);
    }

    public void setGroupInfo(final GroupForApiResponse groupForApiResponse) {
      this.groupInfo = groupForApiResponse.asMap();
    }

    public Map<String, Object> asMap() {
      final Map<String, Object> modelAsMap = new HashMap<>();
      modelAsMap.putIfAbsent(USER_INFO, this.userInfo);
      modelAsMap.putIfAbsent(GROUP_INFO, this.groupInfo);
      return modelAsMap;
    }
  }
}
