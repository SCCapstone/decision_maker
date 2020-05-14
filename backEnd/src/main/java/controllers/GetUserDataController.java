package controllers;

import com.google.common.collect.ImmutableList;
import exceptions.MissingApiRequestKeyException;
import handlers.GetUserDataHandler;
import java.util.Map;
import javax.inject.Inject;
import models.User;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class GetUserDataController implements ApiRequestController {

  @Inject
  public GetUserDataHandler getUserDataHandler;

  @Override
  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "GetUserDataController.processApiRequest";

    ResultStatus resultStatus;

    try {
      Injector.getInjector(metrics).inject(this);

      if (jsonMap.containsKey(User.USERNAME)) {
        final String username = (String) jsonMap.get(User.USERNAME);
        resultStatus = this.getUserDataHandler.handleUsername(username);
      } else if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        resultStatus = this.getUserDataHandler.handleActiveUser(activeUser);
      } else {
        throw new MissingApiRequestKeyException(ImmutableList.of(RequestFields.ACTIVE_USER));
      }
    } catch (final MissingApiRequestKeyException marke) {
      throw marke;
    } catch (final Exception e) {
      metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    return resultStatus;
  }
}
