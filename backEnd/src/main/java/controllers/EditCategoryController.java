package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.EditCategoryHandler;
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

public class EditCategoryController implements ApiRequestController {

  @Inject
  public EditCategoryHandler editCategoryHandler;

  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "EditCategoryHandler.handle";

    ResultStatus resultStatus = new ResultStatus();

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, RequestFields.USER_RATINGS, Category.CATEGORY_ID,
            Category.CATEGORY_NAME, Category.CHOICES);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get((RequestFields.ACTIVE_USER));
        final String categoryId = (String) jsonMap.get(Category.CATEGORY_ID);
        final String categoryName = (String) jsonMap.get(Category.CATEGORY_NAME);
        final Map<String, Object> choices = (Map<String, Object>) jsonMap.get(Category.CHOICES);
        final Map<String, Object> userRatings = (Map<String, Object>) jsonMap
            .get(RequestFields.USER_RATINGS);

        //resultStatus = this.handle(activeUser, categoryId, categoryName, choices, userRatings);
        Injector.getInjector(metrics).inject(this);
        resultStatus = this.editCategoryHandler
            .handle(activeUser, categoryId, categoryName, choices, userRatings);
      } catch (final Exception e) {
        //something couldn't get parsed
        metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
        resultStatus.resultMessage = "Error: Invalid request.";
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}
