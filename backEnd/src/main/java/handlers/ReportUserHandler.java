package handlers;

import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.Group;
import models.Report;
import models.User;
import models.UserGroup;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;
import utilities.UpdateItemData;

public class ReportUserHandler {

  private DbAccessManager dbAccessManager;
  private Metrics metrics;

  @Inject
  public ReportUserHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method takes in the report form entries for reporting a user and logs this report in the
   * database. In addition, an email gets sent out the the development team so that they can look
   * into the report.
   *
   * @param activeUser       The active user doing the reporting.
   * @param reportedUsername The username of the user being reported.
   * @param reportMessage    The reasoning as to why the active user is reporting the reported
   *                         username.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String reportedUsername,
      final String reportMessage) {
    final String classMethod = "ReportUserHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final User reportedUserObj = this.dbAccessManager.getUser(reportedUsername);

      final Report userReport = Report
          .user(activeUser, reportedUsername, reportMessage, reportedUserObj.asMember().asMap(),
              this.dbAccessManager.now());


      this.dbAccessManager.putReport(userReport);

      //the db entry has been recorded, now time to send the email to the dev team

      resultStatus = new ResultStatus(true, "Report logged successfully.");
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
