package handlers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import java.util.Collections;
import managers.DbAccessManager;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class MarkAllEventsSeenHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;
  private Metrics metrics;

  public MarkAllEventsSeenHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  public ResultStatus handle(final String activeUser, final String groupId) {
    final String classMethod = "MarkAllEventsSeenHandler.handle";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      //assume the user has this group mapping, otherwise this call shouldn't have been made

      final String updateExpression =
          "set " + User.GROUPS + ".#groupId." + User.EVENTS_UNSEEN + " = :empty";
      final ValueMap valueMap = new ValueMap().withMap(":empty", Collections.emptyMap());
      final NameMap nameMap = new NameMap().with("#groupId", groupId);

      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withNameMap(nameMap)
          .withValueMap(valueMap);

      this.dbAccessManager.updateUser(activeUser, updateItemSpec);

      resultStatus = ResultStatus.successful("All events marked seen successfully.");
    } catch (final Exception e) {
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
      metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
