package handlers;

import javax.inject.Inject;
import managers.DbAccessManager;
import managers.SnsAccessManager;
import models.Group;
import models.Report;
import models.User;
import utilities.Config;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class ReportGroupHandler implements ApiRequestHandler {

  private final DbAccessManager dbAccessManager;
  private final SnsAccessManager snsAccessManager;
  private final Metrics metrics;

  @Inject
  public ReportGroupHandler(final DbAccessManager dbAccessManager,
      final SnsAccessManager snsAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.snsAccessManager = snsAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method takes in the report form entries for reporting a group and logs this report in the
   * database. In addition, an email gets sent out the the development team so that they can look
   * into the report.
   *
   * @param activeUser      The active user doing the reporting.
   * @param reportedGroupId The id of the group being reported.
   * @param reportMessage   The reasoning as to why the active user is reporting the reported
   *                        group.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String reportedGroupId,
      final String reportMessage) {
    final String classMethod = "ReportGroupHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Group reportedGroupObj = this.dbAccessManager.getGroup(reportedGroupId);

      final Report userReport = Report
          .group(activeUser, reportedGroupId, reportMessage, reportedGroupObj.getReportSnapshot(),
              this.dbAccessManager.now());

      this.dbAccessManager.putReport(userReport);

      //the db entry has been recorded, now time to send the email to the dev team
      this.snsAccessManager.sendEmail(Config.SNS_REPORT_TOPIC_ARN, userReport.getEmailSubject(),
          userReport.getEmailBody());

      resultStatus = new ResultStatus(true, "Report processed successfully.");
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
