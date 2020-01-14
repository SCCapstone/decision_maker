package utilities;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.util.HashMap;
import java.util.Map;

public class Metrics {

  private static final String METRIC_MARKER = "METRIC";

  private String requestId;

  private String functionName;
  private Map<String, Map<String, Boolean>> booleanMetrics;
  private Map<String, Map<String, Integer>> countMetrics;
  private Map<String, Map<String, Long>> timeMetrics;

  public Metrics(String requestId) {
    this.requestId = requestId;
    this.functionName = "";
    this.booleanMetrics = new HashMap<>();
    this.countMetrics = new HashMap<>();
    this.timeMetrics = new HashMap<>();
  }

  public void setFunctionName(String functionName) {
    this.functionName = functionName;
  }

  private void ensureFunctionKeyExists(Map input) {
    if (!input.containsKey(this.functionName)) {
      input.put(this.functionName, new HashMap<>());
    }
  }

  public void addBooleanMetric(String metricName, Boolean value) {
    this.ensureFunctionKeyExists(this.booleanMetrics);

    if (this.booleanMetrics.get(this.functionName).containsKey(metricName)) {
      this.booleanMetrics.get(this.functionName).replace(metricName, value);
    } else {
      this.booleanMetrics.get(this.functionName).put(metricName, value);
    }
  }

  public void addIntegerMetric(String metricName, Integer value) {
    this.ensureFunctionKeyExists(this.countMetrics);

    if (this.countMetrics.get(this.functionName).containsKey(metricName)) {
      this.countMetrics.get(this.functionName).replace(metricName, value);
    } else {
      this.countMetrics.get(this.functionName).put(metricName, value);
    }
  }

  public void addIntegerMetric(String metricName) {
    this.ensureFunctionKeyExists(this.countMetrics);

    if (!this.countMetrics.get(this.functionName).containsKey(metricName)) {
      this.countMetrics.get(this.functionName).put(metricName, 0);
    }
  }

  public void incrementMetric(String metricName) {
    this.ensureFunctionKeyExists(this.countMetrics);

    if (this.countMetrics.get(this.functionName).containsKey(metricName)) {
      this.countMetrics.get(this.functionName)
          .replace(metricName, this.countMetrics.get(this.functionName).get(metricName) + 1);
    } else {
      this.addIntegerMetric(metricName, 1); // it's not there -> implies it was 0
    }
  }

  public void decrementMetric(String metricName) {
    this.ensureFunctionKeyExists(this.countMetrics);

    if (this.countMetrics.get(this.functionName).containsKey(metricName)) {
      this.countMetrics.get(this.functionName)
          .replace(metricName, this.countMetrics.get(this.functionName).get(metricName) - 1);
    }
  }

  public void initTimeMetric(String metricName, Long time) {
    this.ensureFunctionKeyExists(this.timeMetrics);

    if (!this.timeMetrics.get(this.functionName).containsKey(metricName)) {
      this.timeMetrics.get(this.functionName).put(metricName, time);
    }
  }

  public void finalizeTimeMetric(String metricName, Long time) {
    this.ensureFunctionKeyExists(this.timeMetrics);

    if (this.timeMetrics.get(this.functionName).containsKey(metricName)) {
      this.timeMetrics.get(this.functionName)
          .replace(metricName, time - this.timeMetrics.get(this.functionName).get(metricName));
    }
  }

  public void logMetrics(LambdaLogger logger) {
    for (String funcName : this.booleanMetrics.keySet()) {
      for (String metricName : this.booleanMetrics.get(funcName).keySet()) {
        logger.log(String
            .format("%s %s %s %s %s", this.requestId, METRIC_MARKER, funcName, metricName,
                this.booleanMetrics.get(funcName).get(metricName)));
      }
    }

    for (String funcName : this.countMetrics.keySet()) {
      for (String metricName : this.countMetrics.get(funcName).keySet()) {
        logger.log(String
            .format("%s %s %s %s %s", this.requestId, METRIC_MARKER, funcName, metricName,
                this.countMetrics.get(funcName).get(metricName)));
      }
    }

    for (String funcName : this.timeMetrics.keySet()) {
      for (String metricName : this.timeMetrics.get(funcName).keySet()) {
        logger.log(String
            .format("%s %s %s %s %s", this.requestId, METRIC_MARKER, funcName, metricName,
                this.timeMetrics.get(funcName).get(metricName)));
      }
    }
  }
}
