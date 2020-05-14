package handlers;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import models.Group;
import utilities.RequestFields;

public class GetGroupHandlerTest {


  private final Map<String, Object> getGroupGoodInput = new HashMap<>(ImmutableMap.of(
      RequestFields.ACTIVE_USER, "john_andrews12",
      Group.GROUP_ID, "groupId",
      RequestFields.BATCH_NUMBER, 1
  ));

//  @Test
//  public void getGroup_validInput_successfulResult() {
//    try {
//      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
//          .getItem(any(GetItemSpec.class));
//
//      final ResultStatus resultStatus = this.groupsManager
//          .getGroup(this.getGroupGoodInput, this.metrics);
//
//      assertTrue(resultStatus.success);
//      verify(this.dynamoDB, times(1)).getTable(any(String.class));
//      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//      verify(this.metrics, times(1)).commonClose(true);
//      verify(this.metrics, times(0)).commonClose(false);
//    } catch (final Exception e) {
//      System.out.println(e);
//      fail();
//    }
//  }
//
//  @Test
//  public void getGroup_invalidInputNotAMember_failureResult() {
//    try {
//      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//      doReturn(JsonUtils.getItemFromFile("bigGroup.json")).when(this.table)
//          .getItem(any(GetItemSpec.class));
//
//      this.getGroupGoodInput.put(RequestFields.ACTIVE_USER, "not_a_member");
//      final ResultStatus resultStatus = this.groupsManager
//          .getGroup(this.getGroupGoodInput, this.metrics);
//
//      assertFalse(resultStatus.success);
//      verify(this.dynamoDB, times(1)).getTable(any(String.class));
//      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//      verify(this.metrics, times(0)).commonClose(true);
//      verify(this.metrics, times(1)).commonClose(false);
//    } catch (final Exception e) {
//      System.out.println(e);
//      fail();
//    }
//  }
//
//  @Test
//  public void getGroup_missingRequestKeys_failureResult() {
//    final ResultStatus resultStatus = this.groupsManager
//        .getGroup(Collections.emptyMap(), this.metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.dynamoDB, times(0)).getTable(any(String.class));
//    verify(this.metrics, times(0)).commonClose(true);
//    verify(this.metrics, times(1)).commonClose(false);
//  }
//
//  @Test
//  public void getGroup_noDbConnection_failureResult() {
//    doReturn(null).when(this.dynamoDB).getTable(any(String.class));
//
//    this.getGroupGoodInput.put(RequestFields.ACTIVE_USER, "not_a_member");
//    final ResultStatus resultStatus = this.groupsManager
//        .getGroup(this.getGroupGoodInput, this.metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.metrics, times(0)).commonClose(true);
//    verify(this.metrics, times(1)).commonClose(false);
//  }
}
