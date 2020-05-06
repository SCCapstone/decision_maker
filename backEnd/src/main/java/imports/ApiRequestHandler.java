package imports;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import utilities.Metrics;
import utilities.ResultStatus;

@RequiredArgsConstructor
public class ApiRequestHandler {

  public final DbAccessManager dbAccessManager;
  public final Map<String, Object> requestBody;
  public final Metrics metrics;

  // this must be overwritten otherwise this exception will be thrown
  public ResultStatus handle() {
    throw new NotImplementedException();
  }
}
