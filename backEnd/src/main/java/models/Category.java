package models;

import com.amazonaws.services.dynamodbv2.document.Item;
import imports.CategoriesManager;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class Category implements Model {

  private String categoryId;
  private String categoryName;
  private String owner;
  private Integer nextChoiceNo;

  @Setter(AccessLevel.NONE)
  private Map<String, String> choices;
  @Setter(AccessLevel.NONE)
  private Map<String, String> groups;

  public Category(final Map<String, Object> jsonMap) {
    this.setCategoryId((String) jsonMap.get(CategoriesManager.CATEGORY_ID));
    this.setCategoryName((String) jsonMap.get(CategoriesManager.CATEGORY_NAME));
    this.setOwner((String) jsonMap.get(CategoriesManager.OWNER));
    this.setNextChoiceNo(this.getIntFromObject(jsonMap.get(CategoriesManager.NEXT_CHOICE_NO)));
    this.setChoicesRawMap((Map<String, Object>) jsonMap.get(CategoriesManager.CHOICES));
    this.setGroups((Map<String, Object>) jsonMap.get(CategoriesManager.GROUPS));
  }

  public Item asItem() {
    Item modelAsItem = Item.fromMap(this.asMap());

    //change the category id to be the primary key
    modelAsItem.removeAttribute(CategoriesManager.CATEGORY_ID);
    modelAsItem.withPrimaryKey(CategoriesManager.CATEGORY_ID, this.getCategoryId());

    return modelAsItem;
  }

  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.putIfAbsent(CategoriesManager.CATEGORY_ID, this.categoryId);
    modelAsMap.putIfAbsent(CategoriesManager.CATEGORY_NAME, this.categoryName);
    modelAsMap.putIfAbsent(CategoriesManager.OWNER, this.owner);
    modelAsMap.putIfAbsent(CategoriesManager.NEXT_CHOICE_NO, this.nextChoiceNo);
    modelAsMap.putIfAbsent(CategoriesManager.CHOICES, this.choices);
    modelAsMap.putIfAbsent(CategoriesManager.GROUPS, this.groups);
    return modelAsMap;
  }

  public void setChoicesRawMap(final Map<String, Object> jsonMap) {
    this.choices = null;
    if (jsonMap != null) {
      this.choices = new HashMap<>();
      for (String choiceId : jsonMap.keySet()) {
        this.choices.putIfAbsent(choiceId, (String) jsonMap.get(choiceId));
      }
    }
  }

  public void setChoices(final Map<String, String> choices) {
    this.choices = choices;
  }

  public void setGroups(final Map<String, Object> jsonMap) {
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

  public void updateNextChoiceNo() {
    if (this.choices != null) {
      int nextChoiceNo = -1;

      //get the max current choiceNo
      for (String choiceNo : this.choices.keySet()) {
        if (Integer.parseInt(choiceNo) > nextChoiceNo) {
          nextChoiceNo = Integer.parseInt(choiceNo);
        }
      }

      //move the next choice to be the next value up from the max
      this.nextChoiceNo = nextChoiceNo + 1;
    }
  }
}
