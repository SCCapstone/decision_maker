package imports;

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
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utilities.RequestFields;
import utilities.ResultStatus;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class CategoriesManagerTest {

  private final Map<String, Object> badInput = new HashMap<>();

  private final Map<String, Object> newCategoryGoodInput = ImmutableMap.of(
      CategoriesManager.CATEGORY_NAME, "CategoryName",
      CategoriesManager.CHOICES,
      ImmutableMap.of("0", ImmutableMap.of("string", "object")),
      RequestFields.USER_RATINGS,
      ImmutableMap.of("0", ImmutableMap.of("string", "object")),
      RequestFields.ACTIVE_USER, "ActiveUser"
  );

  private final Map<String, Object> editCategoryGoodInput = ImmutableMap.of(
      CategoriesManager.CATEGORY_ID, "CategoryId",
      CategoriesManager.CATEGORY_NAME, "CategoryName",
      CategoriesManager.CHOICES,
      ImmutableMap.of("-1", ImmutableMap.of("string", "object"), "0",
          ImmutableMap.of("string", "object")),
      RequestFields.USER_RATINGS,
      ImmutableMap.of("0", ImmutableMap.of("string", "object")),
      RequestFields.ACTIVE_USER, "ActiveUser"
  );

  private final Map<String, Object> deleteCategoryGoodInput = ImmutableMap.of(
      CategoriesManager.CATEGORY_ID, "CategoryId",
      RequestFields.ACTIVE_USER, "ActiveUser"
  );

  private final Map<String, Object> getCategoriesGoodInputActiveUser = ImmutableMap.of(
      RequestFields.ACTIVE_USER, "ActiveUser"
  );

  private final Map<String, Object> getCategoriesGoodInputCategoryIds = ImmutableMap.of(
      RequestFields.CATEGORY_IDS, ImmutableList.of("catId1", "catId2")
  );

  private final Map<String, Object> getCategoriesGoodInputGroupId = ImmutableMap.of(
      GroupsManager.GROUP_ID, "groupId"
  );

  private CategoriesManager categoriesManager;// = new CategoriesManager(new EnvironmentVariableCredentialsProvider());

  @Mock
  private Table table;

  @Mock
  private DynamoDB dynamoDB;

  @Mock
  private UsersManager usersManager;

  @Mock
  private GroupsManager groupsManager;

  @BeforeEach
  private void init() {
    this.categoriesManager = new CategoriesManager(this.dynamoDB);

    DatabaseManagers.CATEGORIES_MANAGER = this.categoriesManager;
    DatabaseManagers.USERS_MANAGER = this.usersManager;
    DatabaseManagers.GROUPS_MANAGER = this.groupsManager;
  }

  //////////////////////////
  // addNewCategory tests //
  //////////////////////////

  @Test
  public void addNewCategory_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new ResultStatus(true, "usersManagerWorks")).when(this.usersManager)
        .updateUserChoiceRatings(any(Map.class));

    ResultStatus resultStatus = this.categoriesManager.addNewCategory(this.newCategoryGoodInput);

    assertTrue(resultStatus.success);
    verify(this.usersManager, times(1)).updateUserChoiceRatings(any(Map.class));
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class)); // the db is hit twice, but only once by the dependency being tested
    verify(this.table, times(1)).putItem(any(PutItemSpec.class));
  }

  @Test
  public void addNewCategory_validInputBadUsers_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new ResultStatus(false, "usersManagerBroken")).when(this.usersManager)
        .updateUserChoiceRatings(any(Map.class));

    ResultStatus resultStatus = this.categoriesManager.addNewCategory(this.newCategoryGoodInput);

    assertFalse(resultStatus.success);
    verify(this.usersManager, times(1)).updateUserChoiceRatings(any(Map.class));
    //TODO we need to update the function to try to revert what it has already done maybe? -> 2 calls then
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class)); // the db is hit twice, but only once by the dependency being tested\
    verify(this.table, times(1)).putItem(any(PutItemSpec.class));
  }

  @Test
  public void addNewCategory_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.categoriesManager.addNewCategory(this.newCategoryGoodInput);

    assertFalse(resultStatus.success);
    verify(this.usersManager, times(0)).updateUserChoiceRatings(any(Map.class));
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
  }

  @Test
  public void addNewCategory_missingKey_failureResult() {
    ResultStatus resultStatus = this.categoriesManager.addNewCategory(this.badInput);
    assertFalse(resultStatus.success);

    this.badInput.put(CategoriesManager.CATEGORY_NAME, "testName");
    resultStatus = this.categoriesManager.addNewCategory(this.badInput);
    assertFalse(resultStatus.success);
    this.badInput.put(CategoriesManager.CHOICES, "testChoices");
    resultStatus = this.categoriesManager.addNewCategory(this.badInput);
    assertFalse(resultStatus.success);
    this.badInput.put(RequestFields.USER_RATINGS, "userRatings");
    resultStatus = this.categoriesManager.addNewCategory(this.badInput);
    assertFalse(resultStatus.success);

    verify(this.usersManager, times(0)).updateUserChoiceRatings(any(Map.class));
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
  }

  ////////////////////////
  // editCategory tests //
  ////////////////////////

  @Test
  public void editCategory_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new ResultStatus(true, "usersManagerWorks")).when(this.usersManager)
        .updateUserChoiceRatings(any(Map.class));

    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput);

    assertTrue(resultStatus.success);
    verify(this.usersManager, times(1)).updateUserChoiceRatings(any(Map.class));
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class)); // the db is hit twice, but only once by the dependency being tested
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
  }

  @Test
  public void editCategory_validInputBadUsers_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new ResultStatus(false, "usersManagerBroken")).when(this.usersManager)
        .updateUserChoiceRatings(any(Map.class));

    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput);

    assertFalse(resultStatus.success);
    verify(this.usersManager, times(1)).updateUserChoiceRatings(any(Map.class));
    //TODO we need to update the function to try to revert what it has already done maybe? -> 2 calls then
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class)); // the db is hit twice, but only once by the dependency being tested\
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
  }

  @Test
  public void editCategory_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput);

    assertFalse(resultStatus.success);
    verify(this.usersManager, times(0)).updateUserChoiceRatings(any(Map.class));
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
  }

  @Test
  public void editCategory_missingKey_failureResult() {
    ResultStatus resultStatus = this.categoriesManager.editCategory(this.badInput);
    assertFalse(resultStatus.success);

    this.badInput.put(CategoriesManager.CATEGORY_ID, "testId");
    resultStatus = this.categoriesManager.editCategory(this.badInput);
    assertFalse(resultStatus.success);
    this.badInput.put(CategoriesManager.CATEGORY_NAME, "testName");
    resultStatus = this.categoriesManager.editCategory(this.badInput);
    assertFalse(resultStatus.success);
    this.badInput.put(CategoriesManager.CHOICES, "testChoices");
    resultStatus = this.categoriesManager.editCategory(this.badInput);
    assertFalse(resultStatus.success);
    this.badInput.put(RequestFields.USER_RATINGS, "testRatings");
    resultStatus = this.categoriesManager.editCategory(this.badInput);
    assertFalse(resultStatus.success);

    verify(this.usersManager, times(0)).updateUserChoiceRatings(any(Map.class));
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
  }

  ////////////////////////
  // getCategories test //
  ////////////////////////

  @Test
  public void getCategories_validInputActiveUser_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(ImmutableList.of("catId1", "catId2")).when(this.usersManager)
        .getAllCategoryIds(any(String.class));

    ResultStatus resultStatus = this.categoriesManager
        .getCategories(this.getCategoriesGoodInputActiveUser);

    assertTrue(resultStatus.success);
    verify(this.usersManager, times(1)).getAllCategoryIds(any(String.class));
    verify(this.dynamoDB, times(2)).getTable(
        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
  }

  @Test
  public void getCategories_validInputCategoryIds_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(null, new Item()).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.categoriesManager
        .getCategories(this.getCategoriesGoodInputCategoryIds);

    assertTrue(resultStatus.success);
    assertEquals(resultStatus.resultMessage, "[{}]");
    verify(this.usersManager, times(0)).getAllCategoryIds(any(String.class));
    verify(this.groupsManager, times(0)).getAllCategoryIds(any(String.class));
    verify(this.dynamoDB, times(2)).getTable(
        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
  }

  @Test
  public void getCategories_validInputGroupId_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(GroupsManager.GROUP_ID).when(this.groupsManager).getPrimaryKeyIndex();
    doReturn(ImmutableList.of("catId1", "catId2")).when(this.groupsManager)
        .getAllCategoryIds(any(String.class));

    ResultStatus resultStatus = this.categoriesManager
        .getCategories(this.getCategoriesGoodInputGroupId);

    assertTrue(resultStatus.success);
    verify(this.groupsManager, times(1)).getAllCategoryIds(any(String.class));
    verify(this.dynamoDB, times(2)).getTable(
        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
  }

  @Test
  public void getCategories_noDbConnection_successfulResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.categoriesManager
        .getCategories(this.getCategoriesGoodInputCategoryIds);

    assertTrue(resultStatus.success);
    assertEquals(resultStatus.resultMessage,
        "[]"); // we failed gracefully and returned an empty list
    verify(this.usersManager, times(0)).updateUserChoiceRatings(any(Map.class));
    verify(this.dynamoDB, times(2)).getTable(any(String.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
  }

  @Test
  public void getCategories_missingKey_failureResult() {
    ResultStatus resultStatus = this.categoriesManager.getCategories(this.badInput);
    assertFalse(resultStatus.success);

    verify(this.usersManager, times(0)).getAllCategoryIds(any(String.class));
    verify(this.groupsManager, times(0)).getAllCategoryIds(any(String.class));
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
  }

  ///////////////////////////
  // deleteCategories test //
  ///////////////////////////

  @Test
  public void deleteCategory_validInputWithGroups_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(CategoriesManager.GROUPS, ImmutableMap.of("groupId1", "groupName1"))
        .withString(CategoriesManager.OWNER, "ActiveUser")).when(this.table)
        .getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.categoriesManager.deleteCategory(this.deleteCategoryGoodInput);

    assertTrue(resultStatus.success);
    verify(this.groupsManager, times(1))
        .removeCategoryFromGroups(any(List.class), any(String.class));
    verify(this.dynamoDB, times(2)).getTable(
        any(String.class)); // the db is hit twice, but only once by the dependency being tested
    verify(this.table, times(1)).deleteItem(any(DeleteItemSpec.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
  }

  @Test
  public void deleteCategory_validInputWithOutGroups_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(CategoriesManager.GROUPS, ImmutableMap.of()).withString(CategoriesManager.OWNER, "ActiveUser"))
        .when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.categoriesManager.deleteCategory(this.deleteCategoryGoodInput);

    assertTrue(resultStatus.success);
    verify(this.groupsManager, times(0))
        .removeCategoryFromGroups(any(List.class), any(String.class));
    verify(this.dynamoDB, times(2)).getTable(
        any(String.class)); // the db is hit twice, but only once by the dependency being tested
    verify(this.table, times(1)).deleteItem(any(DeleteItemSpec.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
  }

  @Test
  public void deleteCategory_noDbConnection_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withString(CategoriesManager.OWNER, "BadUser"))
        .when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.categoriesManager.deleteCategory(this.editCategoryGoodInput);

    assertFalse(resultStatus.success);
    verify(this.groupsManager, times(0))
        .removeCategoryFromGroups(any(List.class), any(String.class));
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).deleteItem(any(DeleteItemSpec.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
  }

  @Test
  public void deleteCategory_activeUserIsNotOwner_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.categoriesManager.deleteCategory(this.editCategoryGoodInput);

    assertFalse(resultStatus.success);
    verify(this.groupsManager, times(0))
        .removeCategoryFromGroups(any(List.class), any(String.class));
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).deleteItem(any(DeleteItemSpec.class));
  }

  @Test
  public void deleteCategory_missingKey_failureResult() {
    ResultStatus resultStatus = this.categoriesManager.deleteCategory(this.badInput);
    assertFalse(resultStatus.success);

    this.badInput.put(CategoriesManager.CATEGORY_ID, "testId");
    resultStatus = this.categoriesManager.deleteCategory(this.badInput);
    assertFalse(resultStatus.success);

    verify(this.usersManager, times(0)).updateUserChoiceRatings(any(Map.class));
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).deleteItem(any(DeleteItemSpec.class));
  }
}