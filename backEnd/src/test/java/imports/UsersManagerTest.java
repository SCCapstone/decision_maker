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
import com.amazonaws.services.lambda.runtime.LambdaLogger;
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
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class UsersManagerTest {

  private UsersManager usersManager;

  private final Map<String, Object> badInput = new HashMap<>();

  private final String goodCategoryId = "CategoryId1";

  private final Map<String, Object> getUserRatingsGoodInput = ImmutableMap.of(
      CategoriesManager.CATEGORY_ID, goodCategoryId,
      RequestFields.ACTIVE_USER, "ActiveUser"
  );

  @Mock
  private Table table;

  @Mock
  private DynamoDB dynamoDB;

  @Mock
  private CategoriesManager categoriesManager;

  @Mock
  private GroupsManager groupsManager;

  @Mock
  private LambdaLogger lambdaLogger;

  @Mock
  private Metrics metrics;

  @BeforeEach
  private void init() {
    this.usersManager = new UsersManager(this.dynamoDB);

    DatabaseManagers.CATEGORIES_MANAGER = this.categoriesManager;
    DatabaseManagers.USERS_MANAGER = this.usersManager;
    DatabaseManagers.GROUPS_MANAGER = this.groupsManager;
  }

  /////////////////////////////
  // getAllCategoryIds tests //
  /////////////////////////////

  //////////////////////////
  // getAllGroupIds tests //
  //////////////////////////

  @Test
  public void getCategories_validInputActiveUser_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(UsersManager.GROUPS, ImmutableMap
        .of("GroupId1", "GroupName1", "GroupId2", "GroupName2")))
        .when(this.table).getItem(any(GetItemSpec.class));

    List<String> groupIds = this.usersManager
        .getAllGroupIds("TestUserName", this.metrics, this.lambdaLogger);

    assertEquals(groupIds.size(), 2);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getCategories_badActiveUser_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(null).when(this.table).getItem(any(GetItemSpec.class));

    List<String> groupIds = this.usersManager
        .getAllGroupIds("BadTestUserName", this.metrics, this.lambdaLogger);

    assertEquals(groupIds.size(), 0);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void getCategories_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    List<String> groupIds = this.usersManager
        .getAllGroupIds("TestUserName", this.metrics, this.lambdaLogger);

    assertEquals(groupIds.size(), 0);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  //////////////////////
  // addNewUser tests //
  //////////////////////

  ///////////////////////////////////
  // updateUserChoiceRatings tests //
  ///////////////////////////////////

  /////////////////////////////////
  // updateUserAppSettings tests //
  /////////////////////////////////

  ////////////////////////// region
  // getUserRatings tests //
  //////////////////////////
  @Test
  public void getUserRatings_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(UsersManager.CATEGORIES, ImmutableMap
        .of(this.goodCategoryId, ImmutableMap.of("Choice", "1"), "ChoiceId2", "ChoiceRating2")))
        .when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .getUserRatings(this.getUserRatingsGoodInput, this.metrics, this.lambdaLogger);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getUserRatings_badUsername_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(null).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .getUserRatings(this.getUserRatingsGoodInput, this.metrics, this.lambdaLogger);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void getUserRatings_badCategoryId_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(UsersManager.CATEGORIES, new HashMap())).when(this.table)
        .getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .getUserRatings(this.getUserRatingsGoodInput, this.metrics, this.lambdaLogger);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void getUserRatings_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.usersManager
        .getUserRatings(this.getUserRatingsGoodInput, this.metrics, this.lambdaLogger);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void getUserRatings_missingKey_failureResult() {
    ResultStatus resultStatus = this.usersManager
        .getUserRatings(this.badInput, metrics, lambdaLogger);
    assertFalse(resultStatus.success);

    this.badInput.put(RequestFields.ACTIVE_USER, "testId");
    resultStatus = this.usersManager.getUserRatings(this.badInput, metrics, lambdaLogger);
    assertFalse(resultStatus.success);

    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).deleteItem(any(DeleteItemSpec.class));
    verify(this.metrics, times(2)).commonClose(false);
  }
  //endregion

  //////////////////////////////
  // getUserAppSettings tests //
  //////////////////////////////

  /////////////////////////////////
  // getDefaultAppSettings tests //
  /////////////////////////////////

  ////////////////////////////////
  // checkAppSettingsVals tests //
  ////////////////////////////////
}
