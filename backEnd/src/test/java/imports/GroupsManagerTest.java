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
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utilities.Metrics;
import utilities.ResultStatus;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class GroupsManagerTest {

  private GroupsManager groupsManager;

  private final String goodCategoryId = "CategoryId1";

  private final ArrayList<String> goodGroupIds = new ArrayList<>();

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
  private LambdaLogger lambdaLogger;

  @Mock
  private Metrics metrics;

  @BeforeEach
  private void init() {
    this.groupsManager = new GroupsManager(this.dynamoDB);
    this.goodGroupIds.add("GoodGroupId");

    DatabaseManagers.CATEGORIES_MANAGER = this.categoriesManager;
    DatabaseManagers.USERS_MANAGER = this.usersManager;
    DatabaseManagers.GROUPS_MANAGER = this.groupsManager;
    DatabaseManagers.PENDING_EVENTS_MANAGER = this.pendingEventsManager;
  }

  /////////////////////
  // getGroups tests //
  /////////////////////region

  //////////////////////////endregion
  // createNewGroup tests //
  //////////////////////////region

  /////////////////////endregion
  // editGroup tests //
  /////////////////////region

  ////////////////////endregion
  // newEvent tests //
  ////////////////////region

  //////////////////////////endregion
  // optInOutOfEvent tests //
  //////////////////////////region

  ////////////////////////////////////////endregion
  // updateMembersMapForInsertion tests //
  ////////////////////////////////////////region

  ////////////////////////////endregion
  // editInputIsValid tests //
  ////////////////////////////region

  /////////////////////////////////endregion
  // makeEventInputIsValid tests //
  /////////////////////////////////region
  @Test
  public void makeEventInputIsValid_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(CATEGORIES, ImmutableMap.of("id", "name", "id2", "name2")))
        .when(this.table).getItem(any(GetItemSpec.class));

    List<String> categoryIds = this.groupsManager
        .getAllCategoryIds("groupId", this.metrics, this.lambdaLogger);

    assertEquals(categoryIds.size(), 2);
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  public void makeEventInputIsValid_emptyInput_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(CATEGORIES, ImmutableMap.of("id", "name", "id2", "name2")))
        .when(this.table).getItem(any(GetItemSpec.class));

    List<String> categoryIds = this.groupsManager
        .getAllCategoryIds("groupId", this.metrics, this.lambdaLogger);

    assertEquals(categoryIds.size(), 2);
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

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
        .getAllCategoryIds("groupId", this.metrics, this.lambdaLogger);

    assertEquals(categoryIds.size(), 2);
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getAllCategoryIds_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    List<String> categoryIds = this.groupsManager
        .getAllCategoryIds("groupId", this.metrics, this.lambdaLogger);

    assertEquals(categoryIds.size(), 0);
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void getAllCategoryIds_badGroupFormat_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item()).when(this.table).getItem(any(GetItemSpec.class));

    List<String> categoryIds = this.groupsManager
        .getAllCategoryIds("groupId", this.metrics, this.lambdaLogger);

    assertEquals(categoryIds.size(), 0);
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class)); // the db is hit thrice, but only twice by the dependency being tested
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
        .removeCategoryFromGroups(this.goodGroupIds, this.goodCategoryId, this.metrics,
            this.lambdaLogger);

    assertTrue(result.success);
    verify(this.dynamoDB, times(1)).getTable(
        any(String.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  public void removeCategoryFromGroups_emptyGroupList_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    ResultStatus result = this.groupsManager
        .removeCategoryFromGroups(new ArrayList<String>(), this.goodCategoryId, this.metrics,
            this.lambdaLogger);

    assertTrue(result.success);
    verify(this.dynamoDB, times(0)).getTable(
        any(String.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  public void removeCategoryFromGroups_emptyCategoryId_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    ResultStatus result = this.groupsManager
        .removeCategoryFromGroups(this.goodGroupIds, null, this.metrics, this.lambdaLogger);

    assertFalse(result.success);
    verify(this.dynamoDB, times(0)).getTable(
        any(String.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void removeCategoryFromGroups_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));
    ResultStatus resultStatus = this.groupsManager
        .removeCategoryFromGroups(this.goodGroupIds, this.goodCategoryId, this.metrics,
            this.lambdaLogger);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  //endregion
}
