package imports;

import static junit.framework.TestCase.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.google.common.collect.ImmutableMap;
import exceptions.InvalidAttributeValueException;
import exceptions.MissingApiRequestKeyException;
import java.util.HashMap;
import java.util.Map;
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
import utilities.UpdateItemData;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class UpdateUserChoiceRatingsHandlerTest {

  private UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler;

  @Mock
  private DbAccessManager dbAccessManager;

  @Mock
  private Metrics metrics;

  private Map<String, Object> updateUserChoiceRatingsInput = new HashMap<String, Object>() {{
    put(RequestFields.ACTIVE_USER, "john_andrews12");
    put(Category.CATEGORY_ID, "ef8dfc02-a79d-4d55-bb03-654a7a31bb16");
    //removing 1's mapping and adding mapping for 3
    put(RequestFields.USER_RATINGS, ImmutableMap.of("2", 5, "3", 4));
    put(Category.CATEGORY_NAME, "TestName");
  }};

  @BeforeEach
  private void init() {
    this.updateUserChoiceRatingsHandler = new UpdateUserChoiceRatingsHandler(this.dbAccessManager,
        this.updateUserChoiceRatingsInput, this.metrics);
  }

  @Test
  public void updateUserChoiceRatings_validInput_successfulResult() {
    try {
      doReturn(new User(JsonUtils.getItemFromFile("john_andrews12.json")))
          .when(this.dbAccessManager).getUser(any(String.class));

      final ResultStatus resultStatus = this.updateUserChoiceRatingsHandler.handle();

      assertTrue(resultStatus.success);
      verify(this.dbAccessManager, times(1))
          .updateUser(any(String.class), any(UpdateItemSpec.class));
      verify(this.dbAccessManager, times(1)).getUser(any(String.class));
      verify(this.metrics, times(1)).commonClose(true);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateUserChoiceRatings_validInputNewCategory_successfulResult() {
    try {
      doReturn(new User(JsonUtils.getItemFromFile("john_andrews12.json")))
          .when(this.dbAccessManager).getUser(any(String.class));

      final ResultStatus<UpdateItemData> resultStatus = this.updateUserChoiceRatingsHandler
          .handle("john_andrews12", "new-id", ImmutableMap.of("1", 1, "2", 5, "3", 4), false, true);
      assertTrue(resultStatus.success);

      //here we're making sure the category name DOES get into the update data
      assertTrue(resultStatus.data.getValueMap().containsKey(":categoryName"));

      verify(this.dbAccessManager, times(1)).getUser(any(String.class));
      verify(this.dbAccessManager, times(0))
          .updateUser(any(String.class), any(UpdateItemSpec.class));
      verify(this.metrics, times(1)).commonClose(true);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateUserChoiceRatings_validInputNoNameChange_successfulResult() {
    try {
      doReturn(new User(JsonUtils.getItemFromFile("john_andrews12.json")))
          .when(this.dbAccessManager)
          .getUser(any(String.class));

      this.updateUserChoiceRatingsInput.remove(CategoriesManager.CATEGORY_NAME);
      final ResultStatus resultStatus = this.updateUserChoiceRatingsHandler.handle();

      assertTrue(resultStatus.success);
      verify(this.dbAccessManager, times(1))
          .updateUser(any(String.class), any(UpdateItemSpec.class));
      verify(this.dbAccessManager, times(1)).getUser(any(String.class));
      verify(this.metrics, times(1)).commonClose(true);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateUserChoiceRatings_validInputNewCategoryWrongMethod_successfulResultBadUpdate() {
    try {
      doReturn(new User(JsonUtils.getItemFromFile("john_andrews12.json")))
          .when(this.dbAccessManager).getUser(any(String.class));

      final ResultStatus resultStatus = this.updateUserChoiceRatingsHandler
          .handle("john_andrews12", "new-id", ImmutableMap.of("1", 1, "2", 5, "3", 4), true);
      assertTrue(resultStatus.success);

      //here we're making sure the category name didn't get updated when using the wrong method
      final ArgumentCaptor<UpdateItemSpec> argument = ArgumentCaptor.forClass(UpdateItemSpec.class);
      verify(this.dbAccessManager).updateUser(any(String.class), argument.capture());
      assertFalse(argument.getValue().getValueMap().containsKey(":categoryName"));

      verify(this.dbAccessManager, times(1))
          .updateUser(any(String.class), any(UpdateItemSpec.class));
      verify(this.dbAccessManager, times(1)).getUser(any(String.class));
      verify(this.metrics, times(1)).commonClose(true);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateUserChoiceRatings_mapRatingValues_failureResult()
      throws MissingApiRequestKeyException {
    this.updateUserChoiceRatingsInput
        .put(RequestFields.USER_RATINGS, ImmutableMap.of("1", 10)); // greater than 5
    ResultStatus resultStatus = this.updateUserChoiceRatingsHandler.handle();
    assertFalse(resultStatus.success);

    this.updateUserChoiceRatingsInput
        .put(RequestFields.USER_RATINGS, ImmutableMap.of("1", -5)); // less than 0
    resultStatus = this.updateUserChoiceRatingsHandler.handle();
    assertFalse(resultStatus.success);

    this.updateUserChoiceRatingsInput
        .put(RequestFields.USER_RATINGS, ImmutableMap.of("1", "not a number")); // NaN
    resultStatus = this.updateUserChoiceRatingsHandler.handle();
    assertFalse(resultStatus.success);

    verifyNoInteractions(this.dbAccessManager);
    verify(this.metrics, times(3)).commonClose(false);
  }

  @Test
  public void updateUserChoiceRatings_missingKey_failureResult() {
    try {
      this.updateUserChoiceRatingsInput.clear();
      this.updateUserChoiceRatingsHandler.handle();
    } catch (final MissingApiRequestKeyException marke) {
      verifyNoInteractions(this.dbAccessManager);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateUserChoiceRatings_badKeyValue_failureResult()
      throws InvalidAttributeValueException {
    try {
      this.updateUserChoiceRatingsInput.put(RequestFields.USER_RATINGS, "not a map");
      final ResultStatus resultStatus = this.updateUserChoiceRatingsHandler.handle();

      assertFalse(resultStatus.success);
      verifyNoInteractions(this.dbAccessManager);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateUserChoiceRatings_noDbConnection_failureResult()
      throws InvalidAttributeValueException, MissingApiRequestKeyException {
    doThrow(InvalidAttributeValueException.class).when(this.dbAccessManager)
        .getUser(any(String.class));

    final ResultStatus resultStatus = this.updateUserChoiceRatingsHandler.handle();

    assertFalse(resultStatus.success);
    verify(this.dbAccessManager, times(1)).getUser(any(String.class));
    verify(this.metrics, times(1)).commonClose(false);
  }
}
