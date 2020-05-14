package handlers;

public class GetCategoryHandlerTest {

//  private final Map<String, Object> getCategoriesGoodInputActiveUser = Maps
//      .newHashMap(ImmutableMap.of(
//          RequestFields.ACTIVE_USER, "ActiveUser"
//      ));
//
//  private final Map<String, Object> getCategoriesGoodInputCategoryIds = Maps
//      .newHashMap(ImmutableMap.of(
//          RequestFields.CATEGORY_IDS, ImmutableList.of("catId1", "catId2")
//      ));
//
//  private final Map<String, Object> getCategoriesGoodInputGroupId = Maps.newHashMap(ImmutableMap.of(
//      GroupsManager.GROUP_ID, "groupId"
//  ));

//  @Test
//  public void getCategories_validInputActiveUser_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(new ArrayList<>()).when(this.usersManager)
//        .getAllOwnedCategoryIds(any(String.class), any(Metrics.class));
//    doReturn(ImmutableList.of("groupId1", "groupId2")).when(this.usersManager)
//        .getAllGroupIds(any(String.class), any(Metrics.class));
//    doReturn(ImmutableList.of("cat1"), ImmutableList.of("cat2")).when(this.groupsManager)
//        .getAllCategoryIds(any(String.class), eq(this.metrics));
//
//    ResultStatus resultStatus = this.categoriesManager
//        .getCategories(this.getCategoriesGoodInputActiveUser, this.metrics);
//
//    assertTrue(resultStatus.success);
//    verify(this.usersManager, times(1))
//        .getAllOwnedCategoryIds(any(String.class), any(Metrics.class));
//    verify(this.dynamoDB, times(2)).getTable(
//        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
//    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(true);
//  }
//
//  @Test
//  public void getCategories_validInputActiveUserNoGroups_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(ImmutableList.of("catId1", "catId2")).when(this.usersManager)
//        .getAllOwnedCategoryIds(any(String.class), any(Metrics.class));
//
//    ResultStatus resultStatus = this.categoriesManager
//        .getCategories(this.getCategoriesGoodInputActiveUser, this.metrics);
//
//    assertTrue(resultStatus.success);
//    verify(this.usersManager, times(1))
//        .getAllOwnedCategoryIds(any(String.class), any(Metrics.class));
//    verify(this.dynamoDB, times(2)).getTable(
//        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
//    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(true);
//  }
//
//  @Test
//  public void getCategories_validInputCategoryIds_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(null, new Item()).when(this.table).getItem(any(GetItemSpec.class));
//
//    ResultStatus resultStatus = this.categoriesManager
//        .getCategories(this.getCategoriesGoodInputCategoryIds, this.metrics);
//
//    assertTrue(resultStatus.success);
//    assertEquals(resultStatus.resultMessage, "[{}]");
//    verify(this.usersManager, times(0))
//        .getAllOwnedCategoryIds(any(String.class), any(Metrics.class));
//    verify(this.groupsManager, times(0))
//        .getAllCategoryIds(any(String.class), any(Metrics.class));
//    verify(this.dynamoDB, times(2)).getTable(
//        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
//    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(true);
//  }
//
//  @Test
//  public void getCategories_validInputGroupId_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(GroupsManager.GROUP_ID).when(this.groupsManager).getPrimaryKeyIndex();
//    doReturn(ImmutableList.of("catId1", "catId2")).when(this.groupsManager)
//        .getAllCategoryIds(any(String.class), any(Metrics.class));
//
//    ResultStatus resultStatus = this.categoriesManager
//        .getCategories(this.getCategoriesGoodInputGroupId, this.metrics);
//
//    assertTrue(resultStatus.success);
//    verify(this.groupsManager, times(1))
//        .getAllCategoryIds(any(String.class), any(Metrics.class));
//    verify(this.dynamoDB, times(2)).getTable(
//        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
//    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(true);
//  }
//
//  @Test
//  public void getCategories_noDbConnection_successfulResult() {
//    doReturn(null).when(this.dynamoDB).getTable(any(String.class));
//
//    ResultStatus resultStatus = this.categoriesManager
//        .getCategories(this.getCategoriesGoodInputCategoryIds, this.metrics);
//
//    assertTrue(resultStatus.success);
//    assertEquals(resultStatus.resultMessage,
//        "[]"); // we failed gracefully and returned an empty list
//    verify(this.dynamoDB, times(2)).getTable(any(String.class));
//    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(true);
//  }
//
//  @Test
//  public void getCategories_missingKey_failureResult() {
//    ResultStatus resultStatus = this.categoriesManager
//        .getCategories(this.badInput, this.metrics);
//    assertFalse(resultStatus.success);
//
//    verify(this.usersManager, times(0))
//        .getAllOwnedCategoryIds(any(String.class), any(Metrics.class));
//    verify(this.groupsManager, times(0))
//        .getAllCategoryIds(any(String.class), any(Metrics.class));
//    verify(this.dynamoDB, times(0)).getTable(any(String.class));
//    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(false);
//  }
}
