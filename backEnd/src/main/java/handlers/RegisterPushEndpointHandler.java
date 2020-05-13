package handlers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import managers.DbAccessManager;
import managers.SnsAccessManager;
import models.User;
import utilities.Config;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class RegisterPushEndpointHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;
  private SnsAccessManager snsAccessManager;
  private Metrics metrics;

  public RegisterPushEndpointHandler(final DbAccessManager dbAccessManager,
      final SnsAccessManager snsAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.snsAccessManager = snsAccessManager;
    this.metrics = metrics;
  }

  /**
   * This function takes in a device token registered in google cloud messaging and creates a SNS
   * endpoint for this token and then registers the ARN of the SNS endpoint on the user item.
   *
   * @param activeUser  The user making the api request whos push endpoint is being registered.
   * @param deviceToken This is the GCM token for the user's device that is used to register the
   *                    endpoint.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String deviceToken) {
    final String classMethod = "RegisterPushEndpointHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      //first thing to do is register the device token with SNS
      final CreatePlatformEndpointRequest createPlatformEndpointRequest =
          new CreatePlatformEndpointRequest()
              .withPlatformApplicationArn(Config.PUSH_SNS_PLATFORM_ARN)
              .withToken(deviceToken)
              .withCustomUserData(activeUser);
      final CreatePlatformEndpointResult createPlatformEndpointResult = this.snsAccessManager
          .registerPlatformEndpoint(createPlatformEndpointRequest, metrics);

      //this creation will give us a new ARN for the sns endpoint associated with the device token
      final String userEndpointArn = createPlatformEndpointResult.getEndpointArn();

      //we need to register the ARN for the user's device on the user item
      final String updateExpression = "set " + User.PUSH_ENDPOINT_ARN + " = :userEndpointArn";
      final ValueMap valueMap = new ValueMap().withString(":userEndpointArn", userEndpointArn);
      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withValueMap(valueMap);

      this.dbAccessManager.updateUser(activeUser, updateItemSpec);
      resultStatus = ResultStatus.successful("User push arn set successfully");
    } catch (Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
