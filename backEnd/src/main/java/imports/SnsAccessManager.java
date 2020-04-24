package imports;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import com.amazonaws.services.sns.model.DeleteEndpointResult;
import com.amazonaws.services.sns.model.EndpointDisabledException;
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.GetEndpointAttributesResult;
import com.amazonaws.services.sns.model.GetPlatformApplicationAttributesRequest;
import com.amazonaws.services.sns.model.GetPlatformApplicationAttributesResult;
import com.amazonaws.services.sns.model.InvalidParameterException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import models.Metadata;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class SnsAccessManager {

  private final Regions region = Regions.US_EAST_1; //TODO migrate everything to us east 1 and move this to config
  private static final String USER_DATA_KEY = "CustomUserData";

  private AmazonSNSClient client;

  public SnsAccessManager() {
    this.client = (AmazonSNSClient) AmazonSNSClient.builder()
        .withRegion(this.region)
        .withCredentials(new EnvironmentVariableCredentialsProvider())
        .build();
  }

  public SnsAccessManager(final AmazonSNSClient amazonSnsClient) {
    this.client = amazonSnsClient;
  }

  /**
   * This method is used to create a new platform endpoint to be used for SNS.
   *
   * @param createPlatformEndpointRequest A request containing the details of the platform endpoint
   *                                      that is to be created.
   * @param metrics                       Standard metrics object for profiling and logging. In this
   *                                      method, we just need the metrics object when calling
   *                                      UsersManager.unregisterPushEndpoint().
   */
  public CreatePlatformEndpointResult registerPlatformEndpoint(
      final CreatePlatformEndpointRequest createPlatformEndpointRequest, final Metrics metrics) {
    CreatePlatformEndpointResult createPlatformEndpointResult = null;

    //The error handling here is obtained from aws doc: https://docs.aws.amazon.com/sns/latest/dg/mobile-platform-endpoint.html#mobile-platform-endpoint-sdk-examples
    try {
      createPlatformEndpointResult = client.createPlatformEndpoint(createPlatformEndpointRequest);
    } catch (final InvalidParameterException ipe) {
      final String message = ipe.getErrorMessage();
      final Pattern p = Pattern
          .compile(".*Endpoint (arn:aws:sns[^ ]+) already exists with the same [Tt]oken.*");
      final Matcher m = p.matcher(message);
      if (m.matches()) {
        //The platform endpoint already exists for this token

        //We have to get the current user associated with the arn and unsubscribe them
        //Then we have to subscribe the new user
        final String endpointArn = m.group(1);

        final GetEndpointAttributesRequest getEndpointAttributesRequest = new GetEndpointAttributesRequest()
            .withEndpointArn(endpointArn);
        final GetEndpointAttributesResult getEndpointAttributesResult = this.client
            .getEndpointAttributes(getEndpointAttributesRequest);

        final String oldUsername = getEndpointAttributesResult.getAttributes()
            .get(USER_DATA_KEY);

        ResultStatus oldEndpointUnregistered = DatabaseManagers.USERS_MANAGER
            .unregisterPushEndpoint(
                ImmutableMap.of(
                    RequestFields.ACTIVE_USER, oldUsername,
                    RequestFields.DEVICE_TOKEN, createPlatformEndpointRequest.getToken()
                ), metrics
            );

        if (oldEndpointUnregistered.success) {
          //by the chance the user that owned this had their mapping removed but the endpoint still existed, we do this for sanity
          this.unregisterPlatformEndpoint(new DeleteEndpointRequest().withEndpointArn(endpointArn));

          createPlatformEndpointResult = this.client
              .createPlatformEndpoint(createPlatformEndpointRequest);
        } else {
          //couldn't unregister the old user...
          throw ipe;
        }
      } else {
        // Rethrow the exception, the input is actually bad.
        throw ipe;
      }
    } catch (Exception e) {
      throw e;
    }

    return createPlatformEndpointResult;
  }

  /**
   * This method is used to delete platform endpoints that are no longer being used.
   *
   * @param deleteEndpointRequest A request containing the details of the endpoint to be deleted.
   */
  public DeleteEndpointResult unregisterPlatformEndpoint(
      final DeleteEndpointRequest deleteEndpointRequest) throws InvalidParameterException {
    return this.client.deleteEndpoint(deleteEndpointRequest);
  }

  /**
   * This method is used to send a message to a user such that the notification will not pop up for
   * that user.
   *
   * @param arn      The arn of the target of this message.
   * @param metadata This contains the action and payload information to be used by the front end.
   */
  //to allow the notification to get sent without popping up, just don't add the notification
  public PublishResult sendMutedMessage(final String arn, final Metadata metadata) {
    Map<String, Object> notification = ImmutableMap.of(
        "data", ImmutableMap.of(
            "click_action", "FLUTTER_NOTIFICATION_CLICK",
            "default", "default message",
            "metadata", metadata.asMap()
        )
    );

    final String jsonNotification =
        "{\"GCM\": \"" + JsonUtils.convertObjectToJson(notification) + "\"}";

    final PublishRequest publishRequest = new PublishRequest()
        .withTargetArn(arn)
        .withMessage(jsonNotification);
    publishRequest.setMessageStructure("json");

    PublishResult publishResult;
    try {
      publishResult = this.client.publish(publishRequest);
    } catch (final EndpointDisabledException ede) {
      //this isn't an error on our end, read more about this exception here:
      //https://forums.aws.amazon.com/thread.jspa?threadID=174551
      publishResult = new PublishResult();
    }

    return publishResult;
  }

  /**
   * This method is used to send a message to a user such that they will see a notification.
   *
   * @param arn      The arn of the target of this message.
   * @param title    The title of the notification.
   * @param body     The body of the notification.
   * @param tag      The tag to be attached to the notification. The tag stops multiple messages
   *                 about the same subject from appearing on the user's device. For example, a
   *                 second message about a specific event will replace the first message.
   * @param metadata This contains the action and payload information to be used by the front end.
   */
  public PublishResult sendMessage(final String arn, final String title, final String body,
      final String tag, final Metadata metadata) {
    Map<String, Object> notification = ImmutableMap.of(
        "notification", ImmutableMap.of(
            "title", title,
            "body", body,
            "tag", tag
        ),
        "data", ImmutableMap.of(
            "click_action", "FLUTTER_NOTIFICATION_CLICK",
            "default", "default message",
            "metadata", metadata.asMap()
        )
    );

    final String jsonNotification =
        "{\"GCM\": \"" + JsonUtils.convertObjectToJson(notification) + "\"}";

    final PublishRequest publishRequest = new PublishRequest()
        .withTargetArn(arn)
        .withMessage(jsonNotification);
    publishRequest.setMessageStructure("json");

    PublishResult publishResult;
    try {
      publishResult = this.client.publish(publishRequest);
    } catch (final EndpointDisabledException ede) {
      //this isn't an error on our end, read more about this exception here:
      //https://forums.aws.amazon.com/thread.jspa?threadID=174551
      publishResult = new PublishResult();
    }

    return publishResult;
  }

  /**
   * This method is used to fetch attributes when given a target arn.
   *
   * @param platformArn The arn of the platform that we want to fetch the attributes of.
   */
  public GetPlatformApplicationAttributesResult getPlatformAttributes(final String platformArn) {
    return this.client
        .getPlatformApplicationAttributes(
            new GetPlatformApplicationAttributesRequest().withPlatformApplicationArn(platformArn));
  }
}
