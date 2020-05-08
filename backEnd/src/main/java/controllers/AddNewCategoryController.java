package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.AddNewCategoryHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.NoArgsConstructor;
import models.Category;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

@NoArgsConstructor
public class AddNewCategoryController implements ApiRequestController {

  @Inject
  public AddNewCategoryHandler addNewCategoryHandler;

  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "AddNewCategoryController.processApiRequest";

    ResultStatus resultStatus = new ResultStatus();

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
        resultStatus = this.addNewCategoryHandler
            .handle(activeUser, categoryName, choices, userRatings);
      } catch (final Exception e) {
        //something couldn't get parsed
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus.resultMessage = "Error: Invalid request.";
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}
