package controllers;

import com.google.common.collect.ImmutableList;
import exceptions.MissingApiRequestKeyException;
import handlers.GetCategoriesHandler;
import handlers.GroupsManager;
import java.util.ArrayList;
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

public class GetCategoriesController implements ApiRequestController {

  @Inject
  public GetCategoriesHandler getCategoriesHandler;

  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "AddNewCategoryController.processApiRequest";

    ResultStatus resultStatus = new ResultStatus();

    Injector.getInjector(metrics).inject(this);

    if (jsonMap.containsKey(RequestFields.CATEGORY_IDS)) {
      resultStatus = this.getCategoriesHandler
          .handle((List<String>) jsonMap.get(RequestFields.CATEGORY_IDS));
    } else if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

      if (jsonMap.containsKey(GroupsManager.GROUP_ID)) {
        final String groupId = (String) jsonMap.get(Group.GROUP_ID);

        resultStatus = this.getCategoriesHandler.handle(activeUser, groupId);
      } else {
        resultStatus = this.getCategoriesHandler.handle(activeUser);
      }
    } else {
      throw new MissingApiRequestKeyException(ImmutableList.of(RequestFields.ACTIVE_USER));
    }

    return resultStatus;
  }
}
