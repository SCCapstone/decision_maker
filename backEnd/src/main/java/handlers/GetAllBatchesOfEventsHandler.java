package handlers;

import static java.lang.StrictMath.max;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.Event;
import models.EventForSorting;
import models.GetGroupResponse;
import models.GroupForApiResponse;
import models.GroupWithCategoryChoices;
import models.User;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class GetAllBatchesOfEventsHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;
  private Metrics metrics;

  @Inject
  public GetAllBatchesOfEventsHandler(final DbAccessManager dbAccessManager,
      final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This function handles getting all of the batches present in all of the events lists possible.
   *
   * @param activeUser   The active user making the request.
   * @param groupId      The id of the group that all of the events should be retrieved from.
   * @param batchIndexes The max batch ids loaded in all of the events lists.
   * @param maxBatches   The max batches allowed to be loaded. This with the batchIndexes gives
   *                     information to know all of tha batches that need information retrieved.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String groupId,
      final Map<String, Integer> batchIndexes, final Integer maxBatches) {
    final String classMethod = "GetAllBatchesOfEventsHandler.handle";
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
        final Map<String, Map<String, Event>> allEventsBatches = this
            .getAllEventBatches(user, groupWithCategoryChoices, batchIndexes, maxBatches);

        groupForApiResponse.setAllEvents(allEventsBatches);

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

  private Map<String, Map<String, Event>> getAllEventBatches(final User user,
      final GroupWithCategoryChoices group, final Map<String, Integer> batchIndexes,
      final Integer maxBatches) {
    final Map<String, Map<String, Event>> eventTypesToEvents = new HashMap<>();
    eventTypesToEvents.put(GroupForApiResponse.OCCURRING_EVENTS, new HashMap<>());
    eventTypesToEvents.put(GroupForApiResponse.NEW_EVENTS, new HashMap<>());
    eventTypesToEvents.put(GroupForApiResponse.CONSIDER_EVENTS, new HashMap<>());
    eventTypesToEvents.put(GroupForApiResponse.VOTING_EVENTS, new HashMap<>());
    eventTypesToEvents.put(GroupForApiResponse.CLOSED_EVENTS, new HashMap<>());

    //set this up for later use
    final Set<String> unseenEventIds = user.getGroups().get(group.getGroupId()).getEventsUnseen()
        .keySet();

    //first of all we convert the map to be events for sorting before we sort
    //this way we don't have to do all of the sorting setup for every comparison in the sort
    final Map<String, EventForSorting> searchingEventsBatch = group.getEvents()
        .entrySet()
        .stream()
        .collect(toMap(Entry::getKey,
            (Map.Entry<String, Event> e) -> new EventForSorting(e.getKey(), e.getValue(),
                this.dbAccessManager.nowObj())));

    //separate the events into their appropriate buckets
    String priorityLabel;
    for (final Map.Entry<String, EventForSorting> eventEntry : searchingEventsBatch.entrySet()) {
      //add unseen events to the new events key
      if (unseenEventIds.contains(eventEntry.getKey())) {
        eventTypesToEvents.get(GroupForApiResponse.NEW_EVENTS)
            .put(eventEntry.getKey(), eventEntry.getValue());
      }

      //add the event to the correct bucket based on priority
      priorityLabel = GetBatchOfEventsHandler.getEventPriorityLabelFromPriority(
          eventEntry.getValue().getPriority());
      eventTypesToEvents.get(priorityLabel).put(eventEntry.getKey(), eventEntry.getValue());
    }

    //then sort each one and reduce down to the appropriate batch indexes
    Integer largestBatch, smallestBatch;
    ArrayList<String> batchEventIdsToRemove, batchEventIdsToKeep;
    String eventId, eventsType;
    Map<String, Event> eventsMap;
    int newestEventIndex, oldestEventIndex;
    for (Map.Entry<String, Map<String, Event>> eventMapEntry : eventTypesToEvents.entrySet()) {
      eventsType = eventMapEntry.getKey();
      eventsMap = eventMapEntry.getValue();

      largestBatch = batchIndexes.get(eventsType);
      smallestBatch = max(largestBatch - maxBatches + 1, 0); // can't be less than 0

      newestEventIndex = (smallestBatch * GetBatchOfEventsHandler.EVENTS_BATCH_SIZE);
      oldestEventIndex = (largestBatch + 1) * GetBatchOfEventsHandler.EVENTS_BATCH_SIZE;

      //start assuming all are not in the batch
      batchEventIdsToRemove = new ArrayList<>(eventsMap.keySet());

      //get all of the events from the first event to the oldest event in the batch
      if (batchEventIdsToRemove.size() > newestEventIndex) {
        //we adjust this so that we stop looping at the correct number of items
        if (batchEventIdsToRemove.size() < oldestEventIndex) {
          oldestEventIndex = batchEventIdsToRemove.size();
        }

        //sort the events
        batchEventIdsToRemove.sort((String e1, String e2) -> searchingEventsBatch.get(e1)
            .compareTo(searchingEventsBatch.get(e2)));

        //remove the event ids in the batch from those being removed
        batchEventIdsToKeep = new ArrayList<>();
        for (int i = newestEventIndex; i < oldestEventIndex; i++) {
          eventId = batchEventIdsToRemove.get(i);
          batchEventIdsToKeep.add(eventId);
        }

        batchEventIdsToRemove.removeAll(batchEventIdsToKeep);
      } // else there are no events in this range so everything gets removed

      //remove event ids not in the batch
      for (final String eventIdToRemove : batchEventIdsToRemove) {
        eventsMap.remove(eventIdToRemove);
      }
    }

    return eventTypesToEvents;
  }
}
