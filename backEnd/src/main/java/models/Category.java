package models;

import com.amazonaws.services.dynamodbv2.document.Item;
import imports.CategoriesManager;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class Category {

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
    this.setNextChoiceNo(
        this.getIntFromBigInt((BigDecimal) jsonMap.get(CategoriesManager.NEXT_CHOICE_NO)));
    this.setChoices((Map<String, Object>) jsonMap.get(CategoriesManager.CHOICES));
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
    Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.putIfAbsent(CategoriesManager.CATEGORY_ID, this.getCategoryId());
    modelAsMap.putIfAbsent(CategoriesManager.CATEGORY_NAME, this.getCategoryName());
    modelAsMap.putIfAbsent(CategoriesManager.OWNER, this.getOwner());
    modelAsMap.putIfAbsent(CategoriesManager.NEXT_CHOICE_NO, this.getNextChoiceNo());
    modelAsMap.putIfAbsent(CategoriesManager.CHOICES, this.getChoices());
    modelAsMap.putIfAbsent(CategoriesManager.GROUPS, this.getGroups());
    return modelAsMap;
  }

  public void setChoices(final Map<String, Object> jsonMap) {
    this.choices = null;
    if (jsonMap != null) {
      this.choices = new HashMap<>();
      for (String choiceId : jsonMap.keySet()) {
        this.choices.putIfAbsent(choiceId, (String) jsonMap.get(choiceId));
      }
    }
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

  private Integer getIntFromBigInt(final BigDecimal input) {
    if (input != null) {
      return input.intValue();
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
