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
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private final Set<String> goodGroupIds = ImmutableSet.of("id1", "id2");

  private final ImmutableMap<String, Object> newEventGoodInput = ImmutableMap.<String, Object>builder()
      .put(RequestFields.ACTIVE_USER, "ActiveUser")
      .put(GroupsManager.EVENT_NAME, "EventName")
      .put(GroupsManager.CATEGORY_ID, "CategoryId")
      .put(GroupsManager.CATEGORY_NAME, "CategoryName")
      .put(GroupsManager.CREATED_DATE_TIME, "CreatedDateTime")
      .put(GroupsManager.EVENT_START_DATE_TIME, "EventStartDateTime")
      .put(GroupsManager.VOTING_DURATION, 50)
      .put(GroupsManager.RSVP_DURATION, 50)
      .put(GroupsManager.GROUP_ID, "GroupId")
      .build();

  private final ImmutableMap<String, Object> leaveGroupGoodInput = ImmutableMap.<String, Object>builder()
      .put(RequestFields.ACTIVE_USER, "ActiveUser")
      .put(GroupsManager.GROUP_ID, "GroupId")
      .build();

  private final ImmutableMap<String, Object> deleteGroupGoodInput = ImmutableMap.<String, Object>builder()
      .put(RequestFields.ACTIVE_USER, "ActiveUser")
      .put(GroupsManager.GROUP_ID, "GroupId")
      .build();

  private final Item deleteGroupItem = new Item()
      .withMap(GroupsManager.CATEGORIES, ImmutableMap.of("categoryId1", "categoryName1"))
      .withMap(GroupsManager.MEMBERS, ImmutableMap.of("username1",
          ImmutableMap.of(UsersManager.DISPLAY_NAME, "displayName1", UsersManager.ICON, "icon1")))
      .withMap(GroupsManager.MEMBERS_LEFT, ImmutableMap.of("username2",
          ImmutableMap.of(UsersManager.DISPLAY_NAME, "displayName2", UsersManager.ICON, "icon2")))
      .withString(GroupsManager.GROUP_CREATOR, "ActiveUser");

  private final Map<String, Object> badInput = new HashMap<>();

  private final Map<String, Object> newEventBadInput = Maps
      .newHashMap(ImmutableMap.<String, Object>builder()
          .put(RequestFields.ACTIVE_USER, "ActiveUser")
          .put(GroupsManager.EVENT_NAME, "EventName")
          .put(GroupsManager.CATEGORY_ID, "CategoryId")
          .put(GroupsManager.CATEGORY_NAME, "CategoryName")
          .put(GroupsManager.CREATED_DATE_TIME, "CreatedDateTime")
          .put(GroupsManager.EVENT_START_DATE_TIME, "EventStartDateTime")
          .put(GroupsManager.VOTING_DURATION, 50)
          .put(GroupsManager.RSVP_DURATION, 50)
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
  private Metrics metrics;

  @BeforeEach
  private void init() {
    this.groupsManager = new GroupsManager(this.dynamoDB);

    DatabaseManagers.CATEGORIES_MANAGER = this.categoriesManager;
    DatabaseManagers.USERS_MANAGER = this.usersManager;
    DatabaseManagers.GROUPS_MANAGER = this.groupsManager;
    DatabaseManagers.PENDING_EVENTS_MANAGER = this.pendingEventsManager;
  }

  ////////////////////
  // getGroup tests //
  ////////////////////region

  //////////////////////////endregion
  // createNewGroup tests //
  //////////////////////////region

  /////////////////////endregion
  // editGroup tests //
  /////////////////////region

  ///////////////////////endregion
  // deleteGroup tests //
  ///////////////////////region
  @Test
  public void deleteGroup_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new ResultStatus(true, "usersManagerWorks")).when(this.usersManager)
        .removeGroupFromUsers(any(Set.class), any(Set.class), any(String.class), any(Metrics.class));
    doReturn(new ResultStatus(true, "categoriesManagerWorks")).when(this.categoriesManager)
        .removeGroupFromCategories(any(Set.class), any(String.class), any(Metrics.class));
    doReturn(this.deleteGroupItem).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.groupsManager
        .deleteGroup(this.deleteGroupGoodInput, metrics);

    assertTrue(resultStatus.success);
    verify(this.usersManager, times(1))
        .removeGroupFromUsers(any(Set.class), any(Set.class), any(String.class), any(Metrics.class));
    verify(this.categoriesManager, times(1))
        .removeGroupFromCategories(any(Set.class), any(String.class), any(Metrics.class));
    verify(this.dynamoDB, times(2)).getTable(any(String.class));
    verify(this.table, times(1)).deleteItem(any(DeleteItemSpec.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
  }

  @Test
  public void deleteGroup_validInputUsersTableError_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new ResultStatus(false, "usersManagerFails")).when(this.usersManager)
        .removeGroupFromUsers(any(Set.class), any(Set.class), any(String.class), any(Metrics.class));
    doReturn(new ResultStatus(true, "categoriesManagerWorks")).when(this.categoriesManager)
        .removeGroupFromCategories(any(Set.class), any(String.class), any(Metrics.class));
    doReturn(this.deleteGroupItem).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.groupsManager
        .deleteGroup(this.deleteGroupGoodInput, metrics);

    assertFalse(resultStatus.success);
    verify(this.usersManager, times(1))
        .removeGroupFromUsers(any(Set.class), any(Set.class), any(String.class), any(Metrics.class));
    verify(this.categoriesManager, times(1))
        .removeGroupFromCategories(any(Set.class), any(String.class), any(Metrics.class));
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
  }

  @Test
  public void deleteGroup_validInputCategoriesTableError_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new ResultStatus(true, "usersManagerWorks")).when(this.usersManager)
        .removeGroupFromUsers(any(Set.class), any(Set.class), any(String.class), any(Metrics.class));
    doReturn(new ResultStatus(false, "categoriesManagerFails")).when(this.categoriesManager)
        .removeGroupFromCategories(any(Set.class), any(String.class), any(Metrics.class));
    doReturn(this.deleteGroupItem).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.groupsManager
        .deleteGroup(this.deleteGroupGoodInput, metrics);

    assertFalse(resultStatus.success);
    verify(this.usersManager, times(1))
        .removeGroupFromUsers(any(Set.class), any(Set.class), any(String.class), any(Metrics.class));
    verify(this.categoriesManager, times(1))
        .removeGroupFromCategories(any(Set.class), any(String.class), any(Metrics.class));
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
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
    ResultStatus resultStatus = this.groupsManager
        .deleteGroup(this.badInput, metrics);
    assertFalse(resultStatus.success);

    this.badInput.put(RequestFields.ACTIVE_USER, "ActiveUser");

    resultStatus = this.groupsManager.deleteGroup(this.badInput, metrics);
    assertFalse(resultStatus.success);

    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.metrics, times(2)).commonClose(false);
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
//  @Test
//  public void newEvent_validInput_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(new Item().withMap(GroupsManager.MEMBERS, ImmutableMap.of("user1", "name1"))).when(this.table)
//        .getItem(any(GetItemSpec.class));
//
//    ResultStatus result = this.groupsManager
//        .newEvent(this.newEventGoodInput, this.metrics);
//
//    assertTrue(result.success);
//    verify(this.dynamoDB, times(2)).getTable(any(String.class));
//    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(true);
//  }
//
//  @Test
//  public void newEvent_missingRequestKeys_failureResult() {
//    this.newEventBadInput.remove(GroupsManager.GROUP_ID);
//
//    ResultStatus result = this.groupsManager
//        .newEvent(this.newEventBadInput, this.metrics);
//
//    assertFalse(result.success);
//    verify(this.dynamoDB, times(0)).getTable(any(String.class));
//    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(false);
//  }
//
//  @Test
//  public void newEvent_badInput_failureResult() {
//    this.newEventBadInput.put(GroupsManager.VOTING_DURATION, "General Kenobi!");
//
//    ResultStatus result = this.groupsManager
//        .newEvent(this.newEventBadInput, this.metrics);
//
//    assertFalse(result.success);
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(false);
//  }
//
//  @Test
//  public void newEvent_groupNotFound_failureResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(null).when(this.table).getItem(any(GetItemSpec.class));
//
//    ResultStatus result = this.groupsManager
//        .newEvent(this.newEventGoodInput, this.metrics);
//
//    assertFalse(result.success);
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(false);
//  }

//  @Test
//  public void newEvent_missingMembersField_failureResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(new Item().withMap("NoMembers", ImmutableMap.of("user1", "name1"))).when(this.table)
//        .getItem(any(GetItemSpec.class));
//
//    ResultStatus result = this.groupsManager
//        .newEvent(this.newEventGoodInput, this.metrics);
//
//    assertFalse(result.success);
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(false);
//  }

  @Test
  public void newEvent_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.groupsManager
        .newEvent(this.newEventGoodInput, this.metrics);

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
  // editGroupInputIsValid tests //
  ////////////////////////////region

  /////////////////////////////////endregion
  // validEventInput tests //
  /////////////////////////////////region
//  @Test
//  public void validEventInput_validInput_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(new Item().withMap(GroupsManager.MEMBERS, ImmutableMap.of("user1", "name1"))).when(this.table)
//        .getItem(any(GetItemSpec.class));
//
//    ResultStatus result = this.groupsManager
//        .newEvent(this.newEventGoodInput, this.metrics);
//    assertTrue(result.success);
//  }
/*
  @Test
  public void validEventInput_emptyString_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(GroupsManager.MEMBERS, ImmutableMap.of("user1", "name1")))
        .when(this.table)
        .getItem(any(GetItemSpec.class));

    this.newEventBadInput.put(GroupsManager.GROUP_ID, "");
    this.newEventBadInput.put(GroupsManager.CATEGORY_ID, "");
    ResultStatus result = this.groupsManager
        .newEvent(this.newEventBadInput, this.metrics);
    assertFalse(result.success);

    this.newEventBadInput.put(GroupsManager.GROUP_ID, "GroupId");
    this.newEventBadInput.put(GroupsManager.CATEGORY_ID, "");
    result = this.groupsManager.newEvent(this.newEventBadInput, this.metrics);
    assertFalse(result.success);
  }

  @Test
  public void validEventInput_invalidVotingDuration_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(GroupsManager.MEMBERS, ImmutableMap.of("user1", "name1")))
        .when(this.table).getItem(any(GetItemSpec.class));

    this.newEventBadInput.put(GroupsManager.VOTING_DURATION, -1);
    ResultStatus result = this.groupsManager
        .newEvent(this.newEventBadInput, this.metrics);
    assertFalse(result.success);

    this.newEventBadInput.put(GroupsManager.VOTING_DURATION, 1000000);
    result = this.groupsManager.newEvent(this.newEventBadInput, this.metrics);
    assertFalse(result.success);
  }

  @Test
  public void validEventInput_invalidRsvpDuration_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(GroupsManager.MEMBERS, ImmutableMap.of("user1", "name1")))
        .when(this.table).getItem(any(GetItemSpec.class));

    this.newEventBadInput.put(GroupsManager.RSVP_DURATION, -1);
    ResultStatus result = this.groupsManager
        .newEvent(this.newEventBadInput, this.metrics);
    assertFalse(result.success);

    this.newEventBadInput.put(GroupsManager.RSVP_DURATION, 1000000);
    result = this.groupsManager.newEvent(this.newEventBadInput, this.metrics);
    assertFalse(result.success);
  }
*/
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
  @Test
  public void removeCategoryFromGroups_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    ResultStatus result = this.groupsManager
        .removeCategoryFromGroups(this.goodGroupIds, this.goodCategoryId, this.metrics);

    assertTrue(result.success);
    verify(this.dynamoDB, times(2)).getTable(any(String.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  public void removeCategoryFromGroups_emptyGroupList_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    ResultStatus result = this.groupsManager
        .removeCategoryFromGroups(Collections.emptySet(), this.goodCategoryId, this.metrics);

    assertTrue(result.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  public void removeCategoryFromGroups_emptyCategoryId_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    ResultStatus result = this.groupsManager
        .removeCategoryFromGroups(this.goodGroupIds, null, this.metrics);

    assertFalse(result.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void removeCategoryFromGroups_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));
    ResultStatus resultStatus = this.groupsManager
        .removeCategoryFromGroups(this.goodGroupIds, this.goodCategoryId, this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  /////////////////////////////endregion
  // leaveGroup tests //
  /////////////////////////////region
  @Test
  public void leaveGroup_missingKeys_failureResult() {
    ResultStatus resultStatus = this.groupsManager
        .leaveGroup(this.badInput, metrics);
    assertFalse(resultStatus.success);

    this.badInput.put(RequestFields.ACTIVE_USER, "testId");

    resultStatus = this.groupsManager.leaveGroup(this.badInput, metrics);
    assertFalse(resultStatus.success);

    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.metrics, times(2)).commonClose(false);
  }

  /////////////////////////////endregion
  // rejoinGroup tests //
  /////////////////////////////region
  @Test
  public void rejoinGroup_missingKeys_failureResult() {
    ResultStatus resultStatus = this.groupsManager
        .leaveGroup(this.badInput, metrics);
    assertFalse(resultStatus.success);

    this.badInput.put(RequestFields.ACTIVE_USER, "testId");

    resultStatus = this.groupsManager.rejoinGroup(this.badInput, metrics);
    assertFalse(resultStatus.success);

    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.metrics, times(2)).commonClose(false);
  }
}
