package handlers;

import javax.inject.Inject;
import managers.DbAccessManager;
import managers.S3AccessManager;
import managers.SnsAccessManager;
import utilities.Config;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class WarmingHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;
  private S3AccessManager s3AccessManager;
  private SnsAccessManager snsAccessManager;
  private Metrics metrics;

  @Inject
  public WarmingHandler(final DbAccessManager dbAccessManager, final S3AccessManager s3AccessManager, final
      SnsAccessManager snsAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.s3AccessManager = s3AccessManager;
    this.snsAccessManager = snsAccessManager;
    this.metrics = metrics;
  }

  public ResultStatus handle() {
    final String classMethod = "WarmingHandler.handle";
    this.metrics.commonSetup(classMethod);

    //squelch metrics on warming -> we only want metrics on user impacting cold starts
    this.metrics.setPrintMetrics(false);

    ResultStatus resultStatus = new ResultStatus();

    try {
      this.dbAccessManager.describeTables();
      this.s3AccessManager.imageBucketExists();
      this.snsAccessManager.getPlatformAttributes(Config.PUSH_SNS_PLATFORM_ARN);

      resultStatus = new ResultStatus(true, "Endpoints warmed.");
    } catch (Exception e) {
      this.metrics.log(new ErrorDescriptor<>(classMethod, e));
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
