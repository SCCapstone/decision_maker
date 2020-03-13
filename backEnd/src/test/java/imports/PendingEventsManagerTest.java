package imports;

import static imports.PendingEventsManager.SCANNER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.InvalidExecutionInputException;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utilities.Metrics;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class PendingEventsManagerTest {

  private static final Map PENDING_EVENTS_ITEM_INSTANCE = ImmutableMap
      .of(SCANNER_ID, "1", "gid;eid", "yyyy-mm-dd hh:mm:ss");
  private String YESTERDAY;
  private String TOMORROW;

  private PendingEventsManager pendingEventsManager;

  @Mock
  private Table table;

  @Mock
  private DynamoDB dynamoDB;

  @Mock
  private CategoriesManager categoriesManager;

  @Mock
  private UsersManager usersManager;

  @Mock
  private GroupsManager groupsManager;

  @Mock
  private AWSStepFunctions awsStepFunctions;

  @Mock
  private Metrics metrics;

  @BeforeEach
  private void init() {
    this.pendingEventsManager = new PendingEventsManager(this.dynamoDB, this.awsStepFunctions);

    DatabaseManagers.CATEGORIES_MANAGER = this.categoriesManager;
    DatabaseManagers.USERS_MANAGER = this.usersManager;
    DatabaseManagers.GROUPS_MANAGER = this.groupsManager;

    this.YESTERDAY = LocalDateTime.now(ZoneId.of("UTC")).minus(1, ChronoUnit.DAYS)
        .format(this.pendingEventsManager.getDateTimeFormatter());
    this.TOMORROW = LocalDateTime.now(ZoneId.of("UTC")).plus(1, ChronoUnit.DAYS)
        .format(this.pendingEventsManager.getDateTimeFormatter());
  }

  ///////////////////////////////
  // processPendingEvent tests //
  ///////////////////////////////region

  @Test
  public void processPendingEvent_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withString(SCANNER_ID, "1").withString("gid;eid", this.YESTERDAY)
        .withString("gid;eid2", this.TOMORROW)).when(this.table).getItem(any(GetItemSpec.class));

    this.pendingEventsManager.scanPendingEvents("1", this.metrics);

    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(2)).commonClose(true);
    verify(this.awsStepFunctions, times(1)).startExecution(any(StartExecutionRequest.class));
  }

  ///////////////////////////endregion
  // addPendingEvent tests //
  ///////////////////////////region

  ///////////////////////////////////////////////////////////endregion
  // scanPendingEvents and startStepMachineExecution tests //
  ///////////////////////////////////////////////////////////region

  @Test
  public void scanPendingEvents_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withString(SCANNER_ID, "1").withString("gid;eid", this.YESTERDAY)
        .withString("gid;eid2", this.TOMORROW)).when(this.table).getItem(any(GetItemSpec.class));

    this.pendingEventsManager.scanPendingEvents("1", this.metrics);

    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(2)).commonClose(true);
    verify(this.awsStepFunctions, times(1)).startExecution(any(StartExecutionRequest.class));
  }

  @Test
  public void scanPendingEvents_stepFunctionWontStart_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withString(SCANNER_ID, "1").withString("gid;eid", this.YESTERDAY)
        .withString("gid;eid2", this.TOMORROW)).when(this.table).getItem(any(GetItemSpec.class));
    doThrow(InvalidExecutionInputException.class).when(this.awsStepFunctions)
        .startExecution(any(StartExecutionRequest.class));

    this.pendingEventsManager.scanPendingEvents("1", this.metrics);

    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(2)).commonClose(false);
    verify(this.awsStepFunctions, times(1)).startExecution(any(StartExecutionRequest.class));
  }

  @Test
  public void scanPendingEvents_badPendingEventKey_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withString(SCANNER_ID, "1").withString("gid;eid;bad", this.YESTERDAY)
        .withString("gid;eid2", this.YESTERDAY)).when(this.table).getItem(any(GetItemSpec.class));

    this.pendingEventsManager.scanPendingEvents("1", this.metrics);

    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
    verify(this.awsStepFunctions, times(0)).startExecution(any(StartExecutionRequest.class));
  }

  @Test
  public void scanPendingEvents_badDateFormat_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withString(SCANNER_ID, "1")
        .withString("gid;eid", "lol this isn't a date string")).when(this.table)
        .getItem(any(GetItemSpec.class));

    this.pendingEventsManager.scanPendingEvents("1", this.metrics);

    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
    verify(this.awsStepFunctions, times(0)).startExecution(any(StartExecutionRequest.class));
  }

  @Test
  public void scanPendingEvents_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    this.pendingEventsManager.scanPendingEvents("1", this.metrics);

    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.metrics, times(1)).commonClose(false);
    verify(this.awsStepFunctions, times(0)).startExecution(any(StartExecutionRequest.class));
  }

  ///////////////////////////endregion
  // getPartitionKey tests //
  ///////////////////////////region

  //endregion
}