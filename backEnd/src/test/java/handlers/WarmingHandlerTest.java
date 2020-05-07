package handlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import managers.DbAccessManager;
import managers.S3AccessManager;
import managers.SnsAccessManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utilities.Config;
import utilities.Metrics;
import utilities.ResultStatus;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class WarmingHandlerTest {

  private WarmingHandler warmingHandler;

  @Mock
  private DbAccessManager dbAccessManager;

  @Mock
  private S3AccessManager s3AccessManager;

  @Mock
  private SnsAccessManager snsAccessManager;

  @Mock
  private Metrics metrics;

  @BeforeEach
  private void init() {
    this.warmingHandler = new WarmingHandler(this.dbAccessManager, null, this.metrics);

    DatabaseManagers.S3_ACCESS_MANAGER = this.s3AccessManager;
    DatabaseManagers.SNS_ACCESS_MANAGER = this.snsAccessManager;
  }

  ////////////////////////
  // warmAllConnections //
  ////////////////////////region

  @Test
  void warmAllConnections_validInput_successfulResult() {
    final ResultStatus resultStatus = this.warmingHandler.handle();

    assertTrue(resultStatus.success);
    verify(this.dbAccessManager, times(1)).describeTables();
    ;
    verify(this.s3AccessManager, times(1)).imageBucketExists();
    verify(this.snsAccessManager, times(1)).getPlatformAttributes(Config.PUSH_SNS_PLATFORM_ARN);
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  void warmAllConnections_validInputBrokenDependency_failureResult() {
    doThrow(NullPointerException.class).when(this.dbAccessManager).describeTables();

    final ResultStatus resultStatus = this.warmingHandler.handle();

    assertFalse(resultStatus.success);
    verify(this.s3AccessManager, times(0)).imageBucketExists();
    verify(this.snsAccessManager, times(0)).getPlatformAttributes(Config.PUSH_SNS_PLATFORM_ARN);
    verify(this.metrics, times(1)).commonClose(false);
  }

  //endregion
}
