package handlers;

import java.util.Map;
import managers.DbAccessManager;
import utilities.Config;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class WarmingHandler extends ApiRequestHandler {

  public WarmingHandler(final DbAccessManager dbAccessManager,
      final Map<String, Object> requestBody, final Metrics metrics) {
    super(dbAccessManager, requestBody, metrics);
  }

  @Override
  public ResultStatus handle() {
    final String classMethod = "WarmingHandler.handle";
    this.metrics.commonSetup(classMethod);

    //squelch metrics on warming -> we only want metrics on user impacting cold starts
    this.metrics.setPrintMetrics(false);

    ResultStatus resultStatus = new ResultStatus();

    try {
      this.dbAccessManager.describeTables();
      DatabaseManagers.S3_ACCESS_MANAGER.imageBucketExists();
      DatabaseManagers.SNS_ACCESS_MANAGER.getPlatformAttributes(Config.PUSH_SNS_PLATFORM_ARN);

      resultStatus = new ResultStatus(true, "Endpoints warmed.");
    } catch (Exception e) {
      this.metrics.log(new ErrorDescriptor<>("", classMethod, e));
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
