package imports;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utilities.Metrics;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class GroupsManagerTest {

  private GroupsManager groupsManager;

  @Mock
  private Table table;

  @Mock
  private DynamoDB dynamoDB;

  @Mock
  private CategoriesManager categoriesManager;

  @Mock
  private UsersManager usersManager;

  @Mock
  private LambdaLogger lambdaLogger;

  @Mock
  private Metrics metrics;

  @BeforeEach
  private void init() {
    this.groupsManager = new GroupsManager(this.dynamoDB);

    DatabaseManagers.CATEGORIES_MANAGER = this.categoriesManager;
    DatabaseManagers.USERS_MANAGER = this.usersManager;
    DatabaseManagers.GROUPS_MANAGER = this.groupsManager;
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

  ////////////////////////////////////endregion
  // removeCategoryFromGroups tests //
  ////////////////////////////////////region

  //endregion
}
