package imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.util.ImmutableMapParameter;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import utilities.RequestFields;
import utilities.ResultStatus;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
@PrepareForTest(UsersManager.class)
public class CategoriesManagerTest {
  private final Map<String, Object> badInput = ImmutableMapParameter.of("NotRequired", "missing keys");

  private final Map<String, Object> newCategoryGoodInput = ImmutableMapParameter.of(
      CategoriesManager.CATEGORY_NAME, "CategoryName",
      CategoriesManager.CHOICES, ImmutableMapParameter.of("0", "First choice."),
      RequestFields.USER_RATINGS, ImmutableMapParameter.of("0", "First rating."),
      RequestFields.ACTIVE_USER, "ActiveUser"
  );

  @Mock
  private CategoriesManager categoriesManager;

  @Mock
  private UsersManager usersManager;

  @BeforeEach
  private void init() {
    CategoriesManager.CATEGORIES_MANAGER = this.categoriesManager;
    UsersManager.USERS_MANAGER = this.usersManager;
  }

  @Test
  public void addNewCategory_validInput_successfulResult() {
    ResultStatus resultStatus = CategoriesManager.addNewCategory(this.newCategoryGoodInput);
    System.out.println(resultStatus.resultMessage);
    assertTrue(resultStatus.success);
    verify(this.categoriesManager, times(1)).putItem(any(PutItemSpec.class));
  }

  @Test
  public void addNewCategory_missingKey_failureResult() {
    PowerMockito.mockStatic(UsersManager.class);
    ResultStatus resultStatus = CategoriesManager.addNewCategory(this.badInput);
    assertFalse(resultStatus.success);
    verify(this.categoriesManager, times(0)).putItem(any(PutItemSpec.class));

    verifyStatic(times(0));
    UsersManager.updateUserChoiceRatings(any(Map.class));
    //verify(this.usersManager, times(0)).putItem(any(PutItemSpec.class));

  }
}
