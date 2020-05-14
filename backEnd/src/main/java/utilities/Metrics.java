package utilities;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Metrics {

  //To limit hard coded metrics names/variations, I'm writing some constant ones here
  public static final String TIME = "Time";
  public static final String SUCCESS = "Success";
  public static final String FAILURE = "Failure";
  public static final String INVOCATIONS = "Invocations";

  private static final String METRIC_MARKER = "METRIC";

  private final LinkedList<String> functionNames;

  private final String requestId;
  private final LambdaLogger lambdaLogger;
  private final Map<String, Map<String, Integer>> countMetrics;
  private final Map<String, Map<String, Long>> timeMetrics;

  private Map<String, Object> requestBody;
  private boolean printMetrics;

  public Metrics(final String requestId, final LambdaLogger lambdaLogger) {
    this.functionNames = new LinkedList<>();

    this.requestId = requestId;
    this.lambdaLogger = lambdaLogger;
    this.countMetrics = new HashMap<>();
    this.timeMetrics = new HashMap<>();
    this.printMetrics = true;
  }

  public void setFunctionName(final String functionName) {
    this.functionNames.push(functionName);
  }

  public void setRequestBody(final Map<String, Object> requestBody) {
    this.requestBody = requestBody;
  }

  public void removeFunctionName() {
    this.functionNames.pop();
  }

  public String getRequestId() {
    return this.requestId;
  }

  public void commonSetup(String functionName) {
    this.setFunctionName(functionName);
    this.initTimeMetric(Metrics.TIME);
    this.incrementMetric(Metrics.INVOCATIONS);
  }

  public void commonClose(boolean success) {
    this.addBooleanMetric(success);
    this.finalizeTimeMetric(Metrics.TIME);
    this.removeFunctionName();
  }

  private void ensureFunctionKeyExists(Map input) {
    if (!input.containsKey(this.functionNames.peek())) {
      input.put(this.functionNames.peek(), new HashMap<>());
    }
  }

  public void addBooleanMetric(Boolean value) {
    String metricName = FAILURE;
    if (value) {
      metricName = SUCCESS;
    }

    this.addIntegerMetric(metricName, 1);
  }

  public void addIntegerMetric(String metricName, Integer value) {
    this.ensureFunctionKeyExists(this.countMetrics);

    if (this.countMetrics.get(this.functionNames.peek()).containsKey(metricName)) {
      this.countMetrics.get(this.functionNames.peek()).replace(metricName, value);
    } else {
      this.countMetrics.get(this.functionNames.peek()).put(metricName, value);
    }
  }

  public void addIntegerMetric(String metricName) {
    this.ensureFunctionKeyExists(this.countMetrics);

    if (!this.countMetrics.get(this.functionNames.peek()).containsKey(metricName)) {
      this.countMetrics.get(this.functionNames.peek()).put(metricName, 0);
    }
  }

  public void incrementMetric(String metricName) {
    this.ensureFunctionKeyExists(this.countMetrics);

    if (this.countMetrics.get(this.functionNames.peek()).containsKey(metricName)) {
      this.countMetrics.get(this.functionNames.peek())
          .replace(metricName,
              this.countMetrics.get(this.functionNames.peek()).get(metricName) + 1);
    } else {
      this.addIntegerMetric(metricName, 1); // it's not there -> implies it was 0
    }
  }

  public void decrementMetric(String metricName) {
    this.ensureFunctionKeyExists(this.countMetrics);

    if (this.countMetrics.get(this.functionNames.peek()).containsKey(metricName)) {
      this.countMetrics.get(this.functionNames.peek())
          .replace(metricName,
              this.countMetrics.get(this.functionNames.peek()).get(metricName) - 1);
    }
  }

  public void initTimeMetric(String metricName) {
    this.ensureFunctionKeyExists(this.timeMetrics);

    if (!this.timeMetrics.get(this.functionNames.peek()).containsKey(metricName)) {
      this.timeMetrics.get(this.functionNames.peek()).put(metricName, System.currentTimeMillis());
    }
  }

  public void finalizeTimeMetric(String metricName) {
    this.ensureFunctionKeyExists(this.timeMetrics);

    if (this.timeMetrics.get(this.functionNames.peek()).containsKey(metricName)) {
      this.timeMetrics.get(this.functionNames.peek())
          .replace(metricName,
              System.currentTimeMillis() - this.timeMetrics.get(this.functionNames.peek())
                  .get(metricName));
    }
  }

  public void log(final String message) {
    this.lambdaLogger.log(message);
  }

  public void log(final LoggingDescriptor descriptor) {
    this.lambdaLogger.log(descriptor.withRequestId(this.requestId).toString());
  }

  public void logWithBody(final LoggingDescriptor<Map> descriptor) {
    this.lambdaLogger
        .log(descriptor.withInput(this.requestBody).withRequestId(this.requestId).toString());
  }

  public void logMetrics() {
    if (this.printMetrics) {
      for (String funcName : this.countMetrics.keySet()) {
        for (String metricName : this.countMetrics.get(funcName).keySet()) {
          this.lambdaLogger.log(String
              .format("%s %s %s %s %s", this.requestId, METRIC_MARKER, funcName, metricName,
                  this.countMetrics.get(funcName).get(metricName)));
        }
      }

      for (String funcName : this.timeMetrics.keySet()) {
        for (String metricName : this.timeMetrics.get(funcName).keySet()) {
          this.lambdaLogger.log(String
              .format("%s %s %s %s %s", this.requestId, METRIC_MARKER, funcName, metricName,
                  this.timeMetrics.get(funcName).get(metricName)));
        }
      }
    }
  }

  public void setPrintMetrics(boolean printMetrics) {
    this.printMetrics = printMetrics;
  }
}
