package controllers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import handlers.DatabaseManagers;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import utilities.ErrorDescriptor;
import utilities.IOStreamsHelper;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.ResultStatus;

public class PendingEventResolutionHandler implements RequestStreamHandler {

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    Metrics metrics = new Metrics(context.getAwsRequestId(), context.getLogger());

    final String classMethod = "PendingEventResolutionHandler.handleRequest";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      Map<String, Object> jsonMap = JsonUtils.parseInput(inputStream);
      resultStatus = DatabaseManagers.PENDING_EVENTS_MANAGER.processPendingEvent(jsonMap, metrics);
    } catch (Exception e) {
      resultStatus.resultMessage = "Exception occurred in handler.";
      metrics.log(new ErrorDescriptor<>(null, classMethod, e));
    }

    metrics.commonClose(resultStatus.success);
    metrics.logMetrics();
    IOStreamsHelper.writeToOutput(outputStream, resultStatus.toString());
  }
}