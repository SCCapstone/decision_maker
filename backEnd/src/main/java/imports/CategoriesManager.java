package imports;

import com.amazonaws.regions.Regions;

public class CategoriesManager extends DatabaseAccessManager {
  public CategoriesManager() {
    super("categories", "CategoryId", Regions.US_EAST_2);
  }
}
