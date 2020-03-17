package imports;

import utilities.Config;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class WarmingManager {
  public ResultStatus warmDynamoDBConnections(final Metrics metrics) {
    final String classMethod = "WarmingManager.warmDynamoDBConnections";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      DatabaseManagers.USERS_MANAGER.describeTable();
      DatabaseManagers.CATEGORIES_MANAGER.describeTable();
      DatabaseManagers.USERS_MANAGER.describeTable();
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
