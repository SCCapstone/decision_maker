package handlers;

public class DeleteCategoryHandlerTest {

//  private final Map<String, Object> deleteCategoryGoodInput = Maps.newHashMap(ImmutableMap.of(
//      CategoriesManager.CATEGORY_ID, "CategoryId",
//      RequestFields.ACTIVE_USER, "ActiveUser"
//  ));

//  @Test
//  public void deleteCategory_validInputWithGroups_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(new Item().withMap(CategoriesManager.GROUPS, ImmutableMap.of("groupId1", "groupName1"))
//        .withString(CategoriesManager.OWNER, "ActiveUser")).when(this.table)
//        .getItem(any(GetItemSpec.class));
//
//    ResultStatus resultStatus = this.categoriesManager
//        .deleteCategory(this.deleteCategoryGoodInput, metrics);
//
//    assertTrue(resultStatus.success);
//    verify(this.groupsManager, times(1))
//        .removeCategoryFromGroups(any(Set.class), any(String.class), any(Metrics.class));
//    verify(this.dynamoDB, times(2)).getTable(any(String.class));
//    verify(this.table, times(1)).deleteItem(any(DeleteItemSpec.class));
//    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//  }
//
//  @Test
//  public void deleteCategory_validInputWithOutGroups_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(new Item().withMap(CategoriesManager.GROUPS, ImmutableMap.of())
//        .withString(CategoriesManager.OWNER, "ActiveUser"))
//        .when(this.table).getItem(any(GetItemSpec.class));
//
//    ResultStatus resultStatus = this.categoriesManager
//        .deleteCategory(this.deleteCategoryGoodInput, metrics);
//
//    assertTrue(resultStatus.success);
//    verify(this.groupsManager, times(1))
//        .removeCategoryFromGroups(any(Set.class), any(String.class), any(Metrics.class));
//    verify(this.dynamoDB, times(2)).getTable(any(String.class));
//    verify(this.table, times(1)).deleteItem(any(DeleteItemSpec.class));
//    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//  }
//
//  @Test
//  public void deleteCategory_noDbConnection_failureResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(new Item().withString(CategoriesManager.OWNER, "BadUser"))
//        .when(this.table).getItem(any(GetItemSpec.class));
//
//    ResultStatus resultStatus = this.categoriesManager
//        .deleteCategory(this.editCategoryGoodInput, metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.groupsManager, times(0))
//        .removeCategoryFromGroups(any(Set.class), any(String.class), any(Metrics.class));
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.table, times(0)).deleteItem(any(DeleteItemSpec.class));
//    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//  }
//
//  @Test
//  public void deleteCategory_activeUserIsNotOwner_failureResult() {
//    doReturn(null).when(this.dynamoDB).getTable(any(String.class));
//
//    ResultStatus resultStatus = this.categoriesManager
//        .deleteCategory(this.editCategoryGoodInput, metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.groupsManager, times(0))
//        .removeCategoryFromGroups(any(Set.class), any(String.class), any(Metrics.class));
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.table, times(0)).deleteItem(any(DeleteItemSpec.class));
//  }
//
//  @Test
//  public void deleteCategory_missingKey_failureResult() {
//    ResultStatus resultStatus = this.categoriesManager
//        .deleteCategory(this.badInput, metrics);
//    assertFalse(resultStatus.success);
//
//    this.badInput.put(CategoriesManager.CATEGORY_ID, "testId");
//    resultStatus = this.categoriesManager.deleteCategory(this.badInput, metrics);
//    assertFalse(resultStatus.success);
//
//    verify(this.dynamoDB, times(0)).getTable(any(String.class));
//    verify(this.table, times(0)).deleteItem(any(DeleteItemSpec.class));
//  }
}
