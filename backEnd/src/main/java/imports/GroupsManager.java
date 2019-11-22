package imports;

import com.amazonaws.regions.Regions;

public class GroupsManager extends DatabaseAccessManager {

  public GroupsManager() {
    super("groups", "GroupId", Regions.US_EAST_2);
  }
}
