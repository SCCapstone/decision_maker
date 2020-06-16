package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.GiveAppFeedbackHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import models.Report;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class GiveAppFeedbackController implements ApiRequestController {

  @Inject
  public GiveAppFeedbackHandler giveAppFeedbackHandler;

  @Override
  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "GiveAppFeedbackController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, Report.REPORT_MESSAGE);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String feedbackMessage = (String) jsonMap.get(Report.REPORT_MESSAGE);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.giveAppFeedbackHandler.handle(activeUser, feedbackMessage);
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
