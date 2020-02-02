package imports;

import static imports.GroupsManager.CATEGORIES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class GroupsManagerTest {

  private GroupsManager groupsManager;

  private final String goodCategoryId = "CategoryId1";

  private final ArrayList<String> goodGroupIds = new ArrayList<>();

  private final ImmutableMap<String, Object> newEventGoodInput = ImmutableMap.<String, Object>builder()
      .put(RequestFields.ACTIVE_USER, "ActiveUser")
      .put(GroupsManager.EVENT_NAME, "EventName")
      .put(GroupsManager.CATEGORY_ID, "CategoryId")
      .put(GroupsManager.CATEGORY_NAME, "CategoryName")
      .put(GroupsManager.CREATED_DATE_TIME, "CreatedDateTime")
      .put(GroupsManager.EVENT_START_DATE_TIME, "EventStartDateTime")
      .put(GroupsManager.TYPE, 1)
      .put(GroupsManager.POLL_DURATION, 50)
      .put(GroupsManager.EVENT_CREATOR, ImmutableMap.of("username", "name"))
      .put(GroupsManager.POLL_PASS_PERCENT, 50)
      .put(GroupsManager.GROUP_ID, "GroupId")
      .build();

  private final Map<String, Object> newEventBadInput = Maps
      .newHashMap(ImmutableMap.<String, Object>builder()
          .put(RequestFields.ACTIVE_USER, "ActiveUser")
          .put(GroupsManager.EVENT_NAME, "EventName")
          .put(GroupsManager.CATEGORY_ID, "CategoryId")
          .put(GroupsManager.CATEGORY_NAME, "CategoryName")
          .put(GroupsManager.CREATED_DATE_TIME, "CreatedDateTime")
          .put(GroupsManager.EVENT_START_DATE_TIME, "EventStartDateTime")
          .put(GroupsManager.TYPE, 1)
          .put(GroupsManager.POLL_DURATION, 50)
          .put(GroupsManager.EVENT_CREATOR, ImmutableMap.of("username", "name"))
          .put(GroupsManager.POLL_PASS_PERCENT, 50)
          .put(GroupsManager.GROUP_ID, "GroupId")
          .build());

  @Mock
  private Table table;

  @Mock
  private DynamoDB dynamoDB;

  @Mock
  private CategoriesManager categoriesManager;

  @Mock
  private UsersManager usersManager;

  @Mock
  private PendingEventsManager pendingEventsManager;

  @Mock
  private LambdaLogger lambdaLogger;

  @Mock
  private Metrics metrics;

  @BeforeEach
  private void init() {
    this.groupsManager = new GroupsManager(this.dynamoDB);
    this.goodGroupIds.add("GoodGroupId");

    DatabaseManagers.CATEGORIES_MANAGER = this.categoriesManager;
    DatabaseManagers.USERS_MANAGER = this.usersManager;
    DatabaseManagers.GROUPS_MANAGER = this.groupsManager;
    DatabaseManagers.PENDING_EVENTS_MANAGER = this.pendingEventsManager;
  }

  /////////////////////
  // getGroups tests //
  /////////////////////region

  //////////////////////////endregion
  // createNewGroup tests //
  //////////////////////////region

  /////////////////////endregion
  // editGroup tests //
  /////////////////////region

  ////////////////////endregion
  // newEvent tests //
  ////////////////////region
  @Test
  public void newEvent_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(GroupsManager.MEMBERS, ImmutableMap.of("user1", "name1"))
        .withBigInteger(GroupsManager.NEXT_EVENT_ID, BigInteger.ONE)).when(this.table)
        .getItem(any(GetItemSpec.class));

    ResultStatus result = this.groupsManager
        .newEvent(this.newEventGoodInput, this.metrics, this.lambdaLogger);

    assertTrue(result.success);
    verify(this.dynamoDB, times(2)).getTable(any(String.class));
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void newEvent_missingRequestKeys_failureResult() {
    this.newEventBadInput.remove(GroupsManager.GROUP_ID);

    ResultStatus result = this.groupsManager
        .newEvent(this.newEventBadInput, this.metrics, this.lambdaLogger);

    assertFalse(result.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void newEvent_badInput_failureResult() {
    this.newEventBadInput.put(GroupsManager.POLL_PASS_PERCENT, "General Kenobi!");

    ResultStatus result = this.groupsManager
        .newEvent(this.newEventBadInput, this.metrics, this.lambdaLogger);

    assertFalse(result.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void newEvent_groupNotFound_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(null).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus result = this.groupsManager
        .newEvent(this.newEventGoodInput, this.metrics, this.lambdaLogger);

    assertFalse(result.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void newEvent_missingMembersField_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap("NoMembers", ImmutableMap.of("user1", "name1"))
        .withBigInteger(GroupsManager.NEXT_EVENT_ID, BigInteger.ONE)).when(this.table)
        .getItem(any(GetItemSpec.class));

    ResultStatus result = this.groupsManager
        .newEvent(this.newEventGoodInput, this.metrics, this.lambdaLogger);

    assertFalse(result.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void newEvent_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.groupsManager
        .newEvent(this.newEventGoodInput, this.metrics, this.lambdaLogger);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.metrics, times(1)).commonClose(false);
  }
  //////////////////////////endregion
  // optInOutOfEvent tests //
  //////////////////////////region

  ////////////////////////////////////////endregion
  // updateMembersMapForInsertion tests //
  ////////////////////////////////////////region

  ////////////////////////////endregion
  // editInputIsValid tests //
  ////////////////////////////region

  /////////////////////////////////endregion
  // validEventInput tests //
  /////////////////////////////////region
  @Test
  public void validEventInput_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(GroupsManager.MEMBERS, ImmutableMap.of("user1", "name1"))
        .withBigInteger(GroupsManager.NEXT_EVENT_ID, BigInteger.ONE)).when(this.table)
        .getItem(any(GetItemSpec.class));

    ResultStatus result = this.groupsManager
        .newEvent(this.newEventGoodInput, this.metrics, this.lambdaLogger);
    assertTrue(result.success);
  }

  @Test
  public void validEventInput_emptyString_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(GroupsManager.MEMBERS, ImmutableMap.of("user1", "name1"))
        .withBigInteger(GroupsManager.NEXT_EVENT_ID, BigInteger.ONE)).when(this.table)
        .getItem(any(GetItemSpec.class));

    this.newEventBadInput.put(GroupsManager.GROUP_ID, "");
    this.newEventBadInput.put(GroupsManager.CATEGORY_ID, "");
    ResultStatus result = this.groupsManager
        .newEvent(this.newEventBadInput, this.metrics, this.lambdaLogger);
    assertFalse(result.success);

    this.newEventBadInput.put(GroupsManager.GROUP_ID, "GroupId");
    this.newEventBadInput.put(GroupsManager.CATEGORY_ID, "");
    result = this.groupsManager
        .newEvent(this.newEventBadInput, this.metrics, this.lambdaLogger);
    assertFalse(result.success);
  }

  @Test
  public void validEventInput_invalidPollDuration_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(GroupsManager.MEMBERS, ImmutableMap.of("user1", "name1"))
        .withBigInteger(GroupsManager.NEXT_EVENT_ID, BigInteger.ONE)).when(this.table)
        .getItem(any(GetItemSpec.class));

    this.newEventBadInput.put(GroupsManager.POLL_DURATION, -1);
    ResultStatus result = this.groupsManager
        .newEvent(this.newEventBadInput, this.metrics, this.lambdaLogger);
    assertFalse(result.success);

    this.newEventBadInput.put(GroupsManager.POLL_DURATION, 1000000);
    result = this.groupsManager
        .newEvent(this.newEventBadInput, this.metrics, this.lambdaLogger);
    assertFalse(result.success);
  }

  @Test
  public void validEventInput_invalidPercentage_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(GroupsManager.MEMBERS, ImmutableMap.of("user1", "name1"))
        .withBigInteger(GroupsManager.NEXT_EVENT_ID, BigInteger.ONE)).when(this.table)
        .getItem(any(GetItemSpec.class));

    this.newEventBadInput.put(GroupsManager.POLL_PASS_PERCENT, -1);
    ResultStatus result = this.groupsManager
        .newEvent(this.newEventBadInput, this.metrics, this.lambdaLogger);
    assertFalse(result.success);

    this.newEventBadInput.put(GroupsManager.POLL_PASS_PERCENT, 1000000);
    result = this.groupsManager
        .newEvent(this.newEventBadInput, this.metrics, this.lambdaLogger);
    assertFalse(result.success);
  }

  ///////////////////////////////////endregion
  // editInputHasPermissions tests //
  ///////////////////////////////////region

  ////////////////////////////endregion
  // updateUsersTable tests //
  ////////////////////////////region

  /////////////////////////////////endregion
  // updateCategoriesTable tests //
  /////////////////////////////////region

  /////////////////////////////endregion
  // getAllCategoryIds tests //
  /////////////////////////////region

  @Test
  public void getAllCategoryIds_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(CATEGORIES, ImmutableMap.of("id", "name", "id2", "name2")))
        .when(this.table).getItem(any(GetItemSpec.class));

    List<String> categoryIds = this.groupsManager
        .getAllCategoryIds("groupId", this.metrics, this.lambdaLogger);

    assertEquals(categoryIds.size(), 2);
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getAllCategoryIds_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    List<String> categoryIds = this.groupsManager
        .getAllCategoryIds("groupId", this.metrics, this.lambdaLogger);

    assertEquals(categoryIds.size(), 0);
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void getAllCategoryIds_badGroupFormat_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item()).when(this.table).getItem(any(GetItemSpec.class));

    List<String> categoryIds = this.groupsManager
        .getAllCategoryIds("groupId", this.metrics, this.lambdaLogger);

    assertEquals(categoryIds.size(), 0);
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  ////////////////////////////////////endregion
  // removeCategoryFromGroups tests //
  ////////////////////////////////////region
  @Test
  public void removeCategoryFromGroups_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    ResultStatus result = this.groupsManager
        .removeCategoryFromGroups(this.goodGroupIds, this.goodCategoryId, this.metrics,
            this.lambdaLogger);

    assertTrue(result.success);
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  public void removeCategoryFromGroups_emptyGroupList_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    ResultStatus result = this.groupsManager
        .removeCategoryFromGroups(new ArrayList<>(), this.goodCategoryId, this.metrics,
            this.lambdaLogger);

    assertTrue(result.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  public void removeCategoryFromGroups_emptyCategoryId_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    ResultStatus result = this.groupsManager
        .removeCategoryFromGroups(this.goodGroupIds, null, this.metrics, this.lambdaLogger);

    assertFalse(result.success);
    verify(this.dynamoDB, times(0)).getTable(
        any(String.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void removeCategoryFromGroups_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));
    ResultStatus resultStatus = this.groupsManager
        .removeCategoryFromGroups(this.goodGroupIds, this.goodCategoryId, this.metrics,
            this.lambdaLogger);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.metrics, times(1)).commonClose(false);
  }
  //endregion
}
