package handlers;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import managers.DbAccessManager;
import models.Event;
import models.EventForSorting;
import models.GetGroupResponse;
import models.Group;
import models.GroupForApiResponse;
import models.GroupWithCategoryChoices;
import models.User;
import models.UserGroup;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class GetBatchOfEventsHandler implements ApiRequestHandler {

  private static final Integer EVENTS_TYPE_NEW = 0;
  private static final Integer EVENTS_TYPE_CLOSED = 1;
  private static final Integer EVENTS_TYPE_CONSIDER = 2;
  private static final Integer EVENTS_TYPE_VOTING = 3;
  private static final Integer EVENTS_TYPE_OCCURRING = 4;

  private static final Integer EVENTS_BATCH_SIZE = 5;

  private final DbAccessManager dbAccessManager;
  private final Metrics metrics;

  public GetBatchOfEventsHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This function takes in a group and batch number and gets the appropriate map of events for the
   * group based on the batch number. This method is used in the 'infinitely' scrolling list of
   * events on a group's page. Using this, we continue to get the next set or 'batch' of events.
   *
   * @param activeUser  The username of the user making this api request.
   * @param groupId     The id of the group to get events from.
   * @param batchNumber The batch index of events to get from the group.
   * @param batchType   The type of events that this batch should return. The value decodes on the
   *                    front end as well as the back end with the same values.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String groupId,
      final Integer batchNumber, final Integer batchType) {
    final String classMethod = "GetBatchOfEventsHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final GroupWithCategoryChoices groupWithCategoryChoices = new GroupWithCategoryChoices(
          this.dbAccessManager.getGroupItem(groupId).asMap());

      final GroupForApiResponse groupForApiResponse = new GroupForApiResponse(
          groupWithCategoryChoices);

      final User user = this.dbAccessManager.getUser(activeUser);

      //the user should not be able to retrieve info from the group if they are not a member
      if (groupForApiResponse.getMembers().containsKey(activeUser) && user.getGroups()
          .containsKey(groupId)) {
        //we set the events on the group so we can use the group's getEventsMap method
        final Map<String, Event> eventsBatch = GetBatchOfEventsHandler
            .handle(user, groupForApiResponse, batchNumber, batchType);

        final Map<String, Map<String, Event>> eventBatches = new HashMap<>();
        eventBatches.putIfAbsent(getEventPriorityLabelFromBatchType(batchType), eventsBatch);
        groupForApiResponse.setAllEvents(eventBatches);

        final GetGroupResponse getGroupResponse = new GetGroupResponse(groupForApiResponse);
        getGroupResponse.setUserData(user, groupWithCategoryChoices);

        resultStatus = ResultStatus.successful(JsonUtils.convertObjectToJson(getGroupResponse));
      } else {
        this.metrics.logWithBody(new WarningDescriptor<>(classMethod, "User not in group."));
        resultStatus = ResultStatus.failure("Error: user not a member of the group.");
      }
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This method is imperative for not overloading the front end with data. Since a group can have
   * an unlimited number of events, we need to limit how many we return at any one time. This is why
   * we're batching events. This function sorts all of the events from the first one to the oldest
   * in the batch, then it flips the sort and gets the top 'n' where 'n' is the number in the
   * batch.
   *
   * @param activeUser  The user object of the user making the api request.
   * @param group       The group we are trying to get a batch of events for.
   * @param batchNumber The index of events to get. Index i gets events (i, (i + 1) * batch size]
   * @param batchType   The type of events to get in the batch. Types map to different priorities.
   * @return A mapping of eventIds to event objects contained in the requested batch.
   */
  public static Map<String, Event> handle(final User activeUser, final Group group,
      final Integer batchNumber, final Integer batchType) {
    Integer newestEventIndex = (batchNumber * EVENTS_BATCH_SIZE);
    Integer oldestEventIndex = (batchNumber + 1) * EVENTS_BATCH_SIZE;

    ArrayList<String> batchEventIds = new ArrayList<>(group.getEvents().keySet());
    Map<String, Event> eventsBatch = new LinkedHashMap<>();

    //get all of the events from the first event to the oldest event in the batch
    if (group.getEvents().size() > newestEventIndex) {
      //we adjust this so that the .limit(oldestEvent - newestEvent) gets the correct number of items
      if (group.getEvents().size() < oldestEventIndex) {
        oldestEventIndex = group.getEvents().size();
      }

      final LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));

      //first of all we convert the map to be events for sorting before we sort
      //this way we don't have to do all of the sorting setup for every comparison in the sort
      //if setting up sorting params takes n time and sorting takes m times, then doing
      //this first leads to n + m complexity vs n * m complexity if we calculated every comparison
      Map<String, EventForSorting> searchingEventsBatch = group.getEvents()
          .entrySet()
          .stream()
          .collect(collectingAndThen(
              toMap(Entry::getKey, (Map.Entry e) -> new EventForSorting((Event) e.getValue(), now)),
              LinkedHashMap::new));

      if (batchType.equals(EVENTS_TYPE_NEW)) {
        final Set<String> unseenEventIds = activeUser.getGroups().get(group.getGroupId())
            .getEventsUnseen().keySet();

        batchEventIds.removeIf(eventId -> !unseenEventIds.contains(eventId));
      } else {
        final Integer eventsTypePriority = getEventPriorityFromBatchType(batchType);

        batchEventIds.removeIf(
            eventId -> !searchingEventsBatch.get(eventId).getPriority().equals(eventsTypePriority));
      }

      //sort the events
      batchEventIds.sort((String e1, String e2) -> searchingEventsBatch.get(e1)
          .compareTo(searchingEventsBatch.get(e2)));

      String eventId;
      for (int i = newestEventIndex; i < oldestEventIndex; i++) {
        eventId = batchEventIds.get(i);
        eventsBatch.put(eventId, group.getEvents().get(eventId));
      }
    } // else there are no events in this range and we return the empty map

    return eventsBatch;
  }

  /**
   * This method is imperative for not overloading the front end with data. Since a group can have
   * an unlimited number of events, we need to limit how many we return at any one time. This is why
   * we're batching events. This function sorts all of the events from the first one to the oldest
   * in the batch, then it flips the sort and gets the top 'n' where 'n' is the number in the batch.
   * This is done for all types.
   *
   * @param activeUser The user object of the user making the api request.
   * @param group      The group we are trying to get a batch of events for.
   * @return A mapping of eventIds to event objects contained in the requested batch.
   */
  public static Map<String, Map<String, Event>> handle(final User activeUser, final Group group) {

    final Map<String, Map<String, Event>> eventTypesToEvent = new HashMap<>();
    eventTypesToEvent.put(GroupForApiResponse.OCCURRING_EVENTS, new HashMap<>());
    eventTypesToEvent.put(GroupForApiResponse.NEW_EVENTS, new HashMap<>());
    eventTypesToEvent.put(GroupForApiResponse.CONSIDER_EVENTS, new HashMap<>());
    eventTypesToEvent.put(GroupForApiResponse.VOTING_EVENTS, new HashMap<>());
    eventTypesToEvent.put(GroupForApiResponse.CLOSED_EVENTS, new HashMap<>());

    //linked hash maps maintain order whereas normal hash maps do not
    LinkedHashMap<String, EventForSorting> eventsBatch = new LinkedHashMap<>();

    final Set<String> unseenEventIds = activeUser.getGroups()
        .getOrDefault(group.getGroupId(), UserGroup.fromNewGroup(group)).getEventsUnseen().keySet();

    //get all of the events from the first event to the oldest event in the batch
    if (group.getEvents().size() > 0) {
      final LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));

      //first of all we convert the map to be events for sorting before we sort
      //this way we don't have to do all of the sorting setup for every comparison in the sort
      //if setting up sorting params takes n time and sorting takes m times, then doing
      //this first leads to n + m complexity vs n * m complexity if we calculated every comparison
      eventsBatch = group.getEvents()
          .entrySet()
          .stream()
          .collect(collectingAndThen(
              toMap(Entry::getKey, (Map.Entry e) -> new EventForSorting((Event) e.getValue(), now)),
              LinkedHashMap::new));
    } // else there are no events in this range and we return the empty map

    //then we sort those events oldest to newest
    eventsBatch.entrySet().stream()
        .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
        .forEach((eventEntry) -> {
          if (unseenEventIds.contains(eventEntry.getKey()) &&
              eventTypesToEvent.get(GroupForApiResponse.NEW_EVENTS).size() < EVENTS_BATCH_SIZE) {
            eventTypesToEvent.get(GroupForApiResponse.NEW_EVENTS)
                .put(eventEntry.getKey(), eventEntry.getValue());
          }

          String priorityLabel = getEventPriorityLabelFromPriority(
              eventEntry.getValue().getPriority());

          if (eventTypesToEvent.get(priorityLabel).size() < EVENTS_BATCH_SIZE) {
            eventTypesToEvent.get(priorityLabel).put(eventEntry.getKey(), eventEntry.getValue());
          }
        });

    return eventTypesToEvent;
  }

  private static Integer getEventPriorityFromBatchType(final Integer batchType) {
    Integer priority = -1;
    if (batchType.equals(EVENTS_TYPE_CLOSED)) {
      priority = EventForSorting.PRIORITY_CLOSED;
    } else if (batchType.equals(EVENTS_TYPE_CONSIDER)) {
      priority = EventForSorting.PRIORITY_CONSIDERING;
    } else if (batchType.equals(EVENTS_TYPE_OCCURRING)) {
      priority = EventForSorting.PRIORITY_OCCURRING;
    } else if (batchType.equals(EVENTS_TYPE_VOTING)) {
      priority = EventForSorting.PRIORITY_VOTING;
    }

    return priority;
  }

  private static String getEventPriorityLabelFromBatchType(final Integer batchType) {
    String label = null;
    if (batchType.equals(EVENTS_TYPE_CLOSED)) {
      label = GroupForApiResponse.CLOSED_EVENTS;
    } else if (batchType.equals(EVENTS_TYPE_CONSIDER)) {
      label = GroupForApiResponse.CONSIDER_EVENTS;
    } else if (batchType.equals(EVENTS_TYPE_OCCURRING)) {
      label = GroupForApiResponse.OCCURRING_EVENTS;
    } else if (batchType.equals(EVENTS_TYPE_VOTING)) {
      label = GroupForApiResponse.VOTING_EVENTS;
    }

    return label;
  }

  private static String getEventPriorityLabelFromPriority(final Integer priority) {
    String label = null;
    if (priority.equals(EventForSorting.PRIORITY_CLOSED)) {
      label = GroupForApiResponse.CLOSED_EVENTS;
    } else if (priority.equals(EventForSorting.PRIORITY_CONSIDERING)) {
      label = GroupForApiResponse.CONSIDER_EVENTS;
    } else if (priority.equals(EventForSorting.PRIORITY_OCCURRING)) {
      label = GroupForApiResponse.OCCURRING_EVENTS;
    } else if (priority.equals(EventForSorting.PRIORITY_VOTING)) {
      label = GroupForApiResponse.VOTING_EVENTS;
    }

    return label;
  }
}

