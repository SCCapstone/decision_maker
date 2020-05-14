package controllers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import exceptions.MissingApiRequestKeyException;
import handlers.ProcessPendingEventHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.Group;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.IOStreamsHelper;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class PendingEventResolutionController implements RequestStreamHandler {

  @Inject
  public ProcessPendingEventHandler processPendingEventHandler;

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    Metrics metrics = new Metrics(context.getAwsRequestId(), context.getLogger());

    final String classMethod = "PendingEventResolutionController.handleRequest";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Map<String, Object> jsonMap = JsonUtils.parseInput(inputStream);
      metrics.setRequestBody(jsonMap);

      final List<String> requiredKeys = Arrays
          .asList(Group.GROUP_ID, RequestFields.EVENT_ID,
              DbAccessManager.PENDING_EVENTS_PRIMARY_KEY);

      if (jsonMap.keySet().containsAll(requiredKeys)) {
        try {
          final String groupId = (String) jsonMap.get(Group.GROUP_ID);
          final String eventId = (String) jsonMap.get(RequestFields.EVENT_ID);
          final String scannerId = (String) jsonMap.get(DbAccessManager.PENDING_EVENTS_PRIMARY_KEY);

          Injector.getInjector(metrics).inject(this);
          resultStatus = this.processPendingEventHandler.handle(groupId, eventId, scannerId);
        } catch (Exception e) {
          resultStatus = ResultStatus.failure("Exception in " + classMethod);
          metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
        }
      } else {
        throw new MissingApiRequestKeyException(requiredKeys);
      }
    } catch (Exception e) {
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
      metrics.log(new ErrorDescriptor<>(classMethod, e));
    }

    metrics.commonClose(resultStatus.success);
    metrics.logMetrics();

    IOStreamsHelper.writeToOutput(outputStream, resultStatus.toString());
  }
}
