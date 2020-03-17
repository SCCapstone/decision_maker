package imports;

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

      resultStatus = new ResultStatus(true, "Endpoints warmed.");
    } catch (Exception e) {
      metrics.log(new ErrorDescriptor<>("input", classMethod, e));
    }

    return resultStatus;
  }
}
