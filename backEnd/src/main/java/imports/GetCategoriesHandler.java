package imports;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

@AllArgsConstructor
public class GetCategoriesHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;

  public ResultStatus handle(final Map<String, Object> jsonMap, final Metrics metrics) {
    final String classMethod = "GetCategoriesHandler.handle";
    metrics.commonSetup(classMethod);

    boolean success = true;
    String resultMessage = "";
    List<String> categoryIds = new ArrayList<>();

    //notice, due to how the ActiveUser key is set for every call, it's check must be last!
    if (jsonMap.containsKey(RequestFields.CATEGORY_IDS)) {
      categoryIds = (List<String>) jsonMap.get(RequestFields.CATEGORY_IDS);
    } else if (jsonMap.containsKey(GroupsManager.GROUP_ID)) {
      String groupId = (String) jsonMap.get(DatabaseManagers.GROUPS_MANAGER.getPrimaryKeyIndex());
      categoryIds = DatabaseManagers.GROUPS_MANAGER.getAllCategoryIds(groupId, metrics);
    } else if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      String username = (String) jsonMap.get(RequestFields.ACTIVE_USER);
      categoryIds = DatabaseManagers.USERS_MANAGER.getAllOwnedCategoryIds(username, metrics);
      List<String> groupIds = DatabaseManagers.USERS_MANAGER.getAllGroupIds(username, metrics);

      for (String groupId : groupIds) {
        List<String> groupCategoryIds = DatabaseManagers.GROUPS_MANAGER
            .getAllCategoryIds(groupId, metrics);
        categoryIds.addAll(groupCategoryIds);
      }
    } else {
      success = false;
      resultMessage = "Error: query key not defined.";
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod,
          "lookup key not in request payload/active user not set"));
    }

    if (success) {
      //remove duplicates from categoryIds
      Set<String> uniqueCategoryIds = new LinkedHashSet<>(categoryIds);

      List<Map> categories = new ArrayList<>();
      for (String id : uniqueCategoryIds) {
        try {
          categories.add(this.dbAccessManager.getCategoryMap(id));
        } catch (Exception e) {
          metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        }
      }

      resultMessage = JsonUtils.convertIterableToJson(categories);
    }

    metrics.commonClose(success);

    return new ResultStatus(success, resultMessage);
  }
}
