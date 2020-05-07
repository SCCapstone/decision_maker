package controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import exceptions.MissingApiRequestKeyException;
import factories.AddNewCategoryFactory;
import factories.UpdateUserChoiceRatingsFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import models.Category;
import modules.PocketPollModule;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class UpdateUserChoiceRatingsController {

  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "UpdateUserChoiceRatingsHandler.handle";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, Category.CATEGORY_ID, RequestFields.USER_RATINGS);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get((RequestFields.ACTIVE_USER));
        final String categoryId = (String) jsonMap.get(Category.CATEGORY_ID);
        final Map<String, Object> userRatings = (Map<String, Object>) jsonMap
            .get(RequestFields.USER_RATINGS);

        final Injector injector = Guice.createInjector(new PocketPollModule());
        final UpdateUserChoiceRatingsFactory updateUserChoiceRatingsFactory = injector
            .getInstance(UpdateUserChoiceRatingsFactory.class);
        resultStatus = updateUserChoiceRatingsFactory.create(jsonMap, metrics)
            .handle(activeUser, categoryId, userRatings, true);
      } catch (final Exception e) {
        //something couldn't get parsed
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus = ResultStatus.failure("Exception in " + classMethod);
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}
