package models;

import com.amazonaws.services.dynamodbv2.document.Item;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Category implements Model {

  public static final String CATEGORY_ID = "CategoryId";
  public static final String CATEGORY_NAME = "CategoryName";
  public static final String CHOICES = "Choices";
  public static final String GROUPS = "Groups";
  public static final String OWNER = "Owner";

  //NOTICE: Choice ids are the labels! This uniqueness is enforced in add/edit category

  private String categoryId;
  private String categoryName;
  private String owner;
  private Map<String, Integer> choices; // choice label to sort order
  private Map<String, String> groups;

  public Category(final Item categoryItem) {
    this(categoryItem.asMap());
  }

  public Category(final Map<String, Object> jsonMap) {
    this.setCategoryId((String) jsonMap.get(CATEGORY_ID));
    this.setCategoryName((String) jsonMap.get(CATEGORY_NAME));
    this.setOwner((String) jsonMap.get(OWNER));
    this.setChoicesRawMap((Map<String, Object>) jsonMap.get(CHOICES));
    this.setGroupsRawMap((Map<String, Object>) jsonMap.get(GROUPS));
  }

  public Item asItem() {
    final Item modelAsItem = Item.fromMap(this.asMap());

    //change the category id to be the primary key
    modelAsItem.removeAttribute(CATEGORY_ID);
    modelAsItem.withPrimaryKey(CATEGORY_ID, this.getCategoryId());

    return modelAsItem;
  }

  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.putIfAbsent(CATEGORY_ID, this.categoryId);
    modelAsMap.putIfAbsent(CATEGORY_NAME, this.categoryName);
    modelAsMap.putIfAbsent(OWNER, this.owner);
    modelAsMap.putIfAbsent(CHOICES, this.choices);
    modelAsMap.putIfAbsent(GROUPS, this.groups);
    return modelAsMap;
  }

  public void setChoicesRawMap(final Map<String, Object> jsonMap) {
    this.choices = null;
    if (jsonMap != null) {
      this.choices = new HashMap<>();
      for (final String choiceId : jsonMap.keySet()) {
        this.choices.putIfAbsent(choiceId, this.getIntFromObject(jsonMap.get(choiceId)));
      }
    }
  }

  public void setGroupsRawMap(final Map<String, Object> jsonMap) {
    this.groups = null;
    if (jsonMap != null) {
      this.groups = new HashMap<>();
      for (String groupId : jsonMap.keySet()) {
        this.groups.putIfAbsent(groupId, (String) jsonMap.get(groupId));
      }
    }
  }

  private Integer getIntFromObject(final Object input) {
    if (input != null) {
      return Integer.parseInt(input.toString());
    }
    return null;
  }
}
