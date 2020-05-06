package imports;

import java.util.Map;
import utilities.Metrics;
import utilities.ResultStatus;

public interface ApiRequestHandler {
  public ResultStatus handle(final Map<String, Object> jsonMap, final Metrics metrics);
}
