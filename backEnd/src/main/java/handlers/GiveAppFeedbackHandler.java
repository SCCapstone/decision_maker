package handlers;

import java.util.UUID;
import javax.inject.Inject;
import managers.DbAccessManager;
import managers.SnsAccessManager;
import models.Feedback;
import models.Group;
import models.Report;
import utilities.Config;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class GiveAppFeedbackHandler implements ApiRequestHandler {

  private final DbAccessManager dbAccessManager;
  private final SnsAccessManager snsAccessManager;
  private final Metrics metrics;

  @Inject
  public GiveAppFeedbackHandler(final DbAccessManager dbAccessManager,
      final SnsAccessManager snsAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.snsAccessManager = snsAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method takes in the report form entries for reporting a user and logs this report in the
   * database. In addition, an email gets sent out the the development team so that they can look
   * into the report.
   *
   * @param activeUser      The active user doing the reporting.
   * @param feedbackMessage The id of the group being reported.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String feedbackMessage) {
    final String classMethod = "GiveAppFeedbackHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Feedback feedback = new Feedback(UUID.randomUUID().toString(), activeUser,
          feedbackMessage, this.dbAccessManager.now());

      this.dbAccessManager.putFeedback(feedback);

      //the db entry has been recorded, now time to send the email to the dev team
      this.snsAccessManager.sendEmail(Config.SNS_REPORT_TOPIC_ARN, feedback.getEmailSubject(),
          feedback.getEmailBody());

      resultStatus = new ResultStatus(true, "Report processed successfully.");
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
