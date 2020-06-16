package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.ReportGroupHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import models.Group;
import models.Report;
import models.User;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class ReportGroupController implements ApiRequestController {

  @Inject
  public ReportGroupHandler reportGroupHandler;

  @Override
  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "ReportGroupController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, Group.GROUP_ID, Report.REPORT_MESSAGE);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String reportedGroupId = (String) jsonMap.get(Group.GROUP_ID);
        final String reportMessage = (String) jsonMap.get(Report.REPORT_MESSAGE);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.reportGroupHandler.handle(activeUser, reportedGroupId, reportMessage);
      } catch (Exception e) {
        metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
        resultStatus = ResultStatus.failure("Exception in " + classMethod);
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}
