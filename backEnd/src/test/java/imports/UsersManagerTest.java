package imports;

import static junit.framework.TestCase.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import com.amazonaws.services.sns.model.InvalidParameterException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
public class UsersManagerTest {

  private UsersManager usersManager;

  private final Map<String, Object> updateUserChoiceRatingsGoodInput = new HashMap<>(
      ImmutableMap.of(
          RequestFields.ACTIVE_USER, "john_andrews12",
          CategoriesManager.CATEGORY_ID, "ef8dfc02-a79d-4d55-bb03-654a7a31bb16",
          //removing 1's mapping and adding mapping for 3
          RequestFields.USER_RATINGS, ImmutableMap.of("2", 5, "3", 4),
          CategoriesManager.CATEGORY_NAME, "TestName"
      ));

  private final Map<String, Object> updateUserSettingsGoodInput = new HashMap<>(ImmutableMap.of(
      RequestFields.ACTIVE_USER, "ActiveUser",
      UsersManager.APP_SETTINGS, ImmutableMap.of(
          UsersManager.APP_SETTINGS_DARK_THEME, true,
          UsersManager.APP_SETTINGS_GROUP_SORT, 0,
          UsersManager.APP_SETTINGS_CATEGORY_SORT, 1,
          UsersManager.APP_SETTINGS_MUTED, false
      ),
      UsersManager.DISPLAY_NAME, "DisplayName",
      UsersManager.FAVORITES, ImmutableList.of("fav1")
  ));

  private final Map<String, Object> updateUserSettingsGoodInputWithIcon = ImmutableMap.of(
      RequestFields.ACTIVE_USER, "ActiveUser",
      UsersManager.APP_SETTINGS, ImmutableMap.of(
          UsersManager.APP_SETTINGS_DARK_THEME, true,
          UsersManager.APP_SETTINGS_GROUP_SORT, 0,
          UsersManager.APP_SETTINGS_CATEGORY_SORT, 1,
          UsersManager.APP_SETTINGS_MUTED, false
      ),
      UsersManager.DISPLAY_NAME, "DisplayName",
      UsersManager.ICON, ImmutableList.of(1, 2, 3),
      UsersManager.FAVORITES, ImmutableList.of("fav1")
  );

  private final Item userItem = new Item()
      .withString(RequestFields.ACTIVE_USER, "ActiveUser")
      .withMap(UsersManager.APP_SETTINGS, ImmutableMap.of(
          UsersManager.APP_SETTINGS_DARK_THEME, true,
          UsersManager.APP_SETTINGS_GROUP_SORT, 0,
          UsersManager.APP_SETTINGS_CATEGORY_SORT, 1,
          UsersManager.APP_SETTINGS_MUTED, false
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

  private final Map<String, Object> markEventAsSeenGoodInput = ImmutableMap.of(
      RequestFields.ACTIVE_USER, "activeUser",
      GroupsManager.GROUP_ID, "groupId",
      RequestFields.EVENT_ID, "eventId"
  );

  private final Map<String, Object> setUserGroupMuteGoodInput = ImmutableMap.of(
      RequestFields.ACTIVE_USER, "activeUser",
      GroupsManager.GROUP_ID, "groupId",
      UsersManager.APP_SETTINGS_MUTED, true
  );

  private final Map<String, Object> markAllEventsSeenGoodInput = ImmutableMap.of(
      RequestFields.ACTIVE_USER, "activeUser",
      GroupsManager.GROUP_ID, "groupId"
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
  private S3AccessManager s3AccessManager;

  @Mock
  private SnsAccessManager snsAccessManager;

  @Mock
  private Metrics metrics;

  @BeforeEach
  private void init() {
    this.usersManager = new UsersManager(this.dynamoDB);

    DatabaseManagers.CATEGORIES_MANAGER = this.categoriesManager;
    DatabaseManagers.USERS_MANAGER = this.usersManager;
    DatabaseManagers.GROUPS_MANAGER = this.groupsManager;
    DatabaseManagers.S3_ACCESS_MANAGER = this.s3AccessManager;
    DatabaseManagers.SNS_ACCESS_MANAGER = this.snsAccessManager;
  }

  //////////////////////////endregion
  // getAllGroupIds tests //
  //////////////////////////region

  @Test
  public void getAllGroupIds_validInputActiveUser_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item().withMap(UsersManager.GROUPS, ImmutableMap
        .of("GroupId1",
            ImmutableMap.of(GroupsManager.GROUP_NAME, "name", GroupsManager.ICON, "icon"),
            "GroupId2",
            ImmutableMap.of(GroupsManager.GROUP_NAME, "name", GroupsManager.ICON, "icon"))))
        .when(this.table).getItem(any(GetItemSpec.class));

    List<String> groupIds = this.usersManager
        .getAllGroupIds("TestUserName", this.metrics);

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
        .getAllGroupIds("BadTestUserName", this.metrics);

    assertEquals(groupIds.size(), 0);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void getAllGroupIds_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    List<String> groupIds = this.usersManager
        .getAllGroupIds("TestUserName", this.metrics);

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
        .getUserData(ImmutableMap.of(RequestFields.ACTIVE_USER, "userName"), this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getUserData_validOtherUsername_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(new Item()).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .getUserData(ImmutableMap.of(UsersManager.USERNAME, "userName"), this.metrics);

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
        .getUserData(ImmutableMap.of(RequestFields.ACTIVE_USER, "userName"), this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(2)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.table, times(1)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getUserData_otherUserNotFound_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(null).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .getUserData(ImmutableMap.of(UsersManager.USERNAME, "userName"), this.metrics);

    assertTrue(resultStatus.success);
    //check the string as it a magic one checked on the front end and we don't want it to change
    assertEquals(resultStatus.resultMessage, "User not found.");
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void getUserData_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.usersManager
        .getUserData(ImmutableMap.of(RequestFields.ACTIVE_USER, "userName"), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.table, times(0)).putItem(any(PutItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void getUserData_missingRequestKeys_failureResult() {
    ResultStatus resultStatus = this.usersManager
        .getUserData(ImmutableMap.of(), this.metrics);

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
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      ResultStatus resultStatus = this.usersManager
          .updateUserChoiceRatings(this.updateUserChoiceRatingsGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateUserChoiceRatings_validInputNewCategory_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      ResultStatus resultStatus = this.usersManager
          .updateUserChoiceRatings(ImmutableMap.of(
              RequestFields.ACTIVE_USER, "john_andrews12",
              CategoriesManager.CATEGORY_ID, "new-id",
              RequestFields.USER_RATINGS, ImmutableMap.of("1", 1, "2", 5, "3", 4),
              CategoriesManager.CATEGORY_NAME, "TestName"
          ), true, this.metrics);

      assertTrue(resultStatus.success);

      //here we're making sure the category name DOES get updated
      final ArgumentCaptor<UpdateItemSpec> argument = ArgumentCaptor.forClass(UpdateItemSpec.class);
      verify(this.table).updateItem(argument.capture());
      assertTrue(argument.getValue().getValueMap().containsKey(":categoryName"));

      verify(this.dynamoDB, times(2)).getTable(any(String.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateUserChoiceRatings_validInputNoNameChange_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      this.updateUserChoiceRatingsGoodInput.remove(CategoriesManager.CATEGORY_NAME);
      final ResultStatus resultStatus = this.usersManager
          .updateUserChoiceRatings(this.updateUserChoiceRatingsGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateUserChoiceRatings_validInputNewCategoryWrongMethod_successfulResultBadUpdate() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      final ResultStatus resultStatus = this.usersManager
          .updateUserChoiceRatings(ImmutableMap.of(
              RequestFields.ACTIVE_USER, "john_andrews12",
              CategoriesManager.CATEGORY_ID, "new-id",
              RequestFields.USER_RATINGS, ImmutableMap.of("1", 1, "2", 5, "3", 4),
              CategoriesManager.CATEGORY_NAME, "TestName"
          ), this.metrics);

      assertTrue(resultStatus.success);

      //here we're making sure the category name didn't get updated when using the wrong method
      final ArgumentCaptor<UpdateItemSpec> argument = ArgumentCaptor.forClass(UpdateItemSpec.class);
      verify(this.table).updateItem(argument.capture());
      assertFalse(argument.getValue().getValueMap().containsKey(":categoryName"));

      verify(this.dynamoDB, times(2)).getTable(any(String.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateUserChoiceRatings_mapRatingValues_failureResult() {
    ResultStatus resultStatus = this.usersManager
        .updateUserChoiceRatings(ImmutableMap.of(
            RequestFields.ACTIVE_USER, "john_andrews12",
            CategoriesManager.CATEGORY_ID, "new-id",
            //update 1's mapping and adding mapping for 3
            RequestFields.USER_RATINGS, ImmutableMap.of("1", 10), // greater than 5
            CategoriesManager.CATEGORY_NAME, "TestName"
        ), this.metrics);
    assertFalse(resultStatus.success);

    resultStatus = this.usersManager
        .updateUserChoiceRatings(ImmutableMap.of(
            RequestFields.ACTIVE_USER, "john_andrews12",
            CategoriesManager.CATEGORY_ID, "new-id",
            //update 1's mapping and adding mapping for 3
            RequestFields.USER_RATINGS, ImmutableMap.of("1", -5), // less than 0
            CategoriesManager.CATEGORY_NAME, "TestName"
        ), this.metrics);
    assertFalse(resultStatus.success);

    resultStatus = this.usersManager
        .updateUserChoiceRatings(ImmutableMap.of(
            RequestFields.ACTIVE_USER, "john_andrews12",
            CategoriesManager.CATEGORY_ID, "new-id",
            //update 1's mapping and adding mapping for 3
            RequestFields.USER_RATINGS, ImmutableMap.of("1", "not an int"), // NaN
            CategoriesManager.CATEGORY_NAME, "TestName"
        ), this.metrics);
    assertFalse(resultStatus.success);

    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(3)).commonClose(false);
  }

  @Test
  public void updateUserChoiceRatings_missingKey_failureResult() {
    final ResultStatus resultStatus = this.usersManager
        .updateUserChoiceRatings(Collections.emptyMap(), this.metrics);
    assertFalse(resultStatus.success);

    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void updateUserChoiceRatings_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.usersManager
        .updateUserChoiceRatings(ImmutableMap.of(RequestFields.ACTIVE_USER, "validActiveUser",
            CategoriesManager.CATEGORY_ID, "CategoryId1",
            RequestFields.USER_RATINGS, ImmutableMap.of("1", 1, "2", 5)), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  /////////////////////////////////endregion
  // updateUserSettings tests //
  /////////////////////////////////region
  @Test
  public void updateUserSettings_validInputNoChanges_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(userItem).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .updateUserSettings(this.updateUserSettingsGoodInput, this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(3)).getTable(any(String.class));
    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(2)).commonClose(true);
  }

  @Test
  public void updateUserSettings_validInputDisplayNameChangeNewIcon_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn("fakePrimaryKey").when(this.groupsManager).getPrimaryKeyIndex();
    doReturn(Optional.of("newIconFileName")).when(this.s3AccessManager)
        .uploadImage(any(List.class), any(Metrics.class));
    userItem.withString(UsersManager.DISPLAY_NAME, "new display name");
    doReturn(userItem).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .updateUserSettings(this.updateUserSettingsGoodInputWithIcon, this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(4)).getTable(any(String.class));
    verify(this.groupsManager, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.table, times(2)).getItem(any(GetItemSpec.class));
    verify(this.table, times(2)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(2)).commonClose(true);
  }

  @Test
  public void updateUserSettings_validInputS3UploadFails_failureResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
    doReturn(Optional.empty()).when(this.s3AccessManager)
        .uploadImage(any(List.class), any(Metrics.class));
    doReturn(userItem).when(this.table).getItem(any(GetItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .updateUserSettings(this.updateUserSettingsGoodInputWithIcon, this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.groupsManager, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
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
        .updateUserSettings(this.updateUserSettingsGoodInput, this.metrics);

    assertTrue(resultStatus.success);
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
        .updateUserSettings(this.updateUserSettingsGoodInput, this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(6)).getTable(any(String.class));
    verify(this.groupsManager, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.table, times(3)).getItem(any(GetItemSpec.class));
    verify(this.table, times(3)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(2)).commonClose(true);
  }

  @Test
  public void updateUserSettings_invalidInputs_successfulResult() {
    this.updateUserSettingsGoodInput.put(UsersManager.DISPLAY_NAME, "");
    ResultStatus resultStatus = this.usersManager
        .updateUserSettings(this.updateUserSettingsGoodInput, this.metrics);
    assertFalse(resultStatus.success);

    String veryLongDisplayName = IntStream.range(0, 100).boxed().map(Object::toString)
        .collect(Collectors.joining(""));

    this.updateUserSettingsGoodInput.put(UsersManager.DISPLAY_NAME, veryLongDisplayName);
    resultStatus = this.usersManager
        .updateUserSettings(this.updateUserSettingsGoodInput, this.metrics);
    assertFalse(resultStatus.success);

    this.updateUserSettingsGoodInput.put(UsersManager.DISPLAY_NAME, "display name");
    this.updateUserSettingsGoodInput.put(UsersManager.APP_SETTINGS, ImmutableMap.of(
        UsersManager.APP_SETTINGS_GROUP_SORT, 5000
    ));
    resultStatus = this.usersManager
        .updateUserSettings(this.updateUserSettingsGoodInput, this.metrics);
    assertFalse(resultStatus.success);

    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.metrics, times(3)).commonClose(false);
  }

  @Test
  public void updateUserSettings_validInputDbDiesDuringDisplayNameUpdate_failureResult() {
    doReturn(this.table, this.table, null).when(this.dynamoDB).getTable(any(String.class));
    doReturn("fakePrimaryKey").when(this.groupsManager).getPrimaryKeyIndex();

    userItem.withString(UsersManager.DISPLAY_NAME, "new display name");
    doReturn(userItem).when(this.table).getItem(any(GetItemSpec.class));

    doThrow(NullPointerException.class).when(this.groupsManager)
        .updateItem(any(UpdateItemSpec.class));

    ResultStatus resultStatus = this.usersManager
        .updateUserSettings(this.updateUserSettingsGoodInput, this.metrics);

    assertFalse(resultStatus.success);
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
        .updateUserSettings(this.updateUserSettingsGoodInput, this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(5)).getTable(any(String.class));
    verify(this.groupsManager, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.table, times(1)).getItem(any(GetItemSpec.class));
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(2)).commonClose(false);
  }

  @Test
  public void updateUserSettings_invalidInputMissingKeys_failureResult() {
    final ResultStatus resultStatus = this.usersManager
        .updateUserSettings(Collections.emptyMap(), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.groupsManager, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  ///////////////////////endregion
  // updateSortSetting //
  ///////////////////////region

  private Map<String, Object> updateSortSettingGroupGoodInput = new HashMap<>(ImmutableMap.of(
      RequestFields.ACTIVE_USER, "john_andrews12",
      UsersManager.APP_SETTINGS_GROUP_SORT, 3
  ));

  private Map<String, Object> updateSortSettingCategoryGoodInput = new HashMap<>(ImmutableMap.of(
      RequestFields.ACTIVE_USER, "john_andrews12",
      UsersManager.APP_SETTINGS_CATEGORY_SORT, 1
  ));

  @Test
  public void updateSortSetting_validInputGroupSort_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      final ResultStatus resultStatus = this.usersManager
          .updateSortSetting(this.updateSortSettingGroupGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class));
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateSortSetting_validInputCategorySort_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      final ResultStatus resultStatus = this.usersManager
          .updateSortSetting(this.updateSortSettingCategoryGoodInput, this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class));
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateSortSetting_invalidGroupSortValue_failureResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      this.updateSortSettingGroupGoodInput.put(UsersManager.APP_SETTINGS_GROUP_SORT, 8);
      final ResultStatus resultStatus = this.usersManager
          .updateSortSetting(this.updateSortSettingGroupGoodInput, this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class));
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateSortSetting_invalidInputNoSortKey_failureResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      this.updateSortSettingGroupGoodInput.remove(UsersManager.APP_SETTINGS_GROUP_SORT);
      final ResultStatus resultStatus = this.usersManager
          .updateSortSetting(this.updateSortSettingGroupGoodInput, this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class));
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateSortSetting_missingRequestKey_failureResult() {
    final ResultStatus resultStatus = this.usersManager
        .updateSortSetting(Collections.emptyMap(), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  /////////////////////////////////////////////endregion
  // createPlatformEndpointAndStoreArn tests //
  /////////////////////////////////////////////region

  private final Map<String, Object> createPlatformEndpointAndStoreArnGoodInput = ImmutableMap.of(
      RequestFields.ACTIVE_USER, "activeUser",
      RequestFields.DEVICE_TOKEN, "googleCloudDeviceToken"
  );

  @Test
  public void createPlatformEndpointAndStoreArn_validInput_successfulResult() {
    try {
      doReturn(new CreatePlatformEndpointResult().withEndpointArn("arn:1234"))
          .when(this.snsAccessManager)
          .registerPlatformEndpoint(any(CreatePlatformEndpointRequest.class), eq(this.metrics));
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

      final ResultStatus resultStatus = this.usersManager
          .createPlatformEndpointAndStoreArn(this.createPlatformEndpointAndStoreArnGoodInput,
              this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void createPlatformEndpointAndStoreArn_snsFailedToRegisterToken_failureResult() {
    doThrow(AmazonServiceException.class).when(this.snsAccessManager).registerPlatformEndpoint(any(
        CreatePlatformEndpointRequest.class), eq(this.metrics));

    final ResultStatus resultStatus = this.usersManager
        .createPlatformEndpointAndStoreArn(this.createPlatformEndpointAndStoreArnGoodInput,
            this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void createPlatformEndpointAndStoreArn_missingRequestKeys_failureResult() {
    final ResultStatus resultStatus = this.usersManager
        .createPlatformEndpointAndStoreArn(Collections.emptyMap(), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  //////////////////////////////////endregion
  // unregisterPushEndpoint tests //
  //////////////////////////////////region

  @Test
  public void unregisterPushEndpoint_validInput_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      final ResultStatus resultStatus = this.usersManager
          .unregisterPushEndpoint(ImmutableMap.of(RequestFields.ACTIVE_USER, "john_andrews12"),
              this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void unregisterPushEndpoint_validInputUserDidNotHaveEndpoint_successfulResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("edmond2.json")).when(this.table)
          .getItem(any(GetItemSpec.class));

      final ResultStatus resultStatus = this.usersManager
          .unregisterPushEndpoint(ImmutableMap.of(RequestFields.ACTIVE_USER, "edmond2"),
              this.metrics);

      assertTrue(resultStatus.success);
      verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
      verify(this.metrics, times(0)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void unregisterPushEndpoint_snsFailsToDeleteArn_failureResult() {
    try {
      doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
      doReturn(JsonUtils.getItemFromFile("john_andrews12.json")).when(this.table)
          .getItem(any(GetItemSpec.class));
      doThrow(InvalidParameterException.class).when(this.snsAccessManager)
          .unregisterPlatformEndpoint(any(DeleteEndpointRequest.class));

      final ResultStatus resultStatus = this.usersManager
          .unregisterPushEndpoint(ImmutableMap.of(RequestFields.ACTIVE_USER, "john_andrews12"),
              this.metrics);

      assertFalse(resultStatus.success);
      verify(this.dynamoDB, times(2)).getTable(any(String.class)); // total # of db interactions
      verify(this.table, times(1)).getItem(any(GetItemSpec.class));
      verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
      verify(this.metrics, times(0)).commonClose(true);
      verify(this.metrics, times(1)).commonClose(false);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void unregisterPushEndpoint_missingRequestKeys_failureResult() {
    final ResultStatus resultStatus = this.usersManager
        .unregisterPushEndpoint(Collections.emptyMap(), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class)); // total # of db interactions
    verify(this.metrics, times(0)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  ///////////////////////////////endregion
  // removeOwnedCategory tests //
  ///////////////////////////////region

//  private final String removeOwnedCategoryCategoryId = "categoryId";
//
//  @Test
//  public void removeOwnedCategory_validInput_successfulResult() {
//    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));
//
//    final ResultStatus resultStatus = this.usersManager
//        .removeOwnedCategory("username", this.removeOwnedCategoryCategoryId, this.metrics);
//
//    assertTrue(resultStatus.success);
//
//    //make sure the entered category id is what is getting removed
//    final ArgumentCaptor<UpdateItemSpec> argument = ArgumentCaptor.forClass(UpdateItemSpec.class);
//    verify(this.table).updateItem(argument.capture());
//    assertEquals(argument.getValue().getNameMap().get("#categoryId"),
//        this.removeOwnedCategoryCategoryId);
//
//    verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
//    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
//    verify(this.metrics, times(1)).commonClose(true);
//    verify(this.metrics, times(0)).commonClose(false);
//  }
//
//  @Test
//  public void removeOwnedCategory_noDbConnection_failureResult() {
//    doReturn(null).when(this.dynamoDB).getTable(any(String.class));
//
//    final ResultStatus resultStatus = this.usersManager
//        .removeOwnedCategory("username", "categoryId", this.metrics);
//
//    assertFalse(resultStatus.success);
//    verify(this.dynamoDB, times(1)).getTable(any(String.class)); // total # of db interactions
//    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
//    verify(this.metrics, times(0)).commonClose(true);
//    verify(this.metrics, times(1)).commonClose(false);
//  }

  ////////////////////////////////endregion
  // removeGroupFromUsers tests //
  ////////////////////////////////region
  @Test
  public void removeGroupFromUsers_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.usersManager
        .removeGroupsLeftFromUsers(Sets.newHashSet("User3", "User4"), "GroupId1", this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(2)).getTable(any(String.class));
    verify(this.table, times(2)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void removeGroupFromUsers_validInputNoMembersLeft_successfulResult() {
    ResultStatus resultStatus = this.usersManager
        .removeGroupsLeftFromUsers(Collections.emptySet(), "GroupId1", this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void removeGroupFromUsers_validInputDbDiesDuringUpdate_failureResult() {
    doReturn(this.table, null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.usersManager
        .removeGroupsLeftFromUsers(Sets.newHashSet("User3", "User4"), "GroupId1", this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(2)).getTable(any(String.class));
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  ///////////////////////////endregion
  // markEventAsSeen tests //
  ///////////////////////////region
  @Test
  public void markEventAsSeen_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.usersManager
        .markEventAsSeen(markEventAsSeenGoodInput, this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void markEventAsSeen_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.usersManager
        .markEventAsSeen(markEventAsSeenGoodInput, this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void markEventAsSeen_missingRequestKeys_failureResult() {
    ResultStatus resultStatus = this.usersManager
        .markEventAsSeen(Collections.emptyMap(), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  ////////////////////////////endregion
  // setUserGroupMute tests //
  ////////////////////////////region
  @Test
  public void setUserGroupMute_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.usersManager
        .setUserGroupMute(setUserGroupMuteGoodInput, this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void setUserGroupMute_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.usersManager
        .setUserGroupMute(setUserGroupMuteGoodInput, this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void setUserGroupMute_missingRequestKeys_failureResult() {
    ResultStatus resultStatus = this.usersManager
        .setUserGroupMute(Collections.emptyMap(), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  /////////////////////////////endregion
  // markAllEventsSeen tests //
  /////////////////////////////region
  @Test
  public void markAllEventsSeen_validInput_successfulResult() {
    doReturn(this.table).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.usersManager
        .markAllEventsSeen(markAllEventsSeenGoodInput, this.metrics);

    assertTrue(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(1)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void markAllEventsSeen_noDbConnection_failureResult() {
    doReturn(null).when(this.dynamoDB).getTable(any(String.class));

    ResultStatus resultStatus = this.usersManager
        .markAllEventsSeen(markAllEventsSeenGoodInput, this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(1)).getTable(any(String.class));
    verify(this.table, times(0)).getItem(any(GetItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void markAllEventsSeen_missingRequestKeys_failureResult() {
    ResultStatus resultStatus = this.usersManager
        .markAllEventsSeen(Collections.emptyMap(), this.metrics);

    assertFalse(resultStatus.success);
    verify(this.dynamoDB, times(0)).getTable(any(String.class));
    verify(this.table, times(0)).updateItem(any(UpdateItemSpec.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  //endregion
}
