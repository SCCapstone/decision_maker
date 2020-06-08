package dbMaintenance.handlers;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import dbMaintenance.managers.MaintenanceDbAccessManager;
import handlers.ApiRequestHandler;
import java.util.Iterator;
import javax.inject.Inject;
import models.AppSettings;
import models.Group;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class AddOwnedGroupsCountHandler implements ApiRequestHandler {

  private MaintenanceDbAccessManager maintenanceDbAccessManager;
  private Metrics metrics;

  @Inject
  public AddOwnedGroupsCountHandler(final MaintenanceDbAccessManager maintenanceDbAccessManager,
      final Metrics metrics) {
    this.maintenanceDbAccessManager = maintenanceDbAccessManager;
    this.metrics = metrics;
  }

  public ResultStatus handle() {
    final String classMethod = "AddOwnedGroupsCountHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus = ResultStatus
        .successful("Owned groups count added to users' successfully.");

    try {
      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression("set " + User.OWNED_GROUPS_COUNT + " = :val");

      final Iterator<Item> tableItems = this.maintenanceDbAccessManager.scanUsersTable();

      while (tableItems.hasNext()) {
        final User user = new User(tableItems.next());

        try {
          Group group;
          int ownedGroupsCount = 0;
          for (final String groupId : user.getGroups().keySet()) {
            group = this.maintenanceDbAccessManager.getGroup(groupId);
            if (group.getGroupCreator().equals(user.getUsername())) {
              ownedGroupsCount++;
            }
          }

          updateItemSpec.withValueMap(new ValueMap().withNumber(":val", ownedGroupsCount));

          this.maintenanceDbAccessManager.updateUser(user.getUsername(), updateItemSpec);
        } catch (final Exception e) {
          this.metrics.log(new ErrorDescriptor<>(user.getUsername(), classMethod, e));
          resultStatus = ResultStatus.failure("Exception in " + classMethod);
        }
      }
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
