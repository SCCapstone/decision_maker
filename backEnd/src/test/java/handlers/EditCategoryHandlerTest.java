package handlers;

import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import managers.DbAccessManager;
import models.Category;
import models.User;
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
public class EditCategoryHandlerTest {

  private EditCategoryHandler editCategoryHandler;

  @Mock
  private DbAccessManager dbAccessManager;

  @Mock
  private Metrics metrics;

  private Map<String, Object> editCategoryHandlerInput = new HashMap<String, Object>() {{
    put(CategoriesManager.CATEGORY_ID, "CategoryId");
    put(CategoriesManager.CATEGORY_NAME, "CategoryName");
    put(CategoriesManager.CHOICES, ImmutableMap.of("-1", "label", "0", "label"));
    put(RequestFields.USER_RATINGS, ImmutableMap.of("-1", 3, "0", 3));
    put(RequestFields.ACTIVE_USER, "johnplaysgolf");
  }};

  @BeforeEach
  private void init() {
    this.editCategoryHandler = new EditCategoryHandler(this.dbAccessManager,
        this.editCategoryHandlerInput, this.metrics);
  }

  @Test
  public void editCategory_validInputCategoryNameChange_successfulResult() {
    try {
      doReturn(new Category(JsonUtils.getItemFromFile("categoryLunchOptions.json")))
          .when(this.dbAccessManager).getCategory(any(String.class));
      doReturn(new User(JsonUtils.getItemFromFile("johnplaysgolf.json"))).when(this.dbAccessManager)
          .getUser(any(String.class));

      final ResultStatus resultStatus = this.editCategoryHandler.handle();

      assertTrue(resultStatus.success);
      ArgumentCaptor<List<TransactWriteItem>> argument = ArgumentCaptor.forClass(List.class);
      verify(this.dbAccessManager).executeWriteTransaction(argument.capture());
      assertEquals(2, argument.getValue().size());

      verify(this.metrics, times(3)).commonClose(true);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

//  @Test
//  public void editCategory_validInputChoiceLabelChange_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    this.editCategoryOldCategory
//        .withMap(CategoriesManager.CHOICES, ImmutableMap.of("-1", "label", "0", "newLabel"));
//    doReturn(this.editCategoryOldCategory).when(this.table).getItem(any(GetItemSpec.class));
//    doReturn(this.editCategoryGoodUser.asMap()).when(this.usersManager)
//        .getMapByPrimaryKey(any(String.class));
//    doReturn(new ResultStatus(true, "usersManagerWorks")).when(this.usersManager)
//        .updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//
//    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput,
//        this.metrics);
//
//    assertTrue(resultStatus.success);
//    verify(this.usersManager, times(1)).updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//    verify(this.dynamoDB, times(2)).getTable(any(String.class));
//    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
//    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//    verify(this.metrics, times(2)).commonClose(true);
//  }
//
//  @Test
//  public void editCategory_validInputRemoveAndAddLabel_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    this.editCategoryOldCategory
//        .withMap(CategoriesManager.CHOICES, ImmutableMap.of("-1", "label", "8", "newLabel"));
//    doReturn(this.editCategoryOldCategory).when(this.table).getItem(any(GetItemSpec.class));
//    doReturn(this.editCategoryGoodUser.asMap()).when(this.usersManager)
//        .getMapByPrimaryKey(any(String.class));
//    doReturn(new ResultStatus(true, "usersManagerWorks")).when(this.usersManager)
//        .updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//
//    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput,
//        this.metrics);
//
//    assertTrue(resultStatus.success);
//    verify(this.usersManager, times(1)).updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//    verify(this.dynamoDB, times(2)).getTable(any(String.class));
//    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
//    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//    verify(this.metrics, times(2)).commonClose(true);
//  }
//
//  @Test
//  public void editCategory_validInputDeleteChoice_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    this.editCategoryOldCategory.withMap(CategoriesManager.CHOICES, ImmutableMap.of("-1", "label"));
//    doReturn(this.editCategoryOldCategory).when(this.table).getItem(any(GetItemSpec.class));
//    doReturn(this.editCategoryGoodUser.asMap()).when(this.usersManager)
//        .getMapByPrimaryKey(any(String.class));
//    doReturn(new ResultStatus(true, "usersManagerWorks")).when(this.usersManager)
//        .updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//
//    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput,
//        this.metrics);
//
//    assertTrue(resultStatus.success);
//    verify(this.usersManager, times(1)).updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//    verify(this.dynamoDB, times(2)).getTable(any(String.class));
//    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
//    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//    verify(this.metrics, times(2)).commonClose(true);
//  }
//
//  @Test
//  public void editCategory_validInputNoCategoryChanges_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(this.editCategoryOldCategory).when(this.table).getItem(any(GetItemSpec.class));
//    doReturn(this.editCategoryGoodUser.asMap()).when(this.usersManager)
//        .getMapByPrimaryKey(any(String.class));
//    doReturn(new ResultStatus(true, "usersManagerWorks")).when(this.usersManager)
//        .updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//
//    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput,
//        this.metrics);
//
//    assertTrue(resultStatus.success);
//    verify(this.usersManager, times(1)).updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
//    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//    verify(this.metrics, times(2)).commonClose(true);
//  }
//
//  @Test
//  public void editCategory_invalidInputUserDoesNotOwnCategory_failureResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    this.editCategoryOldCategory.withString(CategoriesManager.OWNER, "differentUser");
//    doReturn(this.editCategoryOldCategory).when(this.table).getItem(any(GetItemSpec.class));
//
//    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput,
//        this.metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.usersManager, times(0)).updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
//    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//    verify(this.metrics, times(2)).commonClose(false);
//  }
//
//  @Test
//  public void editCategory_invalidInputDuplicateCategoryName_failureResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(this.editCategoryOldCategory).when(this.table).getItem(any(GetItemSpec.class));
//    doReturn(this.editCategoryGoodUser.asMap()).when(this.usersManager)
//        .getMapByPrimaryKey(any(String.class));
//    this.editCategoryGoodInput.put(CategoriesManager.CATEGORY_ID, "catId");
//
//    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput,
//        this.metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.usersManager, times(0)).updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
//    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//    verify(this.metrics, times(2)).commonClose(false);
//  }
//
//  @Test
//  public void editCategory_invalidInputEmptyCategoryNameOrEmptyChoices_failureResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(this.editCategoryOldCategory).when(this.table).getItem(any(GetItemSpec.class));
//    doReturn(this.editCategoryGoodUser.asMap()).when(this.usersManager)
//        .getMapByPrimaryKey(any(String.class));
//    this.editCategoryGoodInput.put(CategoriesManager.CATEGORY_NAME, "");
//    this.editCategoryGoodInput.put(CategoriesManager.CHOICES, Collections.emptyMap());
//
//    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput,
//        this.metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.usersManager, times(0)).updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
//    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//    verify(this.metrics, times(2)).commonClose(false);
//  }
//
//  @Test
//  public void editCategory_invalidInputEmptyChoiceLabel_failureResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(this.editCategoryOldCategory).when(this.table).getItem(any(GetItemSpec.class));
//    doReturn(this.editCategoryGoodUser.asMap()).when(this.usersManager)
//        .getMapByPrimaryKey(any(String.class));
//    this.editCategoryGoodInput.put(CategoriesManager.CHOICES, ImmutableMap.of("id", ""));
//
//    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput,
//        this.metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.usersManager, times(0)).updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
//    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
//    verify(this.metrics, times(2)).commonClose(false);
//  }
//
//  @Test
//  public void editCategory_validInputNoCategoryChangesBadUsers_failureResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(this.editCategoryOldCategory).when(this.table).getItem(any(GetItemSpec.class));
//    doReturn(this.editCategoryGoodUser.asMap()).when(this.usersManager)
//        .getMapByPrimaryKey(any(String.class));
//    doReturn(new ResultStatus(false, "usersManagerBroken")).when(this.usersManager)
//        .updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//
//    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput,
//        this.metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.usersManager, times(1)).updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(false);
//  }
//
//  @Test
//  public void editCategory_validInputNoCategoryChangesBadUsers2_failureResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//    doReturn(this.editCategoryOldCategory).when(this.table).getItem(any(GetItemSpec.class));
//    doReturn(null).when(this.usersManager).getMapByPrimaryKey(any(String.class));
//
//    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput,
//        this.metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.usersManager, times(0)).updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
//    verify(this.metrics, times(2)).commonClose(false);
//  }
//
//  @Test
//  public void editCategory_noDbConnection_failureResult() {
//    doReturn(null).when(this.dynamoDB).getTable(any(String.class));
//
//    ResultStatus resultStatus = this.categoriesManager.editCategory(this.editCategoryGoodInput,
//        this.metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.usersManager, times(0)).updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//    verify(this.dynamoDB, times(1)).getTable(any(String.class));
//    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(false);
//  }
//
//  @Test
//  public void editCategory_missingKey_failureResult() {
//    ResultStatus resultStatus = this.categoriesManager.editCategory(this.badInput, this.metrics);
//    assertFalse(resultStatus.success);
//
//    verify(this.usersManager, times(0)).updateUserChoiceRatings(any(Map.class), any(Metrics.class));
//    verify(this.dynamoDB, times(0)).getTable(any(String.class));
//    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(false);
//  }
}
