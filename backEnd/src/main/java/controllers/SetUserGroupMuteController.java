package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.SetUserGroupMuteHandler;
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

public class SetUserGroupMuteController implements ApiRequestController {

  @Inject
  public SetUserGroupMuteHandler setUserGroupMuteHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "SetUserGroupMuteController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, Group.GROUP_ID, User.APP_SETTINGS_MUTED);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(Group.GROUP_ID);
        final Boolean muteValue = (Boolean) jsonMap.get(User.APP_SETTINGS_MUTED);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.setUserGroupMuteHandler.handle(activeUser, groupId, muteValue);
      } catch (final Exception e) {
        resultStatus = ResultStatus.failure("Exception in " + classMethod);
        metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}
