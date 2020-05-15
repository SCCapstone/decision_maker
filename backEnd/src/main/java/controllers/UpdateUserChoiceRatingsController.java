package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.UpdateUserChoiceRatingsHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import models.Category;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class UpdateUserChoiceRatingsController implements ApiRequestController {

  @Inject
  public UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler;

  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "UpdateUserChoiceRatingsHandler.handle";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, Category.CATEGORY_ID, Category.VERSION,
            RequestFields.USER_RATINGS);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get((RequestFields.ACTIVE_USER));
        final String categoryId = (String) jsonMap.get(Category.CATEGORY_ID);
        final Integer categoryVersion = (Integer) jsonMap.get(Category.VERSION);
        final Map<String, Object> userRatings = (Map<String, Object>) jsonMap
            .get(RequestFields.USER_RATINGS);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.updateUserChoiceRatingsHandler
            .handle(activeUser, categoryId, categoryVersion, userRatings, true);
      } catch (final Exception e) {
        metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
        resultStatus = ResultStatus.failure("Exception in " + classMethod);
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}
