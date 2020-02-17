package imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
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

  private final Map<String, Object> updateUserChoiceRatingsGoodInput = ImmutableMap.of(
      RequestFields.ACTIVE_USER, "validActiveUser",
      CategoriesManager.CATEGORY_ID, "CategoryId1",
      RequestFields.USER_RATINGS, ImmutableMap.of("1", "1", "2", "5")
  );

  private final Map<String, Object> updateUserSettingsGoodInput = ImmutableMap.of(
      RequestFields.ACTIVE_USER, "ActiveUser",
      UsersManager.APP_SETTINGS, ImmutableMap.of(
          UsersManager.APP_SETTINGS_DARK_THEME, 1,
          UsersManager.APP_SETTINGS_GROUP_SORT, 0,
          UsersManager.APP_SETTINGS_MUTED, 0
      ),
      UsersManager.DISPLAY_NAME, "DisplayName",
      UsersManager.ICON, "Icon",
      UsersManager.FAVORITES, ImmutableList.of("fav1")
  );

  private final Item userItem = new Item()
      .withString(RequestFields.ACTIVE_USER, "ActiveUser")
      .withMap(UsersManager.APP_SETTINGS, ImmutableMap.of(
          UsersManager.APP_SETTINGS_DARK_THEME, 1,
          UsersManager.APP_SETTINGS_GROUP_SORT, 0,
          UsersManager.APP_SETTINGS_MUTED, 0
      ))
      .withString(UsersManager.DISPLAY_NAME, "DisplayName")
      .withString(UsersManager.ICON, "Icon")
      .withMap(UsersManager.FAVORITES, ImmutableMap.of(
          "fav1", ImmutableMap.of(
              UsersManager.DISPLAY_NAME, "favDisplayName",
              UsersManager.ICON, "favIcon"
          )
      ))
      .withMap(UsersManager.GROUPS, ImmutableMap.of(
          "gid1", ImmutableMap.of(
              GroupsManager.GROUP_NAME, "gidName",
              GroupsManager.ICON, "gidIcon"
          )
      ))
      .withMap(UsersManager.FAVORITE_OF, ImmutableMap.of(
          "favOf1", true
      ));

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
  /////////////////////////////region

  @Test
  public void getAllCategoryIds_validInputActiveUser_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(UsersManager.CATEGORIES, ImmutableMap
        .of("CatId1", "choices mappings", "CatId2", "choices mappings")))
        .when(this.table).getItem(any(GetItemSpec.class));

    List<String> categoryIds = this.usersManager
        .getAllCategoryIds("TestUserName", this.metrics, this.lambdaLogger);

    assertEquals(categoryIds.size(), 2);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getAllCategoryIds_badActiveUser_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(null).when(this.table).getItem(any(GetItemSpec.class));

    List<String> categoryIds = this.usersManager
        .getAllCategoryIds("BadTestUserName", this.metrics, this.lambdaLogger);

    assertEquals(categoryIds.size(), 0);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void getAllCategoryIds_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    List<String> categoryIds = this.usersManager
        .getAllCategoryIds("TestUserName", this.metrics, this.lambdaLogger);

    assertEquals(categoryIds.size(), 0);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  //////////////////////////endregion
  // getAllGroupIds tests //
  //////////////////////////region

  @Test
  public void getAllGroupIds_validInputActiveUser_successfulResult() {
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
  public void getAllGroupIds_badActiveUser_failureResult() {
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
  public void getAllGroupIds_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    List<String> groupIds = this.usersManager
        .getAllGroupIds("TestUserName", this.metrics, this.lambdaLogger);

    assertEquals(groupIds.size(), 0);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  ///////////////////////endregion
  // getUserData tests //
  ///////////////////////region

  @Test
  public void getUserData_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item()).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .getUserData(ImmutableMap.of(RequestFields.ACTIVE_USER, "userName"), this.metrics,
            this.lambdaLogger);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getUserData_newUser_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(null).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .getUserData(ImmutableMap.of(RequestFields.ACTIVE_USER, "userName"), this.metrics,
            this.lambdaLogger);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(2)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.table, times(1)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getUserData_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.usersManager
        .getUserData(ImmutableMap.of(RequestFields.ACTIVE_USER, "userName"), this.metrics,
            this.lambdaLogger);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void getUserData_missingRequestKeys_failureResult() {
    ResultStatus resultStatus = this.usersManager
        .getUserData(ImmutableMap.of(), this.metrics, this.lambdaLogger);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  ///////////////////////////////////endregion
  // updateUserChoiceRatings tests //
  ///////////////////////////////////region
  @Test
  public void updateUserChoiceRatings_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.usersManager
        .updateUserChoiceRatings(this.updateUserChoiceRatingsGoodInput, this.metrics,
            this.lambdaLogger);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void updateUserChoiceRatings_missingKey_failureResult() {
    ResultStatus resultStatus = this.usersManager
        .updateUserChoiceRatings(this.badInput, this.metrics, this.lambdaLogger);
    assertFalse(resultStatus.success);

    this.badInput.put(RequestFields.ACTIVE_USER, "activeUser");
    resultStatus = this.usersManager
        .updateUserChoiceRatings(this.badInput, this.metrics, this.lambdaLogger);
    assertFalse(resultStatus.success);

    this.badInput.put(CategoriesManager.CATEGORY_ID, "categoryId");
    resultStatus = this.usersManager
        .updateUserChoiceRatings(this.badInput, this.metrics, this.lambdaLogger);
    assertFalse(resultStatus.success);

    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(3)).commonClose(false);
  }

  @Test
  public void updateUserChoiceRatings_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.usersManager
        .updateUserChoiceRatings(ImmutableMap.of(RequestFields.ACTIVE_USER, "validActiveUser",
            CategoriesManager.CATEGORY_ID, "CategoryId1",
            RequestFields.USER_RATINGS, ImmutableMap.of("1", "1", "2", "5")),
            this.metrics, this.lambdaLogger);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  /////////////////////////////////endregion
  // updateUserSettings tests //
  /*
  /////////////////////////////////region
  @Test
  public void updateUserSettings_validInputNoChanges_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(userItem).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .updateUserSettings(this.updateUserSettingsGoodInput, this.metrics, this.lambdaLogger);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(3)).getTable(any(String.class));
    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(2)).commonClose(true);
  }

  @Test
  public void updateUserSettings_validInputDisplayNameAndIconChange_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn("fakePrimaryKey").when(this.groupsManager).getPrimaryKeyIndex();
    userItem.withString(UsersManager.DISPLAY_NAME, "new display name");
    userItem.withString(UsersManager.ICON, "new icon");
    doReturn(userItem).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .updateUserSettings(this.updateUserSettingsGoodInput, this.metrics, this.lambdaLogger);

    assertTrue(resultStatus.success);
    verify(this.lambdaLogger, times(0)).log(any(String.class));
    verify(this.dynamoDB, times(4)).getTable(any(String.class));
    verify(this.groupsManager, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
    verify(this.table, times(2)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(2)).commonClose(true);
  }

  @Test
  public void updateUserSettings_validInputRemoveFavorite_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    userItem.withMap(UsersManager.FAVORITES, ImmutableMap.of(
        "fav1", ImmutableMap.of(
            UsersManager.DISPLAY_NAME, "favDisplayName",
            UsersManager.ICON, "favIcon"
        ),
        "fav2", ImmutableMap.of(
            UsersManager.DISPLAY_NAME, "favDisplayName",
            UsersManager.ICON, "favIcon"
        )
    ));
    doReturn(userItem).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .updateUserSettings(this.updateUserSettingsGoodInput, this.metrics, this.lambdaLogger);

    assertTrue(resultStatus.success);
    verify(this.lambdaLogger, times(0)).log(any(String.class));
    verify(this.dynamoDB, times(5)).getTable(any(String.class));
    verify(this.groupsManager, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
    verify(this.table, times(3)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(2)).commonClose(true);
  }

  @Test
  public void updateUserSettings_validInputAddFavorite_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    userItem.withMap(UsersManager.FAVORITES, ImmutableMap.of());
    doReturn(userItem).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .updateUserSettings(this.updateUserSettingsGoodInput, this.metrics, this.lambdaLogger);

    assertTrue(resultStatus.success);
    verify(this.lambdaLogger, times(0)).log(any(String.class));
    verify(this.dynamoDB, times(6)).getTable(any(String.class));
    verify(this.groupsManager, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.table, times(3)).getItem(any(GetItemSpec.class));
    verify(this.table, times(3)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(2)).commonClose(true);
  }

  @Test
  public void updateUserSettings_validInputDbDiesDuringDisplayNameIconUpdate_failureResult() {
    doReturn(this.table, this.table, null).when(this.dynamoDB).getTable(any(String.class));
    doReturn("fakePrimaryKey").when(this.groupsManager).getPrimaryKeyIndex();

    userItem.withString(UsersManager.DISPLAY_NAME, "new display name");
    userItem.withString(UsersManager.ICON, "new icon");
    doReturn(userItem).when(this.table).getItem(any(GetItemSpec.class));

    doThrow(NullPointerException.class).when(this.groupsManager)
        .updateItem(any(UpdateItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .updateUserSettings(this.updateUserSettingsGoodInput, this.metrics, this.lambdaLogger);

    assertFalse(resultStatus.success);
    verify(this.lambdaLogger, times(3)).log(any(String.class));
    verify(this.dynamoDB, times(4)).getTable(any(String.class));
    verify(this.groupsManager, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true); // favorites still works here
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void updateUserSettings_validInputDbDiesDuringFavoritesUpdate_failureResult() {
    doReturn(this.table, this.table, null).when(this.dynamoDB).getTable(any(String.class));
    //adding fav2 and removing fav1
    userItem.withMap(UsersManager.FAVORITES, ImmutableMap.of(
        "fav2", ImmutableMap.of(
            UsersManager.DISPLAY_NAME, "favDisplayName",
            UsersManager.ICON, "favIcon"
        )
    ));
    doReturn(userItem).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .updateUserSettings(this.updateUserSettingsGoodInput, this.metrics, this.lambdaLogger);

    assertFalse(resultStatus.success);
    verify(this.lambdaLogger, times(3)).log(any(String.class));
    verify(this.dynamoDB, times(5)).getTable(any(String.class));
    verify(this.groupsManager, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(2)).commonClose(false);
  }

  @Test
  public void updateUserSettings_invalidInputMissingKeys_failureResult() {
    ResultStatus resultStatus = this.usersManager
        .updateUserSettings(this.badInput, this.metrics, this.lambdaLogger);

    assertFalse(resultStatus.success);
    verify(this.lambdaLogger, times(1)).log(any(String.class));
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.groupsManager, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  //////////////////////////endregion
  */
  // getUserRatings tests //
  //////////////////////////region
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

  //////////////////////////////endregion
  // getUserAppSettings tests //
  //////////////////////////////region

  /////////////////////////////////endregion
  // getDefaultAppSettings tests //
  /////////////////////////////////region

  ////////////////////////////////endregion
  // checkAppSettingsVals tests //
  ////////////////////////////////region

  //endregion
}
