package imports;

import java.util.Map;
import lombok.AllArgsConstructor;
import utilities.Config;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

@AllArgsConstructor
public class WarmingHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;

  public ResultStatus handle(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "WarmingHandler.handle";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      this.dbAccessManager.describeTables();
      DatabaseManagers.S3_ACCESS_MANAGER.imageBucketExists();
      DatabaseManagers.SNS_ACCESS_MANAGER.getPlatformAttributes(Config.PUSH_SNS_PLATFORM_ARN);

      resultStatus = new ResultStatus(true, "Endpoints warmed.");
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>("input", classMethod, e));
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
