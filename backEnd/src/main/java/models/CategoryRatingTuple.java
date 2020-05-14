package models;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryRatingTuple {
  public static final String CATEGORY = "Category";
  public static final String RATINGS = "Ratings";

  private Category category;
  private Map<String, Integer> ratings;

  public Map<String, Object> asMap() {
    final HashMap<String, Object> objectMap = new HashMap<>();
    objectMap.put(CATEGORY, this.category.asMap());
    objectMap.put(RATINGS, this.ratings);
    return objectMap;
  }
}
