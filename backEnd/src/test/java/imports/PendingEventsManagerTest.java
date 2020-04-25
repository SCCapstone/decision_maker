package imports;

import static imports.PendingEventsManager.SCANNER_ID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.InvalidExecutionInputException;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import models.Group;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

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

  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

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

  /////////////////////////////////////////////////////////////////////////////////////////////////////endregion
  // processPendingEvent, getSelectedChoice, getPartitionKey, and getTentativeAlgorithmChoices tests //
  /////////////////////////////////////////////////////////////////////////////////////////////////////region

  private Map<String, Object> processPendingEventGoodInput = new HashMap<>(ImmutableMap.of(
      GroupsManager.GROUP_ID, "b702b849-d981-4b74-bb93-18819a2c1156",
      RequestFields.EVENT_ID, "593e9c8e-a713-40ff-ae55-5d5bdfe940f4", // in consider
      SCANNER_ID, "1"
  ));

  @Test
  public void processPendingEvent_validInputNeedsTentativeChoices_successfulResult() {
    try {
      this.environmentVariables.set(PendingEventsManager.NUMBER_OF_PARTITIONS_ENV_KEY, "1");
      doReturn(JsonUtils.getItemFromFile("groupWithAllEventStages.json")).when(this.groupsManager)
          .getItemByPrimaryKey("b702b849-d981-4b74-bb93-18819a2c1156");
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

      final ResultStatus resultStatus = this.pendingEventsManager
          .processPendingEvent(this.processPendingEventGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(3)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void processPendingEvent_validInputNeedsSelectedChoice_successfulResult() {
    try {
      this.environmentVariables.set(PendingEventsManager.NUMBER_OF_PARTITIONS_ENV_KEY, "1");
      doReturn(JsonUtils.getItemFromFile("groupWithAllEventStages.json")).when(this.groupsManager)
          .getItemByPrimaryKey("b702b849-d981-4b74-bb93-18819a2c1156");
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

      //id of an event in the voting stage
      this.processPendingEventGoodInput
          .put(RequestFields.EVENT_ID, "0fe7c737-ef19-4540-b4c8-39f1d3e2264f");
      this.processPendingEventGoodInput.put(RequestFields.NEW_EVENT, false); // getting coverage
      final ResultStatus resultStatus = this.pendingEventsManager
          .processPendingEvent(this.processPendingEventGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void processPendingEvent_validInputNeedsSelectedChoiceWHasVotingNumbers_successfulResult() {
    try {
      this.environmentVariables.set(PendingEventsManager.NUMBER_OF_PARTITIONS_ENV_KEY, "1");
      doReturn(JsonUtils.getItemFromFile("groupWithAllEventStages.json")).when(this.groupsManager)
          .getItemByPrimaryKey("b702b849-d981-4b74-bb93-18819a2c1156");
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

      //id of an event in the voting stage
      this.processPendingEventGoodInput
          .put(RequestFields.EVENT_ID, "in-voting-with-votes");
      this.processPendingEventGoodInput.put(RequestFields.NEW_EVENT, false); // getting coverage
      final ResultStatus resultStatus = this.pendingEventsManager
          .processPendingEvent(this.processPendingEventGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void processPendingEvent_validInputNewEventVoteSkip_successfulResult() {
    try {
      //this will exclude any category choices from the json since this model doesn't include those
      final Group group = new Group(
          JsonUtils.getItemFromFile("groupWithAllEventStages.json").asMap());

      this.environmentVariables.set(PendingEventsManager.NUMBER_OF_PARTITIONS_ENV_KEY, "1");
      doReturn(group.asItem()).when(this.groupsManager)
          .getItemByPrimaryKey("b702b849-d981-4b74-bb93-18819a2c1156");
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));
      doReturn(JsonUtils.getItemFromFile("categoryLunchOptions.json")).when(this.categoriesManager)
          .getItemByPrimaryKey(any(String.class));
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

      this.processPendingEventGoodInput.put(RequestFields.NEW_EVENT, true);
      this.processPendingEventGoodInput
          .put(RequestFields.EVENT_ID, "253b8594-e66d-4f15-88dd-3acbbde76a9c");
      final ResultStatus resultStatus = this.pendingEventsManager
          .processPendingEvent(this.processPendingEventGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(2)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void processPendingEvent_validInputNewEventNoCategory_successfulResult() {
    try {
      //this will exclude any category choices from the json since this model doesn't include those
      final Group group = new Group(
          JsonUtils.getItemFromFile("groupWithAllEventStages.json").asMap());

      this.environmentVariables.set(PendingEventsManager.NUMBER_OF_PARTITIONS_ENV_KEY, "1");
      doReturn(group.asItem()).when(this.groupsManager)
          .getItemByPrimaryKey("b702b849-d981-4b74-bb93-18819a2c1156");
      doReturn(null).when(this.categoriesManager).getItemByPrimaryKey(any(String.class));
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

      this.processPendingEventGoodInput.put(RequestFields.NEW_EVENT, true);
      final ResultStatus resultStatus = this.pendingEventsManager
          .processPendingEvent(this.processPendingEventGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(2)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void processPendingEvent_groupDataNotFound_failureResult() {
    doReturn(null).when(this.groupsManager)
        .getItemByPrimaryKey("b702b849-d981-4b74-bb93-18819a2c1156");

    final ResultStatus resultStatus = this.pendingEventsManager
        .processPendingEvent(this.processPendingEventGoodInput, this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void processPendingEvent_missingRequestKeys_failureResult() {
    final ResultStatus resultStatus = this.pendingEventsManager
        .processPendingEvent(Collections.emptyMap(), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void processPendingEvent_noDbConnection_failureResult() {
    try {
      this.environmentVariables.set(PendingEventsManager.NUMBER_OF_PARTITIONS_ENV_KEY, "1");
      doReturn(JsonUtils.getItemFromFile("groupWithAllEventStages.json")).when(this.groupsManager)
          .getItemByPrimaryKey("b702b849-d981-4b74-bb93-18819a2c1156");
      doReturn(null).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));

      final ResultStatus resultStatus = this.pendingEventsManager
          .processPendingEvent(this.processPendingEventGoodInput, this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.metrics, times(1)).commonClose(true);
      verify(this.metrics, times(2)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void processPendingEvent_badRequestValues_failureResult() {
    try {
      this.processPendingEventGoodInput.put(RequestFields.NEW_EVENT, "not a bool");
      final ResultStatus resultStatus = this.pendingEventsManager
          .processPendingEvent(this.processPendingEventGoodInput, this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  ///////////////////////////endregion
  // addPendingEvent tests //
  ///////////////////////////region

  @Test
  public void addPendingEvent_callWithZeroDuration_successResult() {
    final ResultStatus resultStatus = this.pendingEventsManager
        .addPendingEvent("groupId", "eventId", 0, this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(any(Boolean.class));
  }

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

  ///////////////////////////////////////endregion
  // deleteAllPendingGroupEvents tests //
  ///////////////////////////////////////region

  @Test
  public void deleteAllPendingGroupEvents_validInput_successfulResult() {
    this.environmentVariables.set(PendingEventsManager.NUMBER_OF_PARTITIONS_ENV_KEY, "3");
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

    final ResultStatus resultStatus = this.pendingEventsManager
        .deleteAllPendingGroupEvents("groupId", ImmutableSet.of("event1", "event2"), this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(3)).getTable(any(String.class)); // total # of db interactions
    verify(this.table, times(3)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
    verify(this.metrics, times(0)).commonClose(false);
  }

  @Test
  public void deleteAllPendingGroupEvents_validInputEmptyEvents_successfulResult() {
    final ResultStatus resultStatus = this.pendingEventsManager
        .deleteAllPendingGroupEvents("groupId", Collections.emptySet(), this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(any(Boolean.class));
  }

  @Test
  public void deleteAllPendingGroupEvents_noDbConnection_failureResult() {
    this.environmentVariables.set(PendingEventsManager.NUMBER_OF_PARTITIONS_ENV_KEY, "3");
    doReturn(null, this.table).when(this.dynamoDB).getTable(any(String.class));

    final ResultStatus resultStatus = this.pendingEventsManager
        .deleteAllPendingGroupEvents("groupId", ImmutableSet.of("event1", "event2"), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(3)).getTable(any(String.class)); // total # of db interactions
    verify(this.table, times(2)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void deleteAllPendingGroupEvents_environmentVarNotSet_failureResult() {
    this.environmentVariables.set(PendingEventsManager.NUMBER_OF_PARTITIONS_ENV_KEY, null);
    final ResultStatus resultStatus = this.pendingEventsManager
        .deleteAllPendingGroupEvents("groupId", ImmutableSet.of("event1", "event2"), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  //endregion
}