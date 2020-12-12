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

  private final DbAccessManager dbAccessManager;
  private final S3AccessManager s3AccessManager;
  private final SnsAccessManager snsAccessManager;
  private final Metrics metrics;

  @Inject
  public WarmingHandler(final DbAccessManager dbAccessManager,
      final S3AccessManager s3AccessManager, final SnsAccessManager snsAccessManager,
      final Metrics metrics) {
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

    ResultStatus resultStatus;

    try {
      this.dbAccessManager.describeTables();
      // this line counts as an s3 get request which is consuming our s3 free tier limit
      // there are few cases where a warm connection to s3 is necessary and we have few users
//      this.s3AccessManager.imageBucketExists();
      this.snsAccessManager.getPlatformAttributes(Config.PUSH_SNS_PLATFORM_ARN);

      resultStatus = ResultStatus.successful("Endpoints warmed.");
    } catch (Exception e) {
      this.metrics.log(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
