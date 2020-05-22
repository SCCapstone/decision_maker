package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.NewCategoryHandler;
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

public class NewCategoryController implements ApiRequestController {

  @Inject
  public NewCategoryHandler newCategoryHandler;

  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "NewCategoryController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, RequestFields.USER_RATINGS, Category.CATEGORY_NAME,
            Category.CHOICES);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get((RequestFields.ACTIVE_USER));
        final String categoryName = (String) jsonMap.get(Category.CATEGORY_NAME);
        final Map<String, Object> choices = (Map<String, Object>) jsonMap.get(Category.CHOICES);
        final Map<String, Object> userRatings = (Map<String, Object>) jsonMap
            .get(RequestFields.USER_RATINGS);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.newCategoryHandler
            .handle(activeUser, categoryName, choices, userRatings);
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
