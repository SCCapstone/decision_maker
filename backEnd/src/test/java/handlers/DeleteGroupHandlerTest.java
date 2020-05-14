package handlers;

import static junit.framework.TestCase.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.google.common.collect.Sets;
import java.util.Collections;
import models.Group;
import org.junit.jupiter.api.Test;
import utilities.JsonUtils;
import utilities.ResultStatus;

public class DeleteGroupHandlerTest {
  ///////////////////////endregion
  // deleteGroup tests //
  ///////////////////////region
//  @Test
//  public void deleteGroup_validInput_successfulResult() {
//    try {
//      final Group groupToDelete = new Group(JsonUtils.getItemFromFile("openGroup.json").asMap());
//      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//      doReturn(new ResultStatus(true, "usersManagerWorks")).when(this.usersManager)
//          .removeGroupsLeftFromUsers(groupToDelete.getMembersLeft().keySet(),
//              groupToDelete.getGroupId(), this.metrics);
//      doReturn(JsonUtils.getItemFromFile("johnplaysgolf.json").asMap()).when(this.usersManager)
//          .getMapByPrimaryKey(any(String.class));
//      doReturn(new ResultStatus(true, "categoriesManagerWorks")).when(this.categoriesManager)
//          .removeGroupFromCategories(groupToDelete.getCategories().keySet(),
//              groupToDelete.getGroupId(), this.metrics);
//      doReturn(groupToDelete.asItem()).when(this.table)
//          .getItem(any(GetItemSpec.class));
//
//      final ResultStatus resultStatus = this.groupsManager
//          .deleteGroup(this.deleteGroupGoodInput, metrics);
//
//      assertTrue(resultStatus.success);
//      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
//      verify(this.table, times(1)).deleteItem(any(DeleteItemSpec.class));
//      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//      verify(this.metrics, times(2)).commonClose(true);
//      verify(this.metrics, times(0)).commonClose(false);
//    } catch (final Exception e) {
//      System.out.println(e);
//      fail();
//    }
//  }
//
//  @Test
//  public void deleteGroup_validInputUsersTableError_failureResult() {
//    try {
//      final Group groupToDelete = new Group(JsonUtils.getItemFromFile("openGroup.json").asMap());
//      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//      doReturn(new ResultStatus(false, "usersManagerFails")).when(this.usersManager)
//          .removeGroupsLeftFromUsers(groupToDelete.getMembersLeft().keySet(),
//              groupToDelete.getGroupId(), this.metrics);
//      doReturn(new ResultStatus(true, "categoriesManagerWorks")).when(this.categoriesManager)
//          .removeGroupFromCategories(groupToDelete.getCategories().keySet(),
//              groupToDelete.getGroupId(), this.metrics);
//      doReturn(groupToDelete.asItem()).when(this.table)
//          .getItem(any(GetItemSpec.class));
//
//      final ResultStatus resultStatus = this.groupsManager
//          .deleteGroup(this.deleteGroupGoodInput, metrics);
//
//      assertFalse(resultStatus.success);
//      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
//      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//      verify(this.metrics, times(1)).commonClose(true);
//      verify(this.metrics, times(1)).commonClose(false);
//    } catch (final Exception e) {
//      System.out.println(e);
//      fail();
//    }
//  }
//
//  @Test
//  public void deleteGroup_validInputCategoriesTableError_failureResult() {
//    try {
//      final Group groupToDelete = new Group(JsonUtils.getItemFromFile("openGroup.json").asMap());
//      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//      doReturn(new ResultStatus(true, "usersManagerWorks")).when(this.usersManager)
//          .removeGroupsLeftFromUsers(groupToDelete.getMembersLeft().keySet(),
//              groupToDelete.getGroupId(), this.metrics);
//      doReturn(new ResultStatus(false, "categoriesManagerFails")).when(this.categoriesManager)
//          .removeGroupFromCategories(groupToDelete.getCategories().keySet(),
//              groupToDelete.getGroupId(), this.metrics);
//      doReturn(JsonUtils.getItemFromFile("john_andrews12.json").asMap(),
//          JsonUtils.getItemFromFile("edmond2.json").asMap()).when(this.usersManager)
//          .getMapByPrimaryKey(any(String.class));
//      doReturn(groupToDelete.asItem()).when(this.table)
//          .getItem(any(GetItemSpec.class));
//
//      final ResultStatus resultStatus = this.groupsManager
//          .deleteGroup(this.deleteGroupGoodInput, metrics);
//
//      assertFalse(resultStatus.success);
//      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
//      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//      verify(this.metrics, times(1)).commonClose(true);
//      verify(this.metrics, times(1)).commonClose(false);
//    } catch (final Exception e) {
//      System.out.println(e);
//      fail();
//    }
//  }
//
//  @Test
//  public void deleteGroup_validInputDeleteUsersError_failureResult() {
//    try {
//      final Group groupToDelete = new Group(JsonUtils.getItemFromFile("openGroup.json").asMap());
//      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//      doReturn(new ResultStatus(true, "usersManagerWorks")).when(this.usersManager)
//          .removeGroupsLeftFromUsers(groupToDelete.getMembersLeft().keySet(),
//              groupToDelete.getGroupId(), this.metrics);
//      doReturn(new ResultStatus(true, "categoriesManagerWorks")).when(this.categoriesManager)
//          .removeGroupFromCategories(groupToDelete.getCategories().keySet(),
//              groupToDelete.getGroupId(), this.metrics);
//      doReturn(null).when(this.usersManager).getMapByPrimaryKey(any(String.class));
//      doReturn(groupToDelete.asItem()).when(this.table)
//          .getItem(any(GetItemSpec.class));
//
//      final ResultStatus resultStatus = this.groupsManager
//          .deleteGroup(this.deleteGroupGoodInput, metrics);
//
//      assertFalse(resultStatus.success);
//      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
//      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//      verify(this.metrics, times(0)).commonClose(true);
//      verify(this.metrics, times(2)).commonClose(false);
//    } catch (final Exception e) {
//      System.out.println(e);
//      fail();
//    }
//  }
//
//  @Test
//  public void deleteGroup_userIsNotGroupCreator_failureResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(new Item().withString(GroupsManager.GROUP_CREATOR, "InvalidUser")).when(this.table)
//        .getItem(any(GetItemSpec.class));
//
//    ResultStatus resultStatus = this.groupsManager.deleteGroup(this.deleteGroupGoodInput, metrics);
//    assertFalse(resultStatus.success);
//
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.metrics, times(1)).commonClose(false);
//  }
//
//
//  @Test
//  public void deleteGroup_missingKeys_failureResult() {
//    ResultStatus resultStatus = this.groupsManager.deleteGroup(Collections.emptyMap(), metrics);
//    assertFalse(resultStatus.success);
//
//    verify(this.dynamoDB, times(0)).getTable(any(String.class));
//    verify(this.metrics, times(1)).commonClose(false);
//  }
//
//  @Test
//  public void deleteGroup_noDbConnection_failureResult() {
//    doReturn(null).when(this.dynamoDB).getTable(any(String.class));
//
//    ResultStatus resultStatus = this.groupsManager
//        .deleteGroup(this.deleteGroupGoodInput, this.metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.metrics, times(1)).commonClose(false);
//  }

  ////////////////////////////////endregion
  // removeGroupFromUsers tests //
  ////////////////////////////////region
//  @Test
//  public void removeGroupFromUsers_validInput_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//
//    ResultStatus resultStatus = this.usersManager
//        .removeGroupsLeftFromUsers(Sets.newHashSet("User3", "User4"), "GroupId1", this.metrics);
//
//    assertTrue(resultStatus.success);
//    verify(this.dynamoDB, times(2)).getTable(any(String.class));
//    verify(this.table, times(2)).updateItem(any(UpdateItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(true);
//  }
//
//  @Test
//  public void removeGroupFromUsers_validInputNoMembersLeft_successfulResult() {
//    ResultStatus resultStatus = this.usersManager
//        .removeGroupsLeftFromUsers(Collections.emptySet(), "GroupId1", this.metrics);
//
//    assertTrue(resultStatus.success);
//    verify(this.dynamoDB, times(0)).getTable(any(String.class));
//    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(true);
//  }
//
//  @Test
//  public void removeGroupFromUsers_validInputDbDiesDuringUpdate_failureResult() {
//    doReturn(this.table, null).when(this.dynamoDB).getTable(any(String.class));
//
//    ResultStatus resultStatus = this.usersManager
//        .removeGroupsLeftFromUsers(Sets.newHashSet("User3", "User4"), "GroupId1", this.metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.dynamoDB, times(2)).getTable(any(String.class));
//    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(false);
//  }

//  ///////////////////////////endregion
//  // removeGroupFromCategories test //
//  ///////////////////////////region
//  @Test
//  public void removeGroupFromCategories_validInput_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//
//    ResultStatus resultStatus = this.categoriesManager
//        .removeGroupFromCategories(Sets.newHashSet("CategoryId1", "CategoryId2"), "GroupId1",
//            this.metrics);
//
//    assertTrue(resultStatus.success);
//    verify(this.dynamoDB, times(2)).getTable(any(String.class));
//    verify(this.table, times(2)).updateItem(any(UpdateItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(true);
//  }
//
//  @Test
//  public void removeGroupFromCategories_validInputDbDiesDuringUpdate_failureResult() {
//    doReturn(this.table, null).when(this.dynamoDB).getTable(any(String.class));
//
//    ResultStatus resultStatus = this.categoriesManager
//        .removeGroupFromCategories(Sets.newHashSet("User1", "User2"), "GroupId1", this.metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.dynamoDB, times(2)).getTable(any(String.class));
//    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(false);
//  }
}
