package DbMaintenance.Managers;

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
}
