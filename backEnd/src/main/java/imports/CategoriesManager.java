package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import utilities.JsonParsers;
import utilities.ResultStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CategoriesManager extends DatabaseAccessManager {
  public static final String CATEGORY_FIELD_CATEGORY_ID = "CategoryId";
  public static final String CATEGORY_FIELD_CATEGORY_NAME = "CategoryName";
  public static final String CATEGORY_FIELD_CHOICES = "Choices";
  public static final String CATEGORY_FIELD_GROUPS = "Groups";
  public static final String CATEGORY_FIELD_NEXT_CHOICE = "NextChoiceNo";
  public static final String CATEGORY_FIELD_OWNER = "Owner";

  private UUID uuid;

  public CategoriesManager() {
    super("categories", "CategoryId", Regions.US_EAST_2);
  }

  public ResultStatus addNewCategory(Map<String, Object> jsonMap) {
    //validate data, log results as there should be some validation already on the front end
    ResultStatus resultStatus = new ResultStatus();
    if (
        jsonMap.containsKey(CATEGORY_FIELD_CATEGORY_NAME) &&
        jsonMap.containsKey(CATEGORY_FIELD_CHOICES) &&
        jsonMap.containsKey(CATEGORY_FIELD_OWNER)
    ) {
      this.uuid = UUID.randomUUID();

      try {
        String nextCategoryIndex = this.uuid.toString();
        String categoryName = (String) jsonMap.get(CATEGORY_FIELD_CATEGORY_NAME);
        Map<String, Object> choices = (Map<String, Object>) jsonMap.get(CATEGORY_FIELD_CHOICES);
        Map<String, Object> groups = new HashMap<String, Object>();
        int nextChoiceNo = choices.size();
        String owner = (String) jsonMap.get(CATEGORY_FIELD_OWNER);

        Item newCategory = new Item()
            .withPrimaryKey(CATEGORY_FIELD_CATEGORY_ID, nextCategoryIndex)
            .withString(CATEGORY_FIELD_CATEGORY_NAME, categoryName)
            .withMap(CATEGORY_FIELD_CHOICES, choices)
            .withMap(CATEGORY_FIELD_GROUPS, groups)
            .withInt(CATEGORY_FIELD_NEXT_CHOICE, nextChoiceNo)
            .withString(CATEGORY_FIELD_OWNER,owner);

        PutItemSpec putItemSpec = new PutItemSpec()
            .withItem(newCategory);

        super.putItem(putItemSpec);

        resultStatus = new ResultStatus(true, "Category created successfully!");
      } catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request";
      }
    } else {
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    return resultStatus;
  }

  public ResultStatus editCategory(Map<String, Object> jsonMap) {
    //validate data, log results as there should be some validation already on the front end
    return new ResultStatus();
  }
}
