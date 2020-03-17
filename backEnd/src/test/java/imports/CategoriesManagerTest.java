package imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
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
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class CategoriesManagerTest {

  private final Map<String, Object> badInput = new HashMap<>();

  private final Map<String, Object> newCategoryGoodInput = new HashMap<String, Object>() {{
    put(CategoriesManager.CATEGORY_NAME, "CategoryName");
    put(CategoriesManager.CHOICES, ImmutableMap.of("0", "label", "2", "label"));
    put(RequestFields.USER_RATINGS, ImmutableMap.of("0", "rating", "2", "rating"));
    put(RequestFields.ACTIVE_USER, "ActiveUser");
  }};

  private final Item newCategoryGoodUser = new Item().withMap(UsersManager.OWNED_CATEGORIES,
      (ImmutableMap.of("catId", "catName", "catId2", "catName2")));

  private final Item newCategoryInvalidUser = new Item().withMap(UsersManager.OWNED_CATEGORIES,
      new HashMap<String, String>() {{
        put("duplicateName", "CategoryName"); // duplicate the name above
        for (int i = 0; i < CategoriesManager.MAX_NUMBER_OF_CATEGORIES; i++) {
          put("catId" + (new Integer(i)).toString(), "catName" + (new Integer(i)).toString());
        }
      }});

  private final Map<String, Object> editCategoryGoodInput = Maps.newHashMap(ImmutableMap.of(
      CategoriesManager.CATEGORY_ID, "CategoryId",
      CategoriesManager.CATEGORY_NAME, "CategoryName",
      CategoriesManager.CHOICES, ImmutableMap.of("-1", "label", "0", "label"),
      RequestFields.USER_RATINGS, ImmutableMap.of("-1", "rating", "0", "rating"),
      RequestFields.ACTIVE_USER, "ActiveUser"
  ));

  private final Map<String, Object> deleteCategoryGoodInput = Maps.newHashMap(ImmutableMap.of(
      CategoriesManager.CATEGORY_ID, "CategoryId",
      RequestFields.ACTIVE_USER, "ActiveUser"
  ));

  private final Map<String, Object> getCategoriesGoodInputActiveUser = Maps
      .newHashMap(ImmutableMap.of(
          RequestFields.ACTIVE_USER, "ActiveUser"
      ));

  private final Map<String, Object> getCategoriesGoodInputCategoryIds = Maps
      .newHashMap(ImmutableMap.of(
          RequestFields.CATEGORY_IDS, ImmutableList.of("catId1", "catId2")
      ));

  private final Map<String, Object> getCategoriesGoodInputGroupId = Maps.newHashMap(ImmutableMap.of(
      GroupsManager.GROUP_ID, "groupId"
  ));

  private CategoriesManager categoriesManager;

  @Mock
  private Table table;

  @Mock
  private DynamoDB dynamoDB;

  @Mock
  private UsersManager usersManager;

  @Mock
  private GroupsManager groupsManager;

  @Mock
  private Metrics metrics;

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
        .updateUserChoiceRatings(any(Map.class), eq(true), any(Metrics.class));
    doReturn(newCategoryGoodUser).when(this.usersManager).getItemByPrimaryKey(any(String.class));

    ResultStatus resultStatus = this.categoriesManager.addNewCategory(this.newCategoryGoodInput,
        this.metrics);

    assertTrue(resultStatus.success);
    verify(this.usersManager, times(1))
        .updateUserChoiceRatings(any(Map.class), eq(true), any(Metrics.class));
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class)); // the db is hit twice, but only once by the dependency being tested
    verify(this.table, times(1)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(2)).commonClose(true);
  }

  @Test
  public void addNewCategory_validInputBadUserRatingsUpdate_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new ResultStatus(false, "usersManagerBroken")).when(this.usersManager)
        .updateUserChoiceRatings(any(Map.class), eq(true), any(Metrics.class));
    doReturn(newCategoryGoodUser).when(this.usersManager).getItemByPrimaryKey(any(String.class));

    ResultStatus resultStatus = this.categoriesManager.addNewCategory(this.newCategoryGoodInput,
        this.metrics);

    assertFalse(resultStatus.success);
    verify(this.usersManager, times(1))
        .updateUserChoiceRatings(any(Map.class), eq(true), any(Metrics.class));
    //TODO we need to update the function to try to revert what it has already done maybe? -> 2 calls then
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class)); // the db is hit twice, but only once by the dependency being tested\
    verify(this.table, times(1)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false); // whole operation
    verify(this.metrics, times(1)).commonClose(true); // valid input
  }

  @Test
  public void addNewCategory_validInputBadUserGet_failureResult() {
    doThrow(NullPointerException.class).when(this.usersManager)
        .getItemByPrimaryKey(any(String.class));

    ResultStatus resultStatus = this.categoriesManager.addNewCategory(this.newCategoryGoodInput,
        this.metrics);

    assertFalse(resultStatus.success);
    verify(this.usersManager, times(0))
        .updateUserChoiceRatings(any(Map.class), eq(true), any(Metrics.class));
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(2)).commonClose(false); // whole operation
    verify(this.metrics, times(0)).commonClose(true); // valid input
  }

  @Test
  public void addNewCategory_invalidInput_failureResult() {
    this.newCategoryGoodInput.put(CategoriesManager.CHOICES, Collections.emptyMap());

    doReturn(newCategoryInvalidUser).when(this.usersManager).getItemByPrimaryKey(any(String.class));

    ResultStatus resultStatus = this.categoriesManager.addNewCategory(this.newCategoryGoodInput,
        this.metrics);

    assertFalse(resultStatus.success);
    verify(this.usersManager, times(0))
        .updateUserChoiceRatings(any(Map.class), eq(true), any(Metrics.class));
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(2)).commonClose(false); // whole operation
    verify(this.metrics, times(0)).commonClose(true); // valid input
  }

  @Test
  public void addNewCategory_invalidInput2_failureResult() {
    //testing no empty choice labels and no empty category names
    this.newCategoryGoodInput.put(CategoriesManager.CHOICES, ImmutableMap.of("0", ""));
    this.newCategoryGoodInput.put(CategoriesManager.CATEGORY_NAME, "");

    doReturn(newCategoryInvalidUser).when(this.usersManager).getItemByPrimaryKey(any(String.class));

    ResultStatus resultStatus = this.categoriesManager.addNewCategory(this.newCategoryGoodInput,
        this.metrics);

    assertFalse(resultStatus.success);
    verify(this.usersManager, times(0))
        .updateUserChoiceRatings(any(Map.class), eq(true), any(Metrics.class));
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(2)).commonClose(false); // whole operation
    verify(this.metrics, times(0)).commonClose(true); // valid input
  }

  @Test
  public void addNewCategory_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));
    doReturn(newCategoryGoodUser).when(this.usersManager).getItemByPrimaryKey(any(String.class));

    ResultStatus resultStatus = this.categoriesManager.addNewCategory(this.newCategoryGoodInput,
        this.metrics);

    assertFalse(resultStatus.success);
    verify(this.usersManager, times(0))
        .updateUserChoiceRatings(any(Map.class), any(Boolean.class), any(Metrics.class));
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false); // whole operation
    verify(this.metrics, times(1)).commonClose(true); // valid input
  }

  @Test
  public void addNewCategory_missingKey_failureResult() {
    ResultStatus resultStatus = this.categoriesManager.addNewCategory(this.badInput,
        this.metrics);
    assertFalse(resultStatus.success);

    this.badInput.put(CategoriesManager.CATEGORY_NAME, "testName");
    resultStatus = this.categoriesManager
        .addNewCategory(this.badInput, this.metrics);
    assertFalse(resultStatus.success);
    this.badInput.put(CategoriesManager.CHOICES, "testChoices");
    resultStatus = this.categoriesManager
        .addNewCategory(this.badInput, this.metrics);
    assertFalse(resultStatus.success);
    this.badInput.put(RequestFields.USER_RATINGS, "userRatings");
    resultStatus = this.categoriesManager
        .addNewCategory(this.badInput, this.metrics);
    assertFalse(resultStatus.success);

    verify(this.usersManager, times(0))
        .updateUserChoiceRatings(any(Map.class), any(Boolean.class), any(Metrics.class));
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(4)).commonClose(false);
  }

  ////////////////////////
  // editCategory tests //
  ////////////////////////

  @Test
  public void editCategory_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withString(CategoriesManager.OWNER, "ActiveUser"))
        .when(this.table)
        .getItem(any(GetItemSpec.class));
    doReturn(new ResultStatus(true, "usersManagerWorks")).when(this.usersManager)
        .updateUserChoiceRatings(any(Map.class), eq(true), any(Metrics.class));

    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput,
        this.metrics);

    assertTrue(resultStatus.success);
    verify(this.usersManager, times(1)).updateUserChoiceRatings(any(Map.class),
        eq(true), any(Metrics.class));
    verify(this.dynamoDB, times(2)).getTable(
        any(String.class)); // the db is hit twice, but only once by the dependency being tested
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void editCategory_validInputBadUsers_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withString(CategoriesManager.OWNER, "ActiveUser"))
        .when(this.table)
        .getItem(any(GetItemSpec.class));
    doReturn(new ResultStatus(false, "usersManagerBroken")).when(this.usersManager)
        .updateUserChoiceRatings(any(Map.class), eq(true), any(Metrics.class));

    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput,
        this.metrics);

    assertFalse(resultStatus.success);
    verify(this.usersManager, times(1)).updateUserChoiceRatings(any(Map.class),
        eq(true), any(Metrics.class));
    //TODO we need to update the function to try to revert what it has already done maybe? -> 2 calls then
    verify(this.dynamoDB, times(2)).getTable(
        any(String.class)); // the db is hit twice, but only once by the dependency being tested\
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void editCategory_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput,
        this.metrics);

    assertFalse(resultStatus.success);
    verify(this.usersManager, times(0)).updateUserChoiceRatings(any(Map.class),
        any(Boolean.class), any(Metrics.class));
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void editCategory_missingKey_failureResult() {
    ResultStatus resultStatus = this.categoriesManager.editCategory(this.badInput, this.metrics);
    assertFalse(resultStatus.success);

    this.badInput.put(CategoriesManager.CATEGORY_ID, "testId");
    resultStatus = this.categoriesManager.editCategory(this.badInput, this.metrics);
    assertFalse(resultStatus.success);
    this.badInput.put(CategoriesManager.CATEGORY_NAME, "testName");
    resultStatus = this.categoriesManager.editCategory(this.badInput, this.metrics);
    assertFalse(resultStatus.success);
    this.badInput.put(CategoriesManager.CHOICES, "testChoices");
    resultStatus = this.categoriesManager.editCategory(this.badInput, this.metrics);
    assertFalse(resultStatus.success);
    this.badInput.put(RequestFields.USER_RATINGS, "testRatings");
    resultStatus = this.categoriesManager.editCategory(this.badInput, this.metrics);
    assertFalse(resultStatus.success);

    verify(this.usersManager, times(0)).updateUserChoiceRatings(any(Map.class),
        any(Boolean.class), any(Metrics.class));
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(5)).commonClose(false);
  }

  ////////////////////////
  // getCategories test //
  ////////////////////////

  @Test
  public void getCategories_validInputActiveUser_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new ArrayList<>()).when(this.usersManager)
        .getAllOwnedCategoryIds(any(String.class), any(Metrics.class));
    doReturn(ImmutableList.of("groupId1", "groupId2")).when(this.usersManager)
        .getAllGroupIds(any(String.class), any(Metrics.class));
    doReturn(ImmutableList.of("cat1"), ImmutableList.of("cat2")).when(this.groupsManager)
        .getAllCategoryIds(any(String.class), eq(this.metrics));

    ResultStatus resultStatus = this.categoriesManager
        .getCategories(this.getCategoriesGoodInputActiveUser, this.metrics);

    assertTrue(resultStatus.success);
    verify(this.usersManager, times(1))
        .getAllOwnedCategoryIds(any(String.class), any(Metrics.class));
    verify(this.dynamoDB, times(2)).getTable(
        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getCategories_validInputActiveUserNoGroups_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(ImmutableList.of("catId1", "catId2")).when(this.usersManager)
        .getAllOwnedCategoryIds(any(String.class), any(Metrics.class));

    ResultStatus resultStatus = this.categoriesManager
        .getCategories(this.getCategoriesGoodInputActiveUser, this.metrics);

    assertTrue(resultStatus.success);
    verify(this.usersManager, times(1))
        .getAllOwnedCategoryIds(any(String.class), any(Metrics.class));
    verify(this.dynamoDB, times(2)).getTable(
        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getCategories_validInputCategoryIds_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(null, new Item()).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.categoriesManager
        .getCategories(this.getCategoriesGoodInputCategoryIds, this.metrics);

    assertTrue(resultStatus.success);
    assertEquals(resultStatus.resultMessage, "[{}]");
    verify(this.usersManager, times(0))
        .getAllOwnedCategoryIds(any(String.class), any(Metrics.class));
    verify(this.groupsManager, times(0))
        .getAllCategoryIds(any(String.class), any(Metrics.class));
    verify(this.dynamoDB, times(2)).getTable(
        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getCategories_validInputGroupId_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(GroupsManager.GROUP_ID).when(this.groupsManager).getPrimaryKeyIndex();
    doReturn(ImmutableList.of("catId1", "catId2")).when(this.groupsManager)
        .getAllCategoryIds(any(String.class), any(Metrics.class));

    ResultStatus resultStatus = this.categoriesManager
        .getCategories(this.getCategoriesGoodInputGroupId, this.metrics);

    assertTrue(resultStatus.success);
    verify(this.groupsManager, times(1))
        .getAllCategoryIds(any(String.class), any(Metrics.class));
    verify(this.dynamoDB, times(2)).getTable(
        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getCategories_noDbConnection_successfulResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.categoriesManager
        .getCategories(this.getCategoriesGoodInputCategoryIds, this.metrics);

    assertTrue(resultStatus.success);
    assertEquals(resultStatus.resultMessage,
        "[]"); // we failed gracefully and returned an empty list
    verify(this.dynamoDB, times(2)).getTable(any(String.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getCategories_missingKey_failureResult() {
    ResultStatus resultStatus = this.categoriesManager
        .getCategories(this.badInput, this.metrics);
    assertFalse(resultStatus.success);

    verify(this.usersManager, times(0))
        .getAllOwnedCategoryIds(any(String.class), any(Metrics.class));
    verify(this.groupsManager, times(0))
        .getAllCategoryIds(any(String.class), any(Metrics.class));
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
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

    ResultStatus resultStatus = this.categoriesManager
        .deleteCategory(this.deleteCategoryGoodInput, metrics);

    assertTrue(resultStatus.success);
    verify(this.groupsManager, times(1))
        .removeCategoryFromGroups(any(List.class), any(String.class), any(Metrics.class));
    verify(this.dynamoDB, times(2)).getTable(any(String.class));
    verify(this.table, times(1)).deleteItem(any(DeleteItemSpec.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
  }

  @Test
  public void deleteCategory_validInputWithOutGroups_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(CategoriesManager.GROUPS, ImmutableMap.of())
        .withString(CategoriesManager.OWNER, "ActiveUser"))
        .when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.categoriesManager
        .deleteCategory(this.deleteCategoryGoodInput, metrics);

    assertTrue(resultStatus.success);
    verify(this.groupsManager, times(0))
        .removeCategoryFromGroups(any(List.class), any(String.class), any(Metrics.class));
    verify(this.dynamoDB, times(2)).getTable(any(String.class));
    verify(this.table, times(1)).deleteItem(any(DeleteItemSpec.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
  }

  @Test
  public void deleteCategory_noDbConnection_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withString(CategoriesManager.OWNER, "BadUser"))
        .when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.categoriesManager
        .deleteCategory(this.editCategoryGoodInput, metrics);

    assertFalse(resultStatus.success);
    verify(this.groupsManager, times(0))
        .removeCategoryFromGroups(any(List.class), any(String.class), any(Metrics.class));
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).deleteItem(any(DeleteItemSpec.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
  }

  @Test
  public void deleteCategory_activeUserIsNotOwner_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.categoriesManager
        .deleteCategory(this.editCategoryGoodInput, metrics);

    assertFalse(resultStatus.success);
    verify(this.groupsManager, times(0))
        .removeCategoryFromGroups(any(List.class), any(String.class), any(Metrics.class));
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).deleteItem(any(DeleteItemSpec.class));
  }

  @Test
  public void deleteCategory_missingKey_failureResult() {
    ResultStatus resultStatus = this.categoriesManager
        .deleteCategory(this.badInput, metrics);
    assertFalse(resultStatus.success);

    this.badInput.put(CategoriesManager.CATEGORY_ID, "testId");
    resultStatus = this.categoriesManager.deleteCategory(this.badInput, metrics);
    assertFalse(resultStatus.success);

    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).deleteItem(any(DeleteItemSpec.class));
  }

  ///////////////////////////endregion
  // removeGroupFromCategories test //
  ///////////////////////////region
  @Test
  public void removeGroupFromCategories_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.categoriesManager
        .removeGroupFromCategories(Sets.newHashSet("CategoryId1", "CategoryId2"), "GroupId1",
            this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(2)).getTable(any(String.class));
    verify(this.table, times(2)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void removeGroupFromCategories_validInput_noCategories_successfulResult() {
    ResultStatus resultStatus = this.categoriesManager
        .removeGroupFromCategories(Sets.newHashSet(), "GroupId1", this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void removeGroupFromCategories_validInputDbDiesDuringUpdate_failureResult() {
    doReturn(this.table, null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.categoriesManager
        .removeGroupFromCategories(Sets.newHashSet("User1", "User2"), "GroupId1", this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(2)).getTable(any(String.class));
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  //endregion
}
