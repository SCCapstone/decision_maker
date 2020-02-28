package imports;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import com.amazonaws.services.sns.model.DeleteEndpointResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

public class SnsAccessManager {

  private final Regions region = Regions.US_EAST_1;

  private AmazonSNSClient client = (AmazonSNSClient) AmazonSNSClient.builder()
      .withRegion(this.region)
      .withCredentials(new EnvironmentVariableCredentialsProvider())
      .build();

  public CreatePlatformEndpointResult registerPlatformEndpoint(
      final CreatePlatformEndpointRequest createPlatformEndpointRequest) {
    return client.createPlatformEndpoint(createPlatformEndpointRequest);
  }

  public DeleteEndpointResult unregisterPlatformEndpoint(
      final DeleteEndpointRequest deleteEndpointRequest) {
    return client.deleteEndpoint(deleteEndpointRequest);
  }

  public PublishResult sendMessage(final String arn, final String message) {
    PublishRequest publishRequest = new PublishRequest()
        .withTargetArn(arn)
        .withMessage(message);
    return this.client.publish(publishRequest);
  }
}
