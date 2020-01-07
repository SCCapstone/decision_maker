package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import imports.DatabaseManagers;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PendingEventsScanningHandler implements RequestStreamHandler {
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        try {
            String scannerId = System.getenv("ScannerId");
            DatabaseManagers.PENDING_EVENTS_MANAGER.scanPendingEvents(scannerId);
        } catch (Exception e) {
            //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        }
    }
}