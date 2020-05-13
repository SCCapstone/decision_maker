package controllers;

import com.google.common.collect.ImmutableList;
import exceptions.MissingApiRequestKeyException;
import handlers.GetUserDataHandler;
import handlers.UsersManager;
import java.util.Map;
import javax.inject.Inject;
import modules.Injector;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class GetUserDataController implements ApiRequestController {

  @Inject
  public GetUserDataHandler getUserDataHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "GetUserDataController.processApiRequest";

    ResultStatus resultStatus;

    Injector.getInjector(metrics).inject(this);

    if (jsonMap.containsKey(UsersManager.USERNAME)) {
      final String username = (String) jsonMap.get(UsersManager.USERNAME);
      resultStatus = this.getUserDataHandler.handleUsername(username);
    } else if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
      resultStatus = this.getUserDataHandler.handleActiveUser(activeUser);
    } else {
      throw new MissingApiRequestKeyException(ImmutableList.of(RequestFields.ACTIVE_USER));
    }

    return resultStatus;
  }
}
