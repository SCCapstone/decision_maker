package handlers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import com.amazonaws.services.sns.model.InvalidParameterException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import managers.DbAccessManager;
import managers.SnsAccessManager;
import models.User;
import utilities.Config;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class RegisterPushEndpointHandler implements ApiRequestHandler {

  private static final String USER_DATA_KEY = "CustomUserData";

  private final DbAccessManager dbAccessManager;
  private final SnsAccessManager snsAccessManager;
  private final UnregisterPushEndpointHandler unregisterPushEndpointHandler;
  private final Metrics metrics;

  @Inject
  public RegisterPushEndpointHandler(final DbAccessManager dbAccessManager,
      final SnsAccessManager snsAccessManager,
      final UnregisterPushEndpointHandler unregisterPushEndpointHandler, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.snsAccessManager = snsAccessManager;
    this.unregisterPushEndpointHandler = unregisterPushEndpointHandler;
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
      this.attemptToRegisterUserEndpoint(activeUser, deviceToken);
      resultStatus = ResultStatus.successful("User push arn set successfully");
    } catch (final InvalidParameterException ipe) {
      //The error handling here is obtained from aws doc: https://docs.aws.amazon.com/sns/latest/dg/mobile-platform-endpoint.html#mobile-platform-endpoint-sdk-examples
      final String message = ipe.getErrorMessage();
      final Pattern p = Pattern
          .compile(".*Endpoint (arn:aws:sns[^ ]+) already exists with the same [Tt]oken.*");
      final Matcher m = p.matcher(message);
      if (m.matches()) {
        //The platform endpoint already exists for this token

        //We have to get the current user associated with the arn and unsubscribe them
        //Then we have to subscribe the new user
        final String endpointArn = m.group(1);

        final Map<String, String> endpointAttributes = this.snsAccessManager
            .getEndpointAttributes(endpointArn);

        final String oldUsername = endpointAttributes.get(USER_DATA_KEY);

        if (this.unregisterPushEndpointHandler.handle(oldUsername).success) {
          //by the chance the user that owned this had their mapping removed but the endpoint still existed, we do this for sanity
          this.snsAccessManager
              .unregisterPlatformEndpoint(new DeleteEndpointRequest().withEndpointArn(endpointArn));

          this.attemptToRegisterUserEndpoint(activeUser, deviceToken);
          resultStatus = ResultStatus.successful("User push arn set successfully");
        } else {
          this.metrics.logWithBody(
              new ErrorDescriptor<>(classMethod, "Unable to unregister endpoint from other user"));
          resultStatus = ResultStatus.failure("Unable to unregister endpoint from other user");
        }
      } else {
        this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, ipe));
        resultStatus = ResultStatus.failure("InvalidParameterException in " + classMethod);
      }
    } catch (Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private void attemptToRegisterUserEndpoint(final String activeUser,
      final String deviceToken) throws InvalidParameterException {
    //first thing to do is register the device token with SNS
    final CreatePlatformEndpointRequest createPlatformEndpointRequest =
        new CreatePlatformEndpointRequest()
            .withPlatformApplicationArn(Config.PUSH_SNS_PLATFORM_ARN)
            .withToken(deviceToken)
            .withCustomUserData(activeUser);
    final CreatePlatformEndpointResult createPlatformEndpointResult = this.snsAccessManager
        .registerPlatformEndpoint(createPlatformEndpointRequest);

    //this creation will give us a new ARN for the sns endpoint associated with the device token
    final String userEndpointArn = createPlatformEndpointResult.getEndpointArn();

    //we need to register the ARN for the user's device on the user item
    final String updateExpression = "set " + User.PUSH_ENDPOINT_ARN + " = :userEndpointArn";
    final ValueMap valueMap = new ValueMap().withString(":userEndpointArn", userEndpointArn);
    final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
        .withUpdateExpression(updateExpression)
        .withValueMap(valueMap);

    this.dbAccessManager.updateUser(activeUser, updateItemSpec);
  }
}
