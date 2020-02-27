package imports;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.SubscribeResult;

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

  public PublishResult sendMessage(final String arn, final String message) {
    PublishRequest publishRequest = new PublishRequest()
        .withTargetArn(arn)
        .withMessage(message);
    return this.client.publish(publishRequest);
  }

//  public SubscribeResult subscribeDeviceToTopic(final String endpointArn) {
//    return this.client.subscribe("asdf", "application", endpointArn);
//  }
}
