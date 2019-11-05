package imports;

import com.amazonaws.regions.Regions;

public class UsersManager extends DatabaseAccessManager {
  public UsersManager() {
    super("users", "Username", Regions.US_EAST_2);
  }
}
