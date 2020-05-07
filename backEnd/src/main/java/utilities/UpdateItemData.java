package utilities;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Update;
import managers.DbAccessManager;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import models.Category;
import models.Group;
import models.User;

@Data
public class UpdateItemData {

  private final String keyValue;
  private final String tableName;

  private String updateExpression;
  private ValueMap valueMap;
  private NameMap nameMap;

  public UpdateItemData(final String keyValue, final String tableName) {
    this.keyValue = keyValue;
    this.tableName = tableName;
  }

  public UpdateItemData withUpdateExpression(final String updateExpression) {
    this.updateExpression = updateExpression;
    return this;
  }

  public UpdateItemData withValueMap(final ValueMap valueMap) {
    this.valueMap = valueMap;
    return this;
  }

  public UpdateItemData withNameMap(final NameMap nameMap) {
    this.nameMap = nameMap;
    return this;
  }

  public UpdateItemSpec asUpdateItemSpec() throws Exception {
    final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
        .withPrimaryKey(this.getKeyIndex(), this.keyValue)
        .withUpdateExpression(this.updateExpression);

    if (this.valueMap != null) {
      updateItemSpec.withValueMap(this.valueMap);
    }

    if (this.nameMap != null) {
      updateItemSpec.withNameMap(this.nameMap);
    }

    return updateItemSpec;
  }

  public Update asUpdate() throws Exception {
    final Update update = new Update().withUpdateExpression(this.updateExpression)
        .withTableName(this.tableName).withKey(this.getKeyMap());

    if (this.valueMap != null) {
      for (final String key : this.valueMap.keySet()) {
        update.addExpressionAttributeValuesEntry(key,
            AttributeValueUtils.convertObjectToAttributeValue(valueMap.get(key)));
      }
    }

    if (this.nameMap != null) {
      for (final Map.Entry entry : this.nameMap.entrySet()) {
        update.addExpressionAttributeNamesEntry(entry.getKey().toString(),
            entry.getValue().toString());
      }
    }

    return update;
  }

  private Map<String, AttributeValue> getKeyMap() throws Exception {
    final String keyIndex = this.getKeyIndex();
    final String keyValueCopy = this.keyValue;
    return new HashMap<String, AttributeValue>() {{
      put(keyIndex, new AttributeValue().withS(keyValueCopy));
    }};
  }

  private String getKeyIndex() throws Exception {
    if (this.tableName.equals(DbAccessManager.CATEGORIES_TABLE_NAME)) {
      return Category.CATEGORY_ID;
    } else if (this.tableName.equals(DbAccessManager.USERS_TABLE_NAME)) {
      return User.USERNAME;
    } else if (this.tableName.equals(DbAccessManager.GROUPS_TABLE_NAME)) {
      return Group.GROUP_ID;
    } else {
      throw new Exception("Invalid table name: " + this.tableName);
    }
  }
}
