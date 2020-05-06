package imports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
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
public class WarmingManagerTest {

  private WarmingHandler warmingHandler;
  private UsersManager usersManager;
  private GroupsManager groupsManager;
  private CategoriesManager categoriesManager;

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
    this.warmingHandler = new WarmingHandler(this.dbAccessManager);

    DatabaseManagers.S3_ACCESS_MANAGER = this.s3AccessManager;
    DatabaseManagers.SNS_ACCESS_MANAGER = this.snsAccessManager;
  }

  ////////////////////////
  // warmAllConnections //
  ////////////////////////region

  @Test
  void warmAllConnections_validInput_successfulResult() {
    final ResultStatus resultStatus = this.warmingHandler.handle(null, this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dbAccessManager, times(1)).describeTables();;
    verify(this.s3AccessManager, times(1)).imageBucketExists();
    verify(this.snsAccessManager, times(1)).getPlatformAttributes(Config.PUSH_SNS_PLATFORM_ARN);
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  void warmAllConnections_validInputBrokenDependency_failureResult() {
    doThrow(NullPointerException.class).when(this.dbAccessManager).describeTables();

    final ResultStatus resultStatus = this.warmingHandler.handle(null, this.metrics);

    assertFalse(resultStatus.success);
    verify(this.s3AccessManager, times(0)).imageBucketExists();
    verify(this.snsAccessManager, times(0)).getPlatformAttributes(Config.PUSH_SNS_PLATFORM_ARN);
    verify(this.metrics, times(1)).commonClose(false);
  }

  //endregion
}
