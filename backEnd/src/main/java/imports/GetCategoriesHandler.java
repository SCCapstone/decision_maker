package imports;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import models.Group;
import models.User;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class GetCategoriesHandler extends ApiRequestHandler {

  public GetCategoriesHandler(final DbAccessManager dbAccessManager,
      final Map<String, Object> requestBody, final Metrics metrics) {
    super(dbAccessManager, requestBody, metrics);
  }

  @Override
  public ResultStatus handle() {
    final String classMethod = "GetCategoriesHandler.handle";
    this.metrics.commonSetup(classMethod);

    boolean success = true;
    String resultMessage = "";
    List<String> categoryIds = new ArrayList<>();

    try {
      //notice, due to how the ActiveUser key is set for every call, it's check must be last!
      if (this.requestBody.containsKey(RequestFields.CATEGORY_IDS)) {
        categoryIds = (List<String>) this.requestBody.get(RequestFields.CATEGORY_IDS);
      } else if (this.requestBody.containsKey(GroupsManager.GROUP_ID)) {
        final String groupId = (String) this.requestBody.get(Group.GROUP_ID);
        final Group group = this.dbAccessManager.getGroup(groupId);
        categoryIds = new ArrayList<>(group.getCategories().keySet());
      } else if (this.requestBody.containsKey(RequestFields.ACTIVE_USER)) {
        final String activeUser = (String) this.requestBody.get(RequestFields.ACTIVE_USER);
        final User user = this.dbAccessManager.getUser(activeUser);
        //final List<String> groupIds = new ArrayList<>(user.getGroups().keySet());

        //we're going to get the user's owned categories along with the categories the user has
        //access to via their groups
        categoryIds = new ArrayList<>(user.getOwnedCategories().keySet());
//      for (String groupId : groupIds) {
//        final Group group = this.dbAccessManager.getGroup(groupId);
//        categoryIds.addAll(new ArrayList<>(group.getCategories().keySet()));
//      }
      } else {
        success = false;
        resultMessage = "Error: query key not defined.";
        this.metrics.log(new ErrorDescriptor<>(this.requestBody, classMethod,
            "lookup key not in request payload/active user not set"));
      }
    } catch (final Exception e) {
      success = false;
      resultMessage = "Error: exception in " + classMethod;
      this.metrics.log(new ErrorDescriptor<>(this.requestBody, classMethod, e));
    }

    if (success) {
      //remove duplicates from categoryIds
      Set<String> uniqueCategoryIds = new LinkedHashSet<>(categoryIds);

      List<Map> categories = new ArrayList<>();
      for (String id : uniqueCategoryIds) {
        try {
          categories.add(this.dbAccessManager.getCategoryMap(id));
        } catch (Exception e) {
          this.metrics.log(new ErrorDescriptor<>(this.requestBody, classMethod, e));
        }
      }

      resultMessage = JsonUtils.convertIterableToJson(categories);
    }

    this.metrics.commonClose(success);
    return new ResultStatus(success, resultMessage);
  }
}
