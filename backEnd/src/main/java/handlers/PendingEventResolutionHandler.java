package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import imports.DatabaseManagers;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import utilities.ErrorDescriptor;
import utilities.IOStreamsHelper;
import utilities.JsonParsers;
import utilities.Metrics;
import utilities.ResultStatus;

public class PendingEventResolutionHandler implements RequestStreamHandler {

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    Metrics metrics = new Metrics(context.getAwsRequestId(), context.getLogger());
    LambdaLogger lambdaLogger = context.getLogger();

    final String classMethod = "PendingEventResolutionHandler.handleRequest";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      Map<String, Object> jsonMap = JsonParsers.parseInput(inputStream);
      resultStatus = DatabaseManagers.PENDING_EVENTS_MANAGER
          .processPendingEvent(jsonMap, metrics, lambdaLogger);
    } catch (Exception e) {
      resultStatus.resultMessage = "Exception occurred in handler.";
      lambdaLogger
          .log(new ErrorDescriptor<>(null, classMethod, metrics.getRequestId(), e).toString());
    }

    metrics.commonClose(resultStatus.success);
    metrics.logMetrics();
    IOStreamsHelper.writeToOutput(outputStream, resultStatus.toString());
  }
}
