package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import imports.DatabaseManagers;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import utilities.ExceptionHelper;
import utilities.IOStreamsHelper;
import utilities.JsonParsers;
import utilities.ResultStatus;

public class PendingEventResolutionHandler implements RequestStreamHandler {
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    ResultStatus resultStatus = new ResultStatus();
    try {
      Map<String, Object> jsonMap = JsonParsers.parseInput(inputStream);
      resultStatus = DatabaseManagers.PENDING_EVENTS_MANAGER.processPendingEvent(jsonMap);
    } catch (Exception e) {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      resultStatus.resultMessage = ExceptionHelper.getStackTrace(e);
    }

    IOStreamsHelper.writeToOutput(outputStream, resultStatus.toString());
  }
}
