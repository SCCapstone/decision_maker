package handlers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import managers.DbAccessManager;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class AddPendingEventHandler implements ApiRequestHandler {

  private static final String DELIM = ";";
  private static final String NUMBER_OF_PARTITIONS_ENV_KEY = "NUMBER_OF_PARTITIONS";

  private DbAccessManager dbAccessManager;
  private Metrics metrics;

  @Inject
  public AddPendingEventHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method handles adding a pending event into one of the pending event table partitions.
   *
   * @param groupId      The group that the pending event belongs to.
   * @param eventId      The event that is pending.
   * @param pollDuration The duration that the event will pend for.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String groupId, final String eventId,
      final Integer pollDuration) {
    if (pollDuration <= 0) { // there no 'pending' needed for zero minutes of duration
      return new ResultStatus(true, "No insert needed");
    }

    final String classMethod = "PendingEventsManager.addPendingEvent";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      final String partitionKey = this.getPartitionKey();
      final LocalDateTime expirationDate = LocalDateTime.now(ZoneId.of("UTC"))
          .plus(pollDuration, ChronoUnit.MINUTES);

      final String updateExpression = "set #key = :date";
      final ValueMap valueMap = new ValueMap()
          .withString(":date", expirationDate.format(this.dbAccessManager.getDateTimeFormatter()));
      final NameMap nameMap = new NameMap().with("#key", groupId + DELIM + eventId);

      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withNameMap(nameMap)
          .withValueMap(valueMap); // only modifying my row

      this.dbAccessManager.updatePendingEvent(partitionKey, updateItemSpec);

      resultStatus = new ResultStatus(true, "Pending event inserted successfully.");
    } catch (Exception e) {
      resultStatus.resultMessage = "Error adding pending event.";
      metrics.log(new ErrorDescriptor<>(String
          .format("Group: %s, Event: %s, duration: %s", groupId, eventId, pollDuration.toString()),
          classMethod, e));
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public String getPartitionKey() throws NullPointerException, NumberFormatException {
    //this gives a 'randomized' key based on the system's clock time.
    return Long.toString(
        (System.currentTimeMillis() % Integer.parseInt(System.getenv(NUMBER_OF_PARTITIONS_ENV_KEY)))
            + 1);
  }
}
