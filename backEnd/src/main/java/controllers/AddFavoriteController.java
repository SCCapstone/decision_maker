package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.AddFavoriteHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import models.Group;
import models.User;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class AddFavoriteController implements ApiRequestController {

  @Inject
  public AddFavoriteHandler addFavoriteHandler;

  @Override
  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "AddFavoriteController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays.asList(RequestFields.ACTIVE_USER, User.USERNAME);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String favoriteUsername = (String) jsonMap.get(User.USERNAME);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.addFavoriteHandler.handle(activeUser, favoriteUsername);
      } catch (Exception e) {
        resultStatus = ResultStatus.failure("Exception in " + classMethod);
        metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}
