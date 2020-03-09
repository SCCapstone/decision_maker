package models;

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
}
