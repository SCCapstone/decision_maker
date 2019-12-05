package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import imports.PendingEventsManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import utilities.JsonParsers;

public class PendingEventResolutionHandler implements RequestStreamHandler {
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    try {
      Map<String, Object> jsonMap = JsonParsers.parseInput(inputStream);
      PendingEventsManager.processPendingEvent(jsonMap);
    } catch (Exception e) {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
    }
  }
}
