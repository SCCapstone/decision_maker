package dbMaintenance.handlers;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import dbMaintenance.managers.MaintenanceDbAccessManager;
import handlers.ApiRequestHandler;
import java.util.Iterator;
import javax.inject.Inject;
import models.AppSettings;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class AddDurationsToUserSettingsHandler implements ApiRequestHandler {

  private MaintenanceDbAccessManager maintenanceDbAccessManager;
  private Metrics metrics;

  @Inject
  public AddDurationsToUserSettingsHandler(
      final MaintenanceDbAccessManager maintenanceDbAccessManager, final Metrics metrics) {
    this.maintenanceDbAccessManager = maintenanceDbAccessManager;
    this.metrics = metrics;
  }

  public ResultStatus handle() {
    final String classMethod = "AddDurationsToUserSettingsHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus = ResultStatus
        .successful("Durations added to users' app settings successfully.");

    try {
      final String updateExpression =
          "set " + User.APP_SETTINGS + "." + AppSettings.DEFAULT_RSVP_DURATION
              + " = :defaultRsvpDuration, " + User.APP_SETTINGS + "."
              + AppSettings.DEFAULT_VOTING_DURATION + " = :defaultVotingDuration";
      final ValueMap valueMap = new ValueMap().withNumber(":defaultRsvpDuration", 10)
          .withNumber(":defaultVotingDuration", 10); // 10 is default in app settings
      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withValueMap(valueMap);

      final Iterator<Item> tableItems = this.maintenanceDbAccessManager.scanUsersTable();

      while (tableItems.hasNext()) {
        final Item userItem = tableItems.next();

        try {
          this.maintenanceDbAccessManager.updateUser(userItem.getString(User.USERNAME), updateItemSpec);
        } catch (final Exception e) {
          this.metrics.log(new ErrorDescriptor<>(userItem.get(User.USERNAME), classMethod, e));
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
