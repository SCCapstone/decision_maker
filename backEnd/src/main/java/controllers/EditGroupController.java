package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.EditGroupHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import models.Group;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class EditGroupController implements ApiRequestController {

  @Inject
  public EditGroupHandler editGroupHandler;

  @Override
  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "EditGroupController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, Group.GROUP_ID, Group.GROUP_NAME, Group.MEMBERS,
            Group.CATEGORIES, Group.IS_OPEN);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(Group.GROUP_ID);
        final String groupName = (String) jsonMap.get(Group.GROUP_NAME);
        final List<String> members = (List<String>) jsonMap.get(Group.MEMBERS);
        final List<String> categories = (List<String>) jsonMap.get(Group.CATEGORIES);
        final Boolean isOpen = (Boolean) jsonMap.get(Group.IS_OPEN);

        //optional request keys
        final List<Integer> iconData = (List<Integer>) jsonMap.get(Group.ICON);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.editGroupHandler
            .handle(activeUser, groupId, groupName, members, categories, isOpen, iconData);
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
