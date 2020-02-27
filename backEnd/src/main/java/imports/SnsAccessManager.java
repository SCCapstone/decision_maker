package imports;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import utilities.Config;

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
}
