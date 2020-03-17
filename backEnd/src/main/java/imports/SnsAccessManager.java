package imports;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import com.amazonaws.services.sns.model.DeleteEndpointResult;
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.GetEndpointAttributesResult;
import com.amazonaws.services.sns.model.GetPlatformApplicationAttributesRequest;
import com.amazonaws.services.sns.model.GetPlatformApplicationAttributesResult;
import com.amazonaws.services.sns.model.InvalidParameterException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.google.common.collect.ImmutableMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class SnsAccessManager {

  private final Regions region = Regions.US_EAST_1; //TODO migrate everything to us east 1 and move this to config
  private static final String USER_DATA_KEY = "CustomUserData";

  private AmazonSNSClient client = (AmazonSNSClient) AmazonSNSClient.builder()
      .withRegion(this.region)
      .withCredentials(new EnvironmentVariableCredentialsProvider())
      .build();

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

  public DeleteEndpointResult unregisterPlatformEndpoint(
      final DeleteEndpointRequest deleteEndpointRequest) {
    return this.client.deleteEndpoint(deleteEndpointRequest);
  }

  public PublishResult sendMessage(final String arn, final String message) {
    PublishRequest publishRequest = new PublishRequest()
        .withTargetArn(arn)
        .withMessage(message);
    return this.client.publish(publishRequest);
  }

  public GetPlatformApplicationAttributesResult getPlatformAttributes(final String platformArn) {
    return this.client
        .getPlatformApplicationAttributes(
            new GetPlatformApplicationAttributesRequest().withPlatformApplicationArn(platformArn));
  }
}
