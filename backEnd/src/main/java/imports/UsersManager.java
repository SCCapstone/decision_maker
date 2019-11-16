package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;

import java.util.ArrayList;
import java.util.Map;

public class UsersManager extends DatabaseAccessManager {
    public UsersManager() {
        super("users", "Username", Regions.US_EAST_2);
    }

    public ArrayList<String> getAllCategoryIds(String username) {
        Item dbData = super.getItem(new GetItemSpec().withPrimaryKey(super.getPrimaryKeyIndex(), username));
        Map<String, Object> dbDataMap = dbData.asMap(); // specific user record as a map
        ArrayList<String> ids = new ArrayList<String>();

        return ids;

    }
}
