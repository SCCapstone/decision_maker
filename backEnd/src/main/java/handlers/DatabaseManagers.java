package handlers;

import managers.S3AccessManager;
import managers.SnsAccessManager;

public class DatabaseManagers {
  public static UsersManager USERS_MANAGER = new UsersManager();
  public static GroupsManager GROUPS_MANAGER = new GroupsManager();
  public static PendingEventsManager PENDING_EVENTS_MANAGER = new PendingEventsManager();
  public static S3AccessManager S3_ACCESS_MANAGER = new S3AccessManager();
  public static SnsAccessManager SNS_ACCESS_MANAGER = new SnsAccessManager();
}
