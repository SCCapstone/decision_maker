package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import imports.DatabaseManagers;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import utilities.Metrics;

public class PendingEventsScanningHandler implements RequestStreamHandler {

  public static final String SCANNER_ID_ENV_KEY = "ScannerId";

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    Metrics metrics = new Metrics(context.getAwsRequestId(), context.getLogger());

    try {
      String scannerId = System.getenv(SCANNER_ID_ENV_KEY);
      DatabaseManagers.PENDING_EVENTS_MANAGER.scanPendingEvents(scannerId, metrics);
    } catch (Exception e) {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
    }

    metrics.logMetrics();
  }
}
