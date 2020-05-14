//package managers;
//
//import static junit.framework.TestCase.fail;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.doReturn;
//import static org.mockito.Mockito.doThrow;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import com.amazonaws.services.sns.AmazonSNSClient;
//import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
//import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
//import com.amazonaws.services.sns.model.DeleteEndpointRequest;
//import com.amazonaws.services.sns.model.DeleteEndpointResult;
//import com.amazonaws.services.sns.model.EndpointDisabledException;
//import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
//import com.amazonaws.services.sns.model.GetEndpointAttributesResult;
//import com.amazonaws.services.sns.model.GetPlatformApplicationAttributesRequest;
//import com.amazonaws.services.sns.model.GetPlatformApplicationAttributesResult;
//import com.amazonaws.services.sns.model.InternalErrorException;
//import com.amazonaws.services.sns.model.InvalidParameterException;
//import com.amazonaws.services.sns.model.PublishRequest;
//import com.amazonaws.services.sns.model.PublishResult;
//import com.google.common.collect.ImmutableMap;
//import handlers.DatabaseManagers;
//import handlers.UsersManager;
//import java.util.Map;
//import managers.SnsAccessManager;
//import models.Metadata;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.platform.runner.JUnitPlatform;
//import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import utilities.Metrics;
//import utilities.ResultStatus;
//
//@ExtendWith(MockitoExtension.class)
//@RunWith(JUnitPlatform.class)
//public class SnsAccessManagerTest {
//
//  private SnsAccessManager snsAccessManager;
//
//  private final InvalidParameterException invalidParameterException =
//      new InvalidParameterException("Endpoint arn:aws:sns1000 already exists with the same Token.");
//
//  @Mock
//  private AmazonSNSClient snsClient;
//
//  @Mock
//  private Metrics metrics;
//
//  @BeforeEach
//  private void init() {
//    this.snsAccessManager = new SnsAccessManager(this.snsClient);
//  }
//
//  ////////////////////////////////////
//  // registerPlatformEndpoint tests //
//  ////////////////////////////////////region
//
//  @Test
//  public void registerPlatformEndpoint_validInput_successfulResult() {
//    doReturn(new CreatePlatformEndpointResult()).when(this.snsClient).createPlatformEndpoint(any(
//        CreatePlatformEndpointRequest.class));
//
//    CreatePlatformEndpointResult createPlatformEndpointResult =
//        this.snsAccessManager.registerPlatformEndpoint(new CreatePlatformEndpointRequest());
//
//    verify(this.snsClient, times(1))
//        .createPlatformEndpoint(any(CreatePlatformEndpointRequest.class));
//    verify(this.snsClient, times(0))
//        .getEndpointAttributes(any(GetEndpointAttributesRequest.class));
//  }
//
//  @Test
//  public void registerPlatformEndpoint_invalidParameterException_successfulResult() {
//    when(this.snsClient.createPlatformEndpoint(any(CreatePlatformEndpointRequest.class)))
//        .thenThrow(this.invalidParameterException)
//        .thenReturn(new CreatePlatformEndpointResult());
//    doReturn(new GetEndpointAttributesResult()
//        .withAttributes(ImmutableMap.of("CustomUserData", "john_andrews12"))).when(this.snsClient)
//        .getEndpointAttributes(any(GetEndpointAttributesRequest.class));
//
//    try {
//      CreatePlatformEndpointResult createPlatformEndpointResult =
//          this.snsAccessManager
//              .registerPlatformEndpoint(new CreatePlatformEndpointRequest().withToken("token"));
//      verify(this.snsClient, times(2))
//          .createPlatformEndpoint(any(CreatePlatformEndpointRequest.class));
//      verify(this.snsClient, times(1))
//          .getEndpointAttributes(any(GetEndpointAttributesRequest.class));
//    } catch (Exception e) {
//      System.out.println(e);
//      fail();
//    }
//  }
//
//  @Test
//  public void registerPlatformEndpoint_invalidParameterExceptionCannotUnregisterOldEndpoint_failureResult() {
//    doThrow(this.invalidParameterException).when(this.snsClient)
//        .createPlatformEndpoint(any(CreatePlatformEndpointRequest.class));
//    doReturn(new GetEndpointAttributesResult()
//        .withAttributes(ImmutableMap.of("CustomUserData", "john_andrews12"))).when(this.snsClient)
//        .getEndpointAttributes(any(GetEndpointAttributesRequest.class));
//    doReturn(new ResultStatus(false, "old endpoint not unregistered")).when(this.usersManager)
//        .unregisterPushEndpoint(any(Map.class), any(Metrics.class));
//
//    try {
//      CreatePlatformEndpointResult createPlatformEndpointResult =
//          this.snsAccessManager
//              .registerPlatformEndpoint(new CreatePlatformEndpointRequest().withToken("token"),
//                  this.metrics);
//    } catch (InvalidParameterException ipe) {
//      verify(this.snsClient, times(1))
//          .createPlatformEndpoint(any(CreatePlatformEndpointRequest.class));
//      verify(this.snsClient, times(1))
//          .getEndpointAttributes(any(GetEndpointAttributesRequest.class));
//    }
//  }
//
//  @Test
//  public void registerPlatformEndpoint_invalidParameterExceptionBadInput_failureResult() {
//    doThrow(new InvalidParameterException("bad input")).when(this.snsClient)
//        .createPlatformEndpoint(any(CreatePlatformEndpointRequest.class));
//
//    try {
//      CreatePlatformEndpointResult createPlatformEndpointResult =
//          this.snsAccessManager
//              .registerPlatformEndpoint(new CreatePlatformEndpointRequest(), this.metrics);
//    } catch (InvalidParameterException ipe) {
//      verify(this.snsClient, times(1))
//          .createPlatformEndpoint(any(CreatePlatformEndpointRequest.class));
//      verify(this.snsClient, times(0))
//          .getEndpointAttributes(any(GetEndpointAttributesRequest.class));
//    }
//  }
//
//  @Test
//  public void registerPlatformEndpoint_internalError_failureResult() {
//    doThrow(InternalErrorException.class).when(this.snsClient)
//        .createPlatformEndpoint(any(CreatePlatformEndpointRequest.class));
//
//    try {
//      CreatePlatformEndpointResult createPlatformEndpointResult =
//          this.snsAccessManager
//              .registerPlatformEndpoint(new CreatePlatformEndpointRequest(), this.metrics);
//    } catch (Exception e) {
//      verify(this.snsClient, times(1))
//          .createPlatformEndpoint(any(CreatePlatformEndpointRequest.class));
//      verify(this.snsClient, times(0))
//          .getEndpointAttributes(any(GetEndpointAttributesRequest.class));
//    }
//  }
//
//  //////////////////////////////////////endregion
//  // unregisterPlatformEndpoint tests //
//  //////////////////////////////////////region
//
//  @Test
//  public void unregisterPlatformEndpoint_validInput_successfulResult() {
//    doReturn(new DeleteEndpointResult()).when(this.snsClient)
//        .deleteEndpoint(any(DeleteEndpointRequest.class));
//
//    DeleteEndpointResult deleteEndpointResult =
//        this.snsAccessManager.unregisterPlatformEndpoint(new DeleteEndpointRequest());
//
//    verify(this.snsClient, times(1))
//        .deleteEndpoint(any(DeleteEndpointRequest.class));
//  }
//
//  ////////////////////////////endregion
//  // sendMutedMessage tests //
//  ////////////////////////////region
//
//  @Test
//  public void sendMutedMessage_validInput_successfulResult() {
//    try {
//      doReturn(new PublishResult()).when(this.snsClient)
//          .publish(any(PublishRequest.class));
//
//      PublishResult publishResult = this.snsAccessManager
//          .sendMutedMessage("arn",
//              new Metadata("action", ImmutableMap.of("key", "value")));
//
//      verify(this.snsClient, times(1)).publish(any(PublishRequest.class));
//    } catch (final Exception e) {
//      System.out.println(e);
//      fail();
//    }
//  }
//
//  @Test
//  public void sendMutedMessage_endPointDisabled_failureResult() {
//    doThrow(EndpointDisabledException.class).when(this.snsClient)
//        .publish(any(PublishRequest.class));
//
//    PublishResult publishResult = this.snsAccessManager
//        .sendMutedMessage("arn",
//            new Metadata("action", ImmutableMap.of("key", "value")));
//
//    verify(this.snsClient, times(1)).publish(any(PublishRequest.class));
//  }
//
//  ///////////////////////endregion
//  // sendMessage tests //
//  ///////////////////////region
//
//  @Test
//  public void sendMessage_validInput_successfulResult() {
//    try {
//      doReturn(new PublishResult()).when(this.snsClient)
//          .publish(any(PublishRequest.class));
//
//      PublishResult publishResult = this.snsAccessManager
//          .sendMessage("arn", "title", "body", "tag",
//              new Metadata("action", ImmutableMap.of("key", "value")));
//
//      verify(this.snsClient, times(1)).publish(any(PublishRequest.class));
//    } catch (final Exception e) {
//      System.out.println(e);
//      fail();
//    }
//  }
//
//  @Test
//  public void sendMessage_endPointDisabled_failureResult() {
//    doThrow(EndpointDisabledException.class).when(this.snsClient)
//        .publish(any(PublishRequest.class));
//
//    PublishResult publishResult = this.snsAccessManager
//        .sendMessage("arn", "title", "body", "tag",
//            new Metadata("action", ImmutableMap.of("key", "value")));
//
//    verify(this.snsClient, times(1)).publish(any(PublishRequest.class));
//  }
//
//  /////////////////////////////////endregion
//  // getPlatformAttributes tests //
//  /////////////////////////////////region
//
//  @Test
//  public void getPlatformAttributes_validInput_successfulResult() {
//    doReturn(new GetPlatformApplicationAttributesResult()).when(this.snsClient)
//        .getPlatformApplicationAttributes(any(GetPlatformApplicationAttributesRequest.class));
//
//    GetPlatformApplicationAttributesResult platformApplicationAttributesResult =
//        this.snsAccessManager.getPlatformAttributes("platformArn");
//
//    verify(this.snsClient, times(1))
//        .getPlatformApplicationAttributes(any(GetPlatformApplicationAttributesRequest.class));
//  }
//
//  //endregion
//}
