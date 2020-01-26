package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import imports.DatabaseManagers;
import utilities.Metrics;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PendingEventsScanningHandler implements RequestStreamHandler {
    public static final String SCANNER_ID_ENV_KEY = "ScannerId";

    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        Metrics metrics = new Metrics(context.getAwsRequestId());
        LambdaLogger lambdaLogger = context.getLogger();

        try {
            String scannerId = System.getenv(SCANNER_ID_ENV_KEY);
            DatabaseManagers.PENDING_EVENTS_MANAGER.scanPendingEvents(scannerId, metrics, lambdaLogger);
        } catch (Exception e) {
            //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        }
    }
}
