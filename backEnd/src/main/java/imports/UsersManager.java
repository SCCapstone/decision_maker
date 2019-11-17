package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;

import java.util.ArrayList;
import java.util.Map;

public class UsersManager extends DatabaseAccessManager {

  public static final String CATEGORY_FIELD = "Categories";

  public UsersManager() {
    super("users", "Username", Regions.US_EAST_2);
  }

  public ArrayList<String> getAllCategoryIds(String username) {
    Item dbData = super
        .getItem(new GetItemSpec().withPrimaryKey(super.getPrimaryKeyIndex(), username));
    Map<String, Object> dbDataMap = dbData.asMap(); // specific user record as a map
    Map<String, String> categoryMap = (Map<String, String>) dbDataMap.get(CATEGORY_FIELD);
    ArrayList<String> ids = new ArrayList<String>(categoryMap.keySet());
    return ids;
  }
}
