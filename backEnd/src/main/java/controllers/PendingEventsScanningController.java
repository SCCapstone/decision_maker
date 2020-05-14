package controllers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.google.common.collect.ImmutableMap;
import handlers.ScanPendingEventsHandler;
import java.io.InputStream;
import java.io.OutputStream;
import javax.inject.Inject;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class PendingEventsScanningController implements RequestStreamHandler {

  private static final String SCANNER_ID_ENV_KEY = "ScannerId";

  @Inject
  public ScanPendingEventsHandler scanPendingEventsHandler;

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
    final String classMethod = "PendingEventsScanningHandler.handleRequest";
    final Metrics metrics = new Metrics(context.getAwsRequestId(), context.getLogger());

    metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final String scannerId = System.getenv(SCANNER_ID_ENV_KEY);
      metrics.setRequestBody(ImmutableMap.of(SCANNER_ID_ENV_KEY, scannerId));

      Injector.getInjector(metrics).inject(this);
      resultStatus = this.scanPendingEventsHandler.handle(scannerId);
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    metrics.commonClose(resultStatus.success);
    metrics.logMetrics();
  }
}
