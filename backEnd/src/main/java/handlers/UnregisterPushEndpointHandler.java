package handlers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import managers.DbAccessManager;
import managers.SnsAccessManager;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class UnregisterPushEndpointHandler implements ApiRequestHandler {

  private final DbAccessManager dbAccessManager;
  private final SnsAccessManager snsAccessManager;
  private final Metrics metrics;

  public UnregisterPushEndpointHandler(final DbAccessManager dbAccessManager,
      final SnsAccessManager snsAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.snsAccessManager = snsAccessManager;
    this.metrics = metrics;
  }

  /**
   * This function takes in a username and if that user has a push notification associated with
   * their account it gets removed from their user item.
   *
   * @param activeUser The map containing the json request payload sent from the front end.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser) {
    final String classMethod = "UnregisterPushEndpointHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final User user = this.dbAccessManager.getUser(activeUser);

      if (user.pushEndpointArnIsSet()) {
        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withUpdateExpression("remove " + User.PUSH_ENDPOINT_ARN);

        this.dbAccessManager.updateUser(activeUser, updateItemSpec);

        //we've made it here without exception which means the user doesn't have record of the
        //endpoint anymore, now we try to actually delete the arn. If the following fails we're
        //still safe as there's no reference to the arn in the db anymore
        final DeleteEndpointRequest deleteEndpointRequest = new DeleteEndpointRequest()
            .withEndpointArn(user.getPushEndpointArn());
        this.snsAccessManager.unregisterPlatformEndpoint(deleteEndpointRequest);

        resultStatus = ResultStatus.successful("endpoint unregistered");
      } else {
        resultStatus = ResultStatus.successful("no endpoint to unregister");
      }
    } catch (Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
