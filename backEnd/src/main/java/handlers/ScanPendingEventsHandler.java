package handlers;

import com.google.common.collect.ImmutableMap;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.DbAccessManager;
import managers.StepFunctionManager;
import models.Group;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class ScanPendingEventsHandler {

  private static final String DELIM = ";";

  private final DbAccessManager dbAccessManager;
  private final StepFunctionManager stepFunctionManager;
  private final Metrics metrics;

  @Inject
  public ScanPendingEventsHandler(final DbAccessManager dbAccessManager,
      final StepFunctionManager stepFunctionManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.stepFunctionManager = stepFunctionManager;
    this.metrics = metrics;
  }

  /**
   * This method is run every minute by a cron job. It looks at one of the partitions of the pending
   * events table and if there are any pending events that are ready to be processed, then it kicks
   * off a step function process to handle that event's resolution.
   *
   * @param scannerId The partition of the table that this function should scan for ready events.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String scannerId) {
    final String classMethod = "ScanPendingEventsHandler.handle";
    this.metrics.commonSetup(classMethod);

    //assume true and set to false if anything fails
    ResultStatus resultStatus = ResultStatus.successful("Pending events scanned successfully");

    try {
      final Map<String, Object> pendingEventsData = this.dbAccessManager.getPendingEvents(scannerId)
          .asMap();
      final LocalDateTime currentDate = LocalDateTime.now(ZoneId.of("UTC"));
      LocalDateTime resolutionDate;

      String groupId, eventId;
      List<String> keyPair;
      for (String key : pendingEventsData.keySet()) {
        //skip the scanner id key as it isn't real pending event data
        if (!key.equals(DbAccessManager.PENDING_EVENTS_PRIMARY_KEY)) {
          resolutionDate = LocalDateTime
              .parse((String) pendingEventsData.get(key),
                  this.dbAccessManager.getDateTimeFormatter());

          if (currentDate.isAfter(resolutionDate)) {
            keyPair = Arrays.asList(key.split(DELIM));

            if (keyPair.size() == 2) {
              groupId = keyPair.get(0);
              eventId = keyPair.get(1);

              if (!this.startStepMachineExecution(groupId, eventId, scannerId)) {
                resultStatus = ResultStatus.failure("Step Function failed to start.");
              }
            } else {
              metrics.log(new ErrorDescriptor<>("scanner id: " + scannerId + ", key : " + key,
                  classMethod, "bad format for key in pending events table"));
              resultStatus = ResultStatus.failure("Bad key format in pending events partition.");
            }
          }
        }
      }
    } catch (Exception e) {
      this.metrics.log(new ErrorDescriptor<>("scanner id: " + scannerId, classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private boolean startStepMachineExecution(final String groupId, final String eventId,
      final String scannerId) {
    final String classMethod = "ScanPendingEventsHandler.startStepMachineExecution";
    this.metrics.commonSetup(classMethod);

    boolean success = true;

    Map<String, Object> input = ImmutableMap
        .of(Group.GROUP_ID, groupId, RequestFields.EVENT_ID, eventId,
            DbAccessManager.PENDING_EVENTS_PRIMARY_KEY, scannerId);

    try {
      this.stepFunctionManager.startStepMachine(input);
    } catch (final Exception e) {
      success = false;
      this.metrics.log(new ErrorDescriptor<>(input, classMethod, e));
    }

    this.metrics.commonClose(success);
    return success;
  }
}
