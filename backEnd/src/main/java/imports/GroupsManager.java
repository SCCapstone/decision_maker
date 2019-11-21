package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import utilities.JsonEncoders;
import utilities.RequestFields;
import utilities.ResultStatus;

public class GroupsManager extends DatabaseAccessManager {

  private UsersManager usersManager = new UsersManager();

  public GroupsManager() {
    super("groups", "GroupId", Regions.US_EAST_2);
  }

  public ResultStatus getGroups(Map<String, Object> jsonMap) {
    boolean success = true;
    String resultMessage = "";
    List<String> groupIds = new ArrayList<String>();

    if (jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
      String username = (String) jsonMap.get(RequestFields.ACTIVE_USER);
      groupIds = this.usersManager.getAllCategoryIds(username);
    } else if (jsonMap.containsKey(RequestFields.CATEGORY_IDS)) {
      groupIds = (List<String>) jsonMap.get(RequestFields.GROUP_IDS);
    } else {
      success = false;
      resultMessage = "Error: query key not defined.";
    }

    // this will be a json string representing an array of objects
    List<Item> groups = new ArrayList<Item>();
    for (String id : groupIds) {
      Item dbData = super.getItem(new GetItemSpec().withPrimaryKey(super.getPrimaryKeyIndex(), id));
      groups.add(dbData);
    }

    if (success) {
      resultMessage = JsonEncoders.convertListToJson(groups);
    }

    return new ResultStatus(success, resultMessage);
  }

  public ResultStatus createNewGroup(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();

    return resultStatus;
  }

  public ResultStatus editGroup(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();

    return resultStatus;
  }
}
