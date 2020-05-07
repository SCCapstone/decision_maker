package imports;

import static junit.framework.TestCase.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import models.Event;
import models.Group;
import models.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class GroupsManagerTest {

  private GroupsManager groupsManager;

  private final String goodCategoryId = "CategoryId1";

  private final Set<String> goodGroupIds = ImmutableSet.of("id1", "id2");

  //This edit group input is in reference to the group in bigGroup.json
  private final Map<String, Object> editGroupValidInput = Maps
      .newHashMap(ImmutableMap.<String, Object>builder()
          .put(RequestFields.ACTIVE_USER, "john_andrews12") // group creator
          .put(GroupsManager.GROUP_ID, "GroupId")
          .put(GroupsManager.GROUP_NAME, "New Name") // updating the name
          //I removed all of the 'number' usernames and added johnplaysgolf
          .put(GroupsManager.MEMBERS,
              ImmutableList.of("johnplaysgolf", "john_andrews12", "josh", "edmond"))
          //I removed the existing category and added a new one
          .put(GroupsManager.CATEGORIES, ImmutableMap.of("new_cat_id", "new cat name"))
          .put(GroupsManager.DEFAULT_VOTING_DURATION, 5) // was 2
          .put(GroupsManager.DEFAULT_RSVP_DURATION, 5) // was 2
          .put(GroupsManager.IS_OPEN, true) // was false
          .put(RequestFields.BATCH_NUMBER, 0)
          .put(GroupsManager.ICON, ImmutableList.of(0, 1, 2, 3, 4, 5, 6)) // new file data
          .build());

  //The leave group inputs are in reference to the group in bigGroup.json
  private final Map<String, Object> leaveGroupGoodInput = ImmutableMap.<String, Object>builder()
      .put(RequestFields.ACTIVE_USER, "johnplaysgolf")
      .put(GroupsManager.GROUP_ID, "bc9c84d9-ae4d-4b75-9f3c-8f9c598e2f48")
      .build();

  private final Map<String, Object> leaveGroupBadInput = ImmutableMap.<String, Object>builder()
      .put(RequestFields.ACTIVE_USER, "john_andrews12")
      .put(GroupsManager.GROUP_ID, "bc9c84d9-ae4d-4b75-9f3c-8f9c598e2f48")
      .build();

  //The rejoin group inputs are in reference to the group in openGroup.json
  private final Map<String, Object> rejoinGroupGoodInput = ImmutableMap.<String, Object>builder()
      .put(RequestFields.ACTIVE_USER, "edmond2")
      .put(GroupsManager.GROUP_ID, "13027fec-7fd7-4290-bd50-4dd84a572a4d")
      .build();

  private final Map<String, Object> rejoinGroupBadInput = ImmutableMap.<String, Object>builder()
      .put(RequestFields.ACTIVE_USER, "john_andrews12")
      .put(GroupsManager.GROUP_ID, "13027fec-7fd7-4290-bd50-4dd84a572a4d")
      .build();

  private final Map<String, Object> deleteGroupGoodInput = ImmutableMap.<String, Object>builder()
      .put(RequestFields.ACTIVE_USER, "john_andrews12") // openGroup.json owner
      .put(GroupsManager.GROUP_ID, "13027fec-7fd7-4290-bd50-4dd84a572a4d") // openGroup.json
      .build();

  private final Map<String, Object> handleGetBatchOfEventsGoodInput = Maps
      .newHashMap(ImmutableMap.<String, Object>builder()
          .put(RequestFields.ACTIVE_USER, "john_andrews12")
          .put(GroupsManager.GROUP_ID, "GroupId")
          .put(RequestFields.BATCH_NUMBER, 2)
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
  private S3AccessManager s3AccessManager;

  @Mock
  private SnsAccessManager snsAccessManager;

  @Mock
  private Metrics metrics;

  @BeforeEach
  private void init() {
    this.groupsManager = new GroupsManager(this.dynamoDB);

    DatabaseManagers.CATEGORIES_MANAGER = this.categoriesManager;
    DatabaseManagers.USERS_MANAGER = this.usersManager;
    DatabaseManagers.GROUPS_MANAGER = this.groupsManager;
    DatabaseManagers.PENDING_EVENTS_MANAGER = this.pendingEventsManager;
    DatabaseManagers.S3_ACCESS_MANAGER = this.s3AccessManager;
    DatabaseManagers.SNS_ACCESS_MANAGER = this.snsAccessManager;
  }

  ////////////////////
  // getGroup tests //
  ////////////////////region

  private final Map<String, Object> getGroupGoodInput = new HashMap<>(ImmutableMap.of(
      RequestFields.ACTIVE_USER, "john_andrews12",
      GroupsManager.GROUP_ID, "groupId",
      RequestFields.BATCH_NUMBER, 1
  ));

  @Test
  public void getGroup_validInput_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      final ResultStatus resultStatus = this.groupsManager
          .getGroup(this.getGroupGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class));
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void getGroup_invalidInputNotAMember_failureResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      this.getGroupGoodInput.put(RequestFields.ACTIVE_USER, "not_a_member");
      final ResultStatus resultStatus = this.groupsManager
          .getGroup(this.getGroupGoodInput, this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class));
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void getGroup_missingRequestKeys_failureResult() {
    final ResultStatus resultStatus = this.groupsManager
        .getGroup(Collections.emptyMap(), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void getGroup_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    this.getGroupGoodInput.put(RequestFields.ACTIVE_USER, "not_a_member");
    final ResultStatus resultStatus = this.groupsManager
        .getGroup(this.getGroupGoodInput, this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  //////////////////////////endregion
  // createNewGroup tests //
  //////////////////////////region

  private final Map<String, Object> createNewGroupGoodInput = Maps
      .newHashMap(ImmutableMap.<String, Object>builder()
          .put(RequestFields.ACTIVE_USER, "john_andrews12") // group creator
          .put(GroupsManager.GROUP_ID, "GroupId")
          .put(GroupsManager.GROUP_NAME, "New Name")
          .put(GroupsManager.MEMBERS,
              new ArrayList<>(
                  ImmutableList.of("john_andrews12", "edmond2", "johnplaysgolf", "user", "user2")))
          //I removed the existing category and added a new one
          .put(GroupsManager.CATEGORIES, ImmutableMap.of("new_cat_id", "new cat name"))
          .put(GroupsManager.DEFAULT_VOTING_DURATION, 5)
          .put(GroupsManager.DEFAULT_RSVP_DURATION, 5)
          .put(GroupsManager.IS_OPEN, true)
          .put(GroupsManager.ICON, ImmutableList.of(0, 1, 2, 3, 4, 5, 6)) // new file data
          .build());

  @Test
  public void createNewGroup_validInput_successfulResult() {
    try {
      final Map groupCreator = JsonUtils.getItemFromFile("john_andrews12.json").asMap();
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(
          groupCreator, // first five for insert
          groupCreator,
          groupCreator,
          groupCreator,
          groupCreator,
          //these five are for notification
          JsonUtils.getItemFromFile("edmond2.json").asMap(), // no push arn
          JsonUtils.getItemFromFile("edmond2.json")
              .withString(UsersManager.PUSH_ENDPOINT_ARN, "arn:1234").asMap(), // app muted
          groupCreator,
          JsonUtils.getItemFromFile("johnplaysgolf.json").asMap(), // app not muted
          null // cause error in send notification
      ).when(this.usersManager).getMapByPrimaryKey(any(String.class));
      doReturn(Optional.of("imageFileUrl")).when(this.s3AccessManager)
          .uploadImage(any(List.class), eq(this.metrics));

      final ResultStatus resultStatus = this.groupsManager
          .createNewGroup(this.createNewGroupGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).putItem(any(PutItemSpec.class));
      verify(this.usersManager, times(10))
          .getMapByPrimaryKey(any(String.class)); // usernames pulled for insert and notifications
      verify(this.metrics, times(3)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void createNewGroup_validInputNoIcon_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));

      this.createNewGroupGoodInput.remove(GroupsManager.ICON);
      final ResultStatus resultStatus = this.groupsManager
          .createNewGroup(this.createNewGroupGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).putItem(any(PutItemSpec.class));
      verify(this.usersManager, times(10))
          .getMapByPrimaryKey(any(String.class)); // usernames pulled for insert and notifications
      verify(this.metrics, times(4)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void createNewGroup_validInputUserUpdatesFail_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));
      doThrow(NullPointerException.class).when(this.usersManager)
          .updateItem(any(String.class), any(UpdateItemSpec.class));

      this.createNewGroupGoodInput.remove(GroupsManager.ICON);
      final ResultStatus resultStatus = this.groupsManager
          .createNewGroup(this.createNewGroupGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).putItem(any(PutItemSpec.class));
      verify(this.usersManager, times(10))
          .getMapByPrimaryKey(any(String.class)); // usernames pulled for insert and notifications
      verify(this.metrics, times(3)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void createNewGroup_badUsernamesEntered_failureResult() {
    doReturn(null).when(this.usersManager).getMapByPrimaryKey(any(String.class));

    this.createNewGroupGoodInput.remove(GroupsManager.ICON);
    final ResultStatus resultStatus = this.groupsManager
        .createNewGroup(this.createNewGroupGoodInput, this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void createNewGroup_missingRequestKey_failureResult() {
    final ResultStatus resultStatus = this.groupsManager
        .createNewGroup(Collections.emptyMap(), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  /////////////////////endregion
  // editGroup tests //
  /////////////////////region

  @Test
  void editGroup_validInput_successfulResult() {
    try {
      final Map john = JsonUtils.getItemFromFile("john_andrews12.json").asMap();
      final Map removed1 = john; // not muted at all
      final Map removed2 = JsonUtils.getItemFromFile("edmond2.json").asMap(); // no push arn
      final Map removed3 = JsonUtils.getItemFromFile("edmond2.json")
          .withString(UsersManager.PUSH_ENDPOINT_ARN, "arn:1234").asMap(); // app muted
      final Map removed4 = JsonUtils.getItemFromFile("johnplaysgolf.json").asMap(); // group muted
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));
      doReturn(Optional.of("new icon file name")).when(this.s3AccessManager)
          .uploadImage(any(List.class), any(Metrics.class));
      doReturn(john, john, john, john, removed1, removed2, removed3, removed4)
          .when(this.usersManager).getMapByPrimaryKey(any(String.class));

      ResultStatus resultStatus = this.groupsManager
          .editGroup(this.editGroupValidInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.usersManager, times(9)).getMapByPrimaryKey(any(String.class));
      verify(this.metrics, times(5)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  void editGroup_validInputNoIconChangeOpenGroup_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("openGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));
      doReturn(Collections.emptyMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));

      this.editGroupValidInput.remove(GroupsManager.ICON);
      ResultStatus resultStatus = this.groupsManager
          .editGroup(this.editGroupValidInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(5)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  void editGroup_validInputNoCategoryChanges_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));
      doReturn(Optional.of("new icon file name")).when(this.s3AccessManager)
          .uploadImage(any(List.class), any(Metrics.class));
      doReturn(Collections.emptyMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));

      //reset the categories and the group name to the the json values
      this.editGroupValidInput.put(GroupsManager.GROUP_NAME, "New Test!");
      this.editGroupValidInput.put(GroupsManager.CATEGORIES,
          ImmutableMap.of("4c27a381-cff9-4ed5-9020-41dc0f0f70c5", "version changes for real"));

      ResultStatus resultStatus = this.groupsManager
          .editGroup(this.editGroupValidInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(5)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  void editGroup_validInputCategoryAdded_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));
      doReturn(Optional.of("new icon file name")).when(this.s3AccessManager)
          .uploadImage(any(List.class), any(Metrics.class));
      doReturn(Collections.emptyMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));

      this.editGroupValidInput.put(GroupsManager.CATEGORIES,
          ImmutableMap.of(
              "4c27a381-cff9-4ed5-9020-41dc0f0f70c5", "version changes for real",
              "new_cat_id", "new category"
          ));

      ResultStatus resultStatus = this.groupsManager
          .editGroup(this.editGroupValidInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(5)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  void editGroup_validInputCategoriesFail_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));
      doReturn(Optional.of("new icon file name")).when(this.s3AccessManager)
          .uploadImage(any(List.class), any(Metrics.class));
      doReturn(Collections.emptyMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));
      doThrow(NullPointerException.class).when(this.categoriesManager)
          .updateItem(any(UpdateItemSpec.class));

      ResultStatus resultStatus = this.groupsManager
          .editGroup(this.editGroupValidInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(4)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  void editGroup_invalidInputUserNotInOpenGroupRemovedOwnerAddedLeft_failureResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("openGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      this.editGroupValidInput.put(RequestFields.ACTIVE_USER, "fakeUser");
      this.editGroupValidInput.put(GroupsManager.MEMBERS, ImmutableList.of("leftUser"));

      ResultStatus resultStatus = this.groupsManager
          .editGroup(this.editGroupValidInput, this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  void editGroup_invalidInputBadDurations_failureResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("openGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      this.editGroupValidInput.put(GroupsManager.DEFAULT_RSVP_DURATION, -1);
      this.editGroupValidInput.put(GroupsManager.DEFAULT_VOTING_DURATION, -1);

      ResultStatus resultStatus = this.groupsManager
          .editGroup(this.editGroupValidInput, this.metrics);
      assertFalse(resultStatus.success);

      this.editGroupValidInput.put(GroupsManager.DEFAULT_RSVP_DURATION, 10001);
      this.editGroupValidInput.put(GroupsManager.DEFAULT_VOTING_DURATION, 10001);

      resultStatus = this.groupsManager
          .editGroup(this.editGroupValidInput, this.metrics);
      assertFalse(resultStatus.success);

      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(2)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(2)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  void editGroup_invalidInputUserNotInGroup_failureResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      this.editGroupValidInput.put(RequestFields.ACTIVE_USER, "fakeUser");

      ResultStatus resultStatus = this.groupsManager
          .editGroup(this.editGroupValidInput, this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  void editGroup_missingKeys_failureResult() {
    ResultStatus resultStatus = this.groupsManager
        .editGroup(Collections.emptyMap(), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  void editGroup_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.groupsManager
        .editGroup(this.editGroupValidInput, this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  ///////////////////////endregion
  // deleteGroup tests //
  ///////////////////////region

  @Test
  public void deleteGroup_validInput_successfulResult() {
    try {
      final Group groupToDelete = new Group(JsonUtils.getItemFromFile("openGroup.json").asMap());
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(new ResultStatus(true, "usersManagerWorks")).when(this.usersManager)
          .removeGroupsLeftFromUsers(groupToDelete.getMembersLeft().keySet(),
              groupToDelete.getGroupId(), this.metrics);
      doReturn(JsonUtils.getItemFromFile("johnplaysgolf.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));
      doReturn(new ResultStatus(true, "categoriesManagerWorks")).when(this.categoriesManager)
          .removeGroupFromCategories(groupToDelete.getCategories().keySet(),
              groupToDelete.getGroupId(), this.metrics);
      doReturn(groupToDelete.asItem()).when(this.table)
          .getItem(any(GetItemSpec.class));

      final ResultStatus resultStatus = this.groupsManager
          .deleteGroup(this.deleteGroupGoodInput, metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).deleteItem(any(DeleteItemSpec.class));
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(2)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void deleteGroup_validInputUsersTableError_failureResult() {
    try {
      final Group groupToDelete = new Group(JsonUtils.getItemFromFile("openGroup.json").asMap());
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(new ResultStatus(false, "usersManagerFails")).when(this.usersManager)
          .removeGroupsLeftFromUsers(groupToDelete.getMembersLeft().keySet(),
              groupToDelete.getGroupId(), this.metrics);
      doReturn(new ResultStatus(true, "categoriesManagerWorks")).when(this.categoriesManager)
          .removeGroupFromCategories(groupToDelete.getCategories().keySet(),
              groupToDelete.getGroupId(), this.metrics);
      doReturn(groupToDelete.asItem()).when(this.table)
          .getItem(any(GetItemSpec.class));

      final ResultStatus resultStatus = this.groupsManager
          .deleteGroup(this.deleteGroupGoodInput, metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void deleteGroup_validInputCategoriesTableError_failureResult() {
    try {
      final Group groupToDelete = new Group(JsonUtils.getItemFromFile("openGroup.json").asMap());
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(new ResultStatus(true, "usersManagerWorks")).when(this.usersManager)
          .removeGroupsLeftFromUsers(groupToDelete.getMembersLeft().keySet(),
              groupToDelete.getGroupId(), this.metrics);
      doReturn(new ResultStatus(false, "categoriesManagerFails")).when(this.categoriesManager)
          .removeGroupFromCategories(groupToDelete.getCategories().keySet(),
              groupToDelete.getGroupId(), this.metrics);
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap(),
          JsonUtils.getItemFromFile("edmond2.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));
      doReturn(groupToDelete.asItem()).when(this.table)
          .getItem(any(GetItemSpec.class));

      final ResultStatus resultStatus = this.groupsManager
          .deleteGroup(this.deleteGroupGoodInput, metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void deleteGroup_validInputDeleteUsersError_failureResult() {
    try {
      final Group groupToDelete = new Group(JsonUtils.getItemFromFile("openGroup.json").asMap());
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(new ResultStatus(true, "usersManagerWorks")).when(this.usersManager)
          .removeGroupsLeftFromUsers(groupToDelete.getMembersLeft().keySet(),
              groupToDelete.getGroupId(), this.metrics);
      doReturn(new ResultStatus(true, "categoriesManagerWorks")).when(this.categoriesManager)
          .removeGroupFromCategories(groupToDelete.getCategories().keySet(),
              groupToDelete.getGroupId(), this.metrics);
      doReturn(null).when(this.usersManager).getMapByPrimaryKey(any(String.class));
      doReturn(groupToDelete.asItem()).when(this.table)
          .getItem(any(GetItemSpec.class));

      final ResultStatus resultStatus = this.groupsManager
          .deleteGroup(this.deleteGroupGoodInput, metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(2)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void deleteGroup_userIsNotGroupCreator_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withString(GroupsManager.GROUP_CREATOR, "InvalidUser")).when(this.table)
        .getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.groupsManager.deleteGroup(this.deleteGroupGoodInput, metrics);
    assertFalse(resultStatus.success);

    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.metrics, times(1)).commonClose(false);
  }


  @Test
  public void deleteGroup_missingKeys_failureResult() {
    ResultStatus resultStatus = this.groupsManager.deleteGroup(Collections.emptyMap(), metrics);
    assertFalse(resultStatus.success);

    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void deleteGroup_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.groupsManager
        .deleteGroup(this.deleteGroupGoodInput, this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  ////////////////////endregion
  // newEvent tests //
  ////////////////////region

  private final Map<String, Object> newEventGoodInput = new HashMap<>(
      ImmutableMap.<String, Object>builder()
          .put(RequestFields.ACTIVE_USER, "john_andrews12")
          .put(GroupsManager.EVENT_NAME, "Event Name")
          .put(GroupsManager.CATEGORY_ID, "CategoryId")
          .put(GroupsManager.EVENT_START_DATE_TIME, "EventStartDateTime")
          .put(GroupsManager.VOTING_DURATION, 50)
          .put(GroupsManager.RSVP_DURATION, 50)
          .put(GroupsManager.GROUP_ID, "GroupId")
          .put(GroupsManager.UTC_EVENT_START_SECONDS, 123456)
          .build());

  @Test
  public void newEvent_validInput_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));
      doReturn(JsonUtils.getItemFromFile("categoryLunchOptions.json").asMap())
          .when(this.categoriesManager).getMapByPrimaryKey(any(String.class));

      final ResultStatus resultStatus = this.groupsManager
          .newEvent(this.newEventGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.pendingEventsManager, times(1))
          .addPendingEvent(any(String.class), any(String.class), eq(50), eq(this.metrics));
      verify(this.metrics, times(5)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void newEvent_validInputSkipRsvp_successfulResult() {
    //verify pending events processPendingEvent gets called
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));
      doReturn(JsonUtils.getItemFromFile("categoryLunchOptions.json").asMap())
          .when(this.categoriesManager).getMapByPrimaryKey(any(String.class));
      doReturn("1").when(this.pendingEventsManager).getPartitionKey();

      this.newEventGoodInput.put(GroupsManager.RSVP_DURATION, 0);
      final ResultStatus resultStatus = this.groupsManager
          .newEvent(this.newEventGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.pendingEventsManager, times(1))
          .processPendingEvent(any(Map.class), eq(this.metrics));
      verify(this.metrics, times(1)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void newEvent_validInputUserUpdatesFail_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));
      doReturn(JsonUtils.getItemFromFile("categoryLunchOptions.json").asMap())
          .when(this.categoriesManager).getMapByPrimaryKey(any(String.class));
      doThrow(NullPointerException.class).when(this.usersManager)
          .updateItem(any(String.class), any(UpdateItemSpec.class));

      final ResultStatus resultStatus = this.groupsManager
          .newEvent(this.newEventGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.pendingEventsManager, times(1))
          .addPendingEvent(any(String.class), any(String.class), eq(50), eq(this.metrics));
      verify(this.metrics, times(4)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void newEvent_invalidInputs_failureResult() {
    //verify pending events processPendingEvent gets called
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      //user that is not in the group
      this.newEventGoodInput.put(RequestFields.ACTIVE_USER, "johnplaysgolf");
      doReturn(JsonUtils.getItemFromFile("johnplaysgolf.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));

      this.newEventGoodInput.put(GroupsManager.RSVP_DURATION, GroupsManager.MAX_DURATION + 5);
      this.newEventGoodInput.put(GroupsManager.VOTING_DURATION, GroupsManager.MAX_DURATION + 5);
      this.newEventGoodInput.put(GroupsManager.EVENT_NAME, "");
      ResultStatus resultStatus = this.groupsManager.newEvent(this.newEventGoodInput, this.metrics);
      assertFalse(resultStatus.success);

      this.newEventGoodInput.put(GroupsManager.RSVP_DURATION, -1);
      this.newEventGoodInput.put(GroupsManager.VOTING_DURATION, -1);
      this.newEventGoodInput.put(GroupsManager.EVENT_NAME,
          IntStream.range(0, GroupsManager.MAX_EVENT_NAME_LENGTH + 5).boxed().map(Object::toString)
              .collect(Collectors.joining("")));
      resultStatus = this.groupsManager.newEvent(this.newEventGoodInput, this.metrics);
      assertFalse(resultStatus.success);

      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(2)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(2)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void newEvent_missingRequestKey_failureResult() {
    final ResultStatus resultStatus = this.groupsManager
        .newEvent(Collections.emptyMap(), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void newEvent_noDbConnection_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(null).when(this.table).getItem(any(GetItemSpec.class));

    this.newEventGoodInput.put(GroupsManager.RSVP_DURATION, 0);
    final ResultStatus resultStatus = this.groupsManager
        .newEvent(this.newEventGoodInput, this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  //////////////////////////endregion
  // optInOutOfEvent tests //
  //////////////////////////region

  private final Map<String, Object> optInOutOfEventGoodInput = new HashMap<>(ImmutableMap.of(
      GroupsManager.GROUP_ID, "b702b849-d981-4b74-bb93-18819a2c1156",
      RequestFields.PARTICIPATING, true,
      RequestFields.EVENT_ID, "593e9c8e-a713-40ff-ae55-5d5bdfe940f4", // in consider
      RequestFields.ACTIVE_USER, "john_andrews12"
  ));

  @Test
  public void optInOutOfEvent_validInputParticipating_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey("john_andrews12");

      final ResultStatus resultStatus = this.groupsManager
          .optInOutOfEvent(this.optInOutOfEventGoodInput, this.metrics);

      assertTrue(resultStatus.success);

      //here we're making sure the user map gets inserted
      final ArgumentCaptor<UpdateItemSpec> argument = ArgumentCaptor.forClass(UpdateItemSpec.class);
      verify(this.table).updateItem(argument.capture());
      assertTrue(argument.getValue().getValueMap().containsKey(":userMap"));
      assertEquals("set ", argument.getValue().getUpdateExpression().substring(0, 4));

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
  public void optInOutOfEvent_validInputNotParticipating_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey("john_andrews12");

      this.optInOutOfEventGoodInput.put(RequestFields.PARTICIPATING, false);
      final ResultStatus resultStatus = this.groupsManager
          .optInOutOfEvent(this.optInOutOfEventGoodInput, this.metrics);

      assertTrue(resultStatus.success);

      //here we're making sure the user map gets removed
      final ArgumentCaptor<UpdateItemSpec> argument = ArgumentCaptor.forClass(UpdateItemSpec.class);
      verify(this.table).updateItem(argument.capture());
      assertNull(argument.getValue().getValueMap());
      assertEquals("remove ", argument.getValue().getUpdateExpression().substring(0, 7));

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
  public void optInOutOfEvent_invalidInputUserNotInGroup_successfulResult() {
    try {
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey("john_andrews12");

      this.optInOutOfEventGoodInput.put(GroupsManager.GROUP_ID, "not_a_real_group_id");
      final ResultStatus resultStatus = this.groupsManager
          .optInOutOfEvent(this.optInOutOfEventGoodInput, this.metrics);

      assertFalse(resultStatus.success);

      verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void optInOutOfEvent_missingRequestKey_failureResult() {
    final ResultStatus resultStatus = this.groupsManager
        .optInOutOfEvent(Collections.emptyMap(), this.metrics);

    assertFalse(resultStatus.success);

    verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void optInOutOfEvent_noDbConnection_failureResult() {
    try {
      doReturn(null).when(this.usersManager).getMapByPrimaryKey("john_andrews12");

      final ResultStatus resultStatus = this.groupsManager
          .optInOutOfEvent(this.optInOutOfEventGoodInput, this.metrics);

      assertFalse(resultStatus.success);

      verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  ///////////////////////endregion
  // updateEvent tests //
  ///////////////////////region

  @Test
  public void updateEvent_validInputNewTentativeChoicesAndSelectedChoiceNewEvent_successfulResult
      () {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

      final Group group = new Group(
          JsonUtils.getItemFromFile("groupWithAllEventStages.json").asMap());
      //593e9c8e-a713-40ff-ae55-5d5bdfe940f4 is in consider
      final Event newEvent = group.getEvents().get("593e9c8e-a713-40ff-ae55-5d5bdfe940f4")
          .clone();
      newEvent.setTentativeAlgorithmChoices(ImmutableMap.of("1", "choice", "2", "c2"));
      newEvent.setSelectedChoice("choice");

      final ResultStatus resultStatus = this.groupsManager
          .updateEvent(group, "593e9c8e-a713-40ff-ae55-5d5bdfe940f4", newEvent, true,
              this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(5)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateEvent_validInputNewTentativeChoicesAndSelectedChoice_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

      final Group group = new Group(
          JsonUtils.getItemFromFile("groupWithAllEventStages.json").asMap());
      //593e9c8e-a713-40ff-ae55-5d5bdfe940f4 is in consider
      final Event newEvent = group.getEvents().get("593e9c8e-a713-40ff-ae55-5d5bdfe940f4")
          .clone();
      newEvent.setTentativeAlgorithmChoices(ImmutableMap.of("1", "choice", "2", "c2"));
      newEvent.setSelectedChoice("choice");

      final ResultStatus resultStatus = this.groupsManager
          .updateEvent(group, "593e9c8e-a713-40ff-ae55-5d5bdfe940f4", newEvent, false,
              this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(5)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateEvent_validInputNewTentativeChoices_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

      final Group group = new Group(
          JsonUtils.getItemFromFile("groupWithAllEventStages.json").asMap());
      //593e9c8e-a713-40ff-ae55-5d5bdfe940f4 is in consider
      final Event newEvent = group.getEvents().get("593e9c8e-a713-40ff-ae55-5d5bdfe940f4")
          .clone();
      newEvent.setTentativeAlgorithmChoices(ImmutableMap.of("1", "choice", "2", "c2"));

      final ResultStatus resultStatus = this.groupsManager
          .updateEvent(group, "593e9c8e-a713-40ff-ae55-5d5bdfe940f4", newEvent, false,
              this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(5)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateEvent_validInputNewTentativeChoicesNewEvent_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

      final Group group = new Group(
          JsonUtils.getItemFromFile("groupWithAllEventStages.json").asMap());
      group.setLastActivity(LocalDateTime.now(ZoneId.of("UTC"))
          .format(this.groupsManager
              .getDateTimeFormatter())); // set this to now so no change will be detected
      //593e9c8e-a713-40ff-ae55-5d5bdfe940f4 is in consider
      final Event newEvent = group.getEvents().get("593e9c8e-a713-40ff-ae55-5d5bdfe940f4")
          .clone();
      newEvent.setTentativeAlgorithmChoices(ImmutableMap.of("1", "choice", "2", "c2"));

      final ResultStatus resultStatus = this.groupsManager
          .updateEvent(group, "593e9c8e-a713-40ff-ae55-5d5bdfe940f4", newEvent, true,
              this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(5)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateEvent_validInputFromVotingToSelected_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

      final Group group = new Group(
          JsonUtils.getItemFromFile("groupWithAllEventStages.json").asMap());
      //0fe7c737-ef19-4540-b4c8-39f1d3e2264f is in voting
      final Event newEvent = group.getEvents().get("0fe7c737-ef19-4540-b4c8-39f1d3e2264f")
          .clone();
      newEvent.setSelectedChoice("Winner!");

      final ResultStatus resultStatus = this.groupsManager
          .updateEvent(group, "0fe7c737-ef19-4540-b4c8-39f1d3e2264f", newEvent, false,
              this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(5)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateEvent_validInputFromVotingToSelectedAllUserMessages_successfulResult() {
    try {
      final Map u1 = JsonUtils.getItemFromFile("john_andrews12.json").asMap(); // not muted at all
      final Map u2 = JsonUtils.getItemFromFile("edmond2.json").asMap(); // no push arn
      final Map u3 = JsonUtils.getItemFromFile("edmond2.json")
          .withString(UsersManager.PUSH_ENDPOINT_ARN, "arn:1234").asMap(); // app muted
      final Map u4 = JsonUtils.getItemFromFile("johnplaysgolf.json").asMap(); // group muted

      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(u1, u2, u3, u4).when(this.usersManager).getMapByPrimaryKey(any(String.class));

      final Group group = new Group(
          JsonUtils.getItemFromFile("groupWithAllEventStages.json").asMap());
      //0fe7c737-ef19-4540-b4c8-39f1d3e2264f is in voting
      final Event newEvent = group.getEvents().get("0fe7c737-ef19-4540-b4c8-39f1d3e2264f")
          .clone();
      newEvent.setSelectedChoice("Winner!");

      final ResultStatus resultStatus = this.groupsManager
          .updateEvent(group, "0fe7c737-ef19-4540-b4c8-39f1d3e2264f", newEvent, false,
              this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(5)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateEvent_validInputFromVotingToSelectedErrorSendingMessages_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class)); // not muted at all
      doThrow(AmazonServiceException.class).when(this.snsAccessManager)
          .sendMessage(any(String.class), any(String.class), any(String.class), any(String.class),
              any(Metadata.class));

      final Group group = new Group(
          JsonUtils.getItemFromFile("groupWithAllEventStages.json").asMap());
      //0fe7c737-ef19-4540-b4c8-39f1d3e2264f is in voting
      final Event newEvent = group.getEvents().get("0fe7c737-ef19-4540-b4c8-39f1d3e2264f")
          .clone();
      newEvent.setSelectedChoice("Winner!");

      final ResultStatus resultStatus = this.groupsManager
          .updateEvent(group, "0fe7c737-ef19-4540-b4c8-39f1d3e2264f", newEvent, false,
              this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(4)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateEvent_invalidInputNothingToUpdate_failureResult() {
    try {
      final Group group = new Group(
          JsonUtils.getItemFromFile("groupWithAllEventStages.json").asMap());
      //859c86d2-20e8-4c12-9d95-77a693c74c5d is finalized
      final Event newEvent = group.getEvents().get("859c86d2-20e8-4c12-9d95-77a693c74c5d")
          .clone();

      final ResultStatus resultStatus = this.groupsManager
          .updateEvent(group, "859c86d2-20e8-4c12-9d95-77a693c74c5d", newEvent, false,
              this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateEvent_invalidInputNotSettingTentativeChoices_failureResult() {
    try {
      final Group group = new Group(
          JsonUtils.getItemFromFile("groupWithAllEventStages.json").asMap());
      //593e9c8e-a713-40ff-ae55-5d5bdfe940f4 is in consider
      final Event newEvent = group.getEvents().get("593e9c8e-a713-40ff-ae55-5d5bdfe940f4")
          .clone();

      final ResultStatus resultStatus = this.groupsManager
          .updateEvent(group, "593e9c8e-a713-40ff-ae55-5d5bdfe940f4", newEvent, false,
              this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  /////////////////////////////endregion
  // getAllCategoryIds tests //
  /////////////////////////////region

  @Test
  public void getAllCategoryIds_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(
        new Item()
            .withMap(GroupsManager.CATEGORIES, ImmutableMap.of("id", "name", "id2", "name2")))
        .when(this.table).getItem(any(GetItemSpec.class));

    List<String> categoryIds = this.groupsManager
        .getAllCategoryIds("groupId", this.metrics);

    assertEquals(categoryIds.size(), 2);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getAllCategoryIds_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    List<String> categoryIds = this.groupsManager
        .getAllCategoryIds("groupId", this.metrics);

    assertEquals(categoryIds.size(), 0);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void getAllCategoryIds_badGroupFormat_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item()).when(this.table).getItem(any(GetItemSpec.class));

    List<String> categoryIds = this.groupsManager
        .getAllCategoryIds("groupId", this.metrics);

    assertEquals(categoryIds.size(), 0);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  ////////////////////////////////////endregion
  // removeCategoryFromGroups tests //
  ////////////////////////////////////region
//  @Test
//  public void removeCategoryFromGroups_validInput_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    ResultStatus result = this.groupsManager
//        .removeCategoryFromGroups(this.goodGroupIds, this.goodCategoryId, this.metrics);
//
//    assertTrue(result.success);
//    verify(this.dynamoDB, times(2)).getTable(any(String.class));
//    verify(this.metrics, times(1)).commonClose(true);
//  }
//
//  public void removeCategoryFromGroups_emptyGroupList_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    ResultStatus result = this.groupsManager
//        .removeCategoryFromGroups(Collections.emptySet(), this.goodCategoryId, this.metrics);
//
//    assertTrue(result.success);
//    verify(this.dynamoDB, times(0)).getTable(any(String.class));
//    verify(this.metrics, times(1)).commonClose(true);
//  }
//
//  public void removeCategoryFromGroups_emptyCategoryId_failureResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    ResultStatus result = this.groupsManager
//        .removeCategoryFromGroups(this.goodGroupIds, null, this.metrics);
//
//    assertFalse(result.success);
//    verify(this.dynamoDB, times(0)).getTable(any(String.class));
//    verify(this.metrics, times(1)).commonClose(false);
//  }
//
//  @Test
//  public void removeCategoryFromGroups_noDbConnection_failureResult() {
//    doReturn(null).when(this.dynamoDB).getTable(any(String.class));
//    ResultStatus resultStatus = this.groupsManager
//        .removeCategoryFromGroups(this.goodGroupIds, this.goodCategoryId, this.metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.metrics, times(1)).commonClose(false);
//  }

  //////////////////////endregion
  // leaveGroup tests //
  //////////////////////region
  @Test
  public void leaveGroup_validInput_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      ResultStatus resultStatus = this.groupsManager
          .leaveGroup(this.leaveGroupGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void leaveGroup_activeUserIsOwner_failureResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      ResultStatus resultStatus = this.groupsManager
          .leaveGroup(this.leaveGroupBadInput, this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void leaveGroup_noDbConnection_failureResult() {
    try {
      doReturn(null).when(this.dynamoDB).getTable(any(String.class));

      ResultStatus resultStatus = this.groupsManager
          .leaveGroup(this.leaveGroupGoodInput, this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(0)).getItem(any(GetItemSpec.class));
      verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void leaveGroup_missingKeys_failureResult() {
    ResultStatus resultStatus = this.groupsManager.leaveGroup(Collections.emptyMap(), metrics);
    assertFalse(resultStatus.success);

    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  ///////////////////////endregion
  // rejoinGroup tests //
  ///////////////////////region
  @Test
  public void rejoinGroup_validInput_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("openGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));
      doReturn(JsonUtils.getItemFromFile("edmond2.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));

      ResultStatus resultStatus = this.groupsManager
          .rejoinGroup(this.rejoinGroupGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void rejoinGroup_userHasNotLeftGroup_failureResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("openGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));

      ResultStatus resultStatus = this.groupsManager
          .rejoinGroup(this.rejoinGroupBadInput, this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void rejoinGroup_discrepancyBetweenGroupsLeftAndMembersLeft_failureResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("openGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));
      doReturn(JsonUtils.getItemFromFile("edmond2.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey(any(String.class));

      ResultStatus resultStatus = this.groupsManager
          .rejoinGroup(this.rejoinGroupBadInput, this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void rejoinGroup_noDbConnection_failureResult() {
    try {
      doReturn(null).when(this.dynamoDB).getTable(any(String.class));

      ResultStatus resultStatus = this.groupsManager
          .rejoinGroup(this.rejoinGroupGoodInput, this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(0)).getItem(any(GetItemSpec.class));
      verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void rejoinGroup_missingKeys_failureResult() {
    ResultStatus resultStatus = this.groupsManager.leaveGroup(Collections.emptyMap(), metrics);
    assertFalse(resultStatus.success);

    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  ///////////////////endregion
  // voteForChoice //
  ///////////////////region

  private final Map<String, Object> voteForChoiceGoodInput = new HashMap<>(ImmutableMap.of(
      GroupsManager.GROUP_ID, "b702b849-d981-4b74-bb93-18819a2c1156",
      RequestFields.EVENT_ID, "593e9c8e-a713-40ff-ae55-5d5bdfe940f4", // in consider
      RequestFields.CHOICE_ID, "3", // Moes
      RequestFields.VOTE_VALUE, 1,
      RequestFields.ACTIVE_USER, "john_andrews12"
  ));

  @Test
  public void voteForChoice_validInput_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey("john_andrews12");

      final ResultStatus resultStatus = this.groupsManager
          .voteForChoice(this.voteForChoiceGoodInput, this.metrics);

      assertTrue(resultStatus.success);

      //here we're making sure the correct vote value gets set
      final ArgumentCaptor<UpdateItemSpec> argument = ArgumentCaptor.forClass(UpdateItemSpec.class);
      verify(this.table).updateItem(argument.capture());
      assertEquals(new BigDecimal(1), argument.getValue().getValueMap().get(":voteValue"));

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
  public void voteForChoice_validInputVoteValueOutOfRange_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey("john_andrews12");

      this.voteForChoiceGoodInput.put(RequestFields.VOTE_VALUE, 8);
      final ResultStatus resultStatus = this.groupsManager
          .voteForChoice(this.voteForChoiceGoodInput, this.metrics);

      assertTrue(resultStatus.success);

      //here we're making sure the correct vote value gets set
      final ArgumentCaptor<UpdateItemSpec> argument = ArgumentCaptor.forClass(UpdateItemSpec.class);
      verify(this.table).updateItem(argument.capture());
      assertEquals(new BigDecimal(0), argument.getValue().getValueMap().get(":voteValue"));

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
  public void voteForChoice_invalidInputUserNotInGroup_failureResult() {
    try {
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap()).when(this.usersManager)
          .getMapByPrimaryKey("john_andrews12");

      this.voteForChoiceGoodInput.put(GroupsManager.GROUP_ID, "not_a_real_group_id");
      final ResultStatus resultStatus = this.groupsManager
          .voteForChoice(this.voteForChoiceGoodInput, this.metrics);

      assertFalse(resultStatus.success);

      verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void voteForChoice_missingRequestKey_failureResult() {
    final ResultStatus resultStatus = this.groupsManager
        .voteForChoice(Collections.emptyMap(), this.metrics);

    assertFalse(resultStatus.success);

    verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void voteForChoice_noDbConnection_failureResult() {
    try {
      doReturn(null).when(this.usersManager).getMapByPrimaryKey("john_andrews12");

      this.voteForChoiceGoodInput.put(GroupsManager.GROUP_ID, "not_a_real_group_id");
      final ResultStatus resultStatus = this.groupsManager
          .voteForChoice(this.voteForChoiceGoodInput, this.metrics);

      assertFalse(resultStatus.success);

      verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  //////////////////////////////////endregion
  // handleGetBatchOfEvents tests //
  //////////////////////////////////region

  @Test
  public void handleGetBatchOfEvents_validInput_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      final ResultStatus resultStatus = this.groupsManager
          .handleGetBatchOfEvents(this.handleGetBatchOfEventsGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class));
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
    } catch (Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void handleGetBatchOfEvents_batchIndexTooLarge_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      this.handleGetBatchOfEventsGoodInput.replace(RequestFields.BATCH_NUMBER, 10000);
      final ResultStatus resultStatus = this.groupsManager
          .handleGetBatchOfEvents(this.handleGetBatchOfEventsGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      assertEquals(resultStatus.resultMessage, "{}"); // empty map
      verify(this.dynamoDB, times(1)).getTable(any(String.class));
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
    } catch (Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void handleGetBatchOfEvents_userNotInGroup_failureResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      this.handleGetBatchOfEventsGoodInput.replace(RequestFields.ACTIVE_USER, "notIntGroup");

      final ResultStatus resultStatus = this.groupsManager
          .handleGetBatchOfEvents(this.handleGetBatchOfEventsGoodInput, this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class));
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(1)).commonClose(false);
    } catch (Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void handleGetBatchOfEvents_missingKeys_failureResult() {
    final ResultStatus resultStatus = this.groupsManager
        .handleGetBatchOfEvents(Collections.emptyMap(), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void handleGetBatchOfEvents_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    final ResultStatus resultStatus = this.groupsManager
        .handleGetBatchOfEvents(this.handleGetBatchOfEventsGoodInput, this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  //endregion
}
