package dbMaintenance.managers;

import com.amazonaws.services.dynamodbv2.document.Item;
import java.util.Iterator;
import managers.DbAccessManager;

public class MaintenanceDbAccessManager extends DbAccessManager {

  public MaintenanceDbAccessManager() {
    super();
  }

  public Iterator<Item> scanUsersTable() {
    return this.usersTable.scan().iterator();
  }

  public Iterator<Item> scanGroupsTable() {
    return this.groupsTable.scan().iterator();
  }

  public Iterator<Item> scanPendingEventsTable() {
    return this.pendingEventsTable.scan().iterator();
  }
}
