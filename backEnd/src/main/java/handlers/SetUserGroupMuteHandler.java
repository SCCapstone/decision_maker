package handlers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import managers.DbAccessManager;
import models.AppSettings;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class SetUserGroupMuteHandler implements ApiRequestHandler {

  private final DbAccessManager dbAccessManager;
  private final Metrics metrics;

  public SetUserGroupMuteHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  public ResultStatus handle(final String activeUser, final String groupId, final Boolean isMuted) {
    final String classMethod = "SetUserGroupMuteHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final String updateExpression =
          "set " + User.GROUPS + ".#groupId." + AppSettings.MUTED + " = :mute";
      final NameMap nameMap = new NameMap().with("#groupId", groupId);
      final ValueMap valueMap = new ValueMap().withBoolean(":mute", isMuted);

      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withNameMap(nameMap)
          .withValueMap(valueMap);

      this.dbAccessManager.updateUser(activeUser, updateItemSpec);

      resultStatus = ResultStatus.successful("User group mute set successfully.");
    } catch (final Exception e) {
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
