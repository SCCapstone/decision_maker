package handlers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import java.util.Arrays;
import java.util.List;
import managers.DbAccessManager;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class MarkEventAsSeenHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;
  private Metrics metrics;

  public MarkEventAsSeenHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  public ResultStatus handle(final String activeUser, final String groupId, final String eventId) {
    final String classMethod = "MarkEventAsSeenHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final String updateExpression =
          "remove " + User.GROUPS + ".#groupId." + User.EVENTS_UNSEEN + ".#eventId";
      final NameMap nameMap = new NameMap().with("#groupId", groupId).with("#eventId", eventId);

      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withNameMap(nameMap);

      this.dbAccessManager.updateUser(activeUser, updateItemSpec);

      resultStatus = ResultStatus.successful("Event marked as seen successfully.");
    } catch (final Exception e) {
      resultStatus = ResultStatus.failure("Exception in: " + classMethod);
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
