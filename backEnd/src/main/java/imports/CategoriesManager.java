package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;

import java.util.Map;


public class CategoriesManager extends DatabaseAccessManager {
    public CategoriesManager() {
        super("categories", "CategoryId", Regions.US_EAST_2);
    }

    public Item getTableData() {
        return super.getItem(
                new GetItemSpec().withPrimaryKey(super.getPrimaryKeyIndex(), "John"));
    }

    public String getJsonStringOfTableDataForFrontEnd() {
        Item dbData = this.getTableData();
        Map<String, Object> dbDataMap = dbData.asMap();

        // this will be a json string representing an array of objects
        StringBuilder outputString = new StringBuilder("[");

        for (String s : dbDataMap.keySet()) {
            outputString.append("{\"key\": \"");
            outputString.append(s);
            outputString.append("\", \"value\": \"");
            outputString.append(dbDataMap.get(s).toString());
            outputString.append("\"},");
        }

        outputString.deleteCharAt(outputString.lastIndexOf(",")); // remove the last comma
        outputString.append("]");

        return outputString.toString();
    }
}
