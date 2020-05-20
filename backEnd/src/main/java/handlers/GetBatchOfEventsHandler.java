package handlers;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import managers.DbAccessManager;
import models.Event;
import models.EventForSorting;
import models.Group;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class GetBatchOfEventsHandler implements ApiRequestHandler {

  public static final Integer EVENTS_BATCH_SIZE = 25;

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
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String groupId,
      final Integer batchNumber) {
    final String classMethod = "GetBatchOfEventsHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Group group = this.dbAccessManager.getGroup(groupId);

      //the user should not be able to retrieve info from the group if they are not a member
      if (group.getMembers().containsKey(activeUser)) {
        //we set the events on the group so we can use the group's getEventsMap method
        group.setEvents(GetBatchOfEventsHandler.handle(group, batchNumber));

        resultStatus = new ResultStatus(true,
            JsonUtils.convertObjectToJson(group.getEventsMap()));
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
   * @param group       The group we are trying to get a batch of events for.
   * @param batchNumber The index of events to get. Index i gets events (i, (i + 1) * batch size]
   * @return A mapping of eventIds to event objects contained in the requested batch.
   */
  public static Map<String, Event> handle(final Group group, final Integer batchNumber) {
    Integer newestEventIndex = (batchNumber * EVENTS_BATCH_SIZE);
    Integer oldestEventIndex = (batchNumber + 1) * EVENTS_BATCH_SIZE;

    //linked hash maps maintain order whereas normal hash maps do not
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

      //then we sort all of the events up to the oldestEvent being asked for
      eventsBatch = searchingEventsBatch
          .entrySet()
          .stream()
          .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
          .limit(oldestEventIndex)
          .collect(collectingAndThen(toMap(Entry::getKey, (Map.Entry e) -> (Event) e.getValue()),
              LinkedHashMap::new));
    } // else there are no events in this range and we return the empty map

    //then we sort in the opposite direction and get the appropriate number of events for the batch
    final List<String> reverseOrderKeys = new ArrayList<>(eventsBatch.keySet());
    Collections.reverse(reverseOrderKeys);

    Map<String, Event> temp = new HashMap<>();
    for (String eventId : reverseOrderKeys) {
      temp.put(eventId, eventsBatch.get(eventId));

      if (temp.size() >= oldestEventIndex - newestEventIndex) {
        break;
      }
    }

    eventsBatch = temp;

    return eventsBatch;
  }
}
