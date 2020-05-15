package handlers;

import static junit.framework.TestCase.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import exceptions.InvalidAttributeValueException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import managers.DbAccessManager;
import models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utilities.JsonUtils;
import utilities.Metrics;
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

  private ArrayList<Object> updateUserChoiceRatingsInputs = new ArrayList<Object>() {{
    add("john_andrews12"); // active user
    add("ef8dfc02-a79d-4d55-bb03-654a7a31bb16"); // categoryId
    add(2); // category version
    //removing 1's mapping and adding mapping for 3
    add(Maps.newHashMap(ImmutableMap.of("2", 5, "3", 4))); // ratings
    add(true); // updateDb
    //add("TestName");
  }};

  private ArrayList<Class> inputClasses = new ArrayList<Class>() {{
    add(String.class);
    add(String.class);
    add(Integer.class);
    add(Map.class);
    add(boolean.class);
  }};

  @BeforeEach
  private void init() {
    this.updateUserChoiceRatingsHandler = new UpdateUserChoiceRatingsHandler(this.dbAccessManager,
        this.metrics);
  }

  @Test
  public void updateUserChoiceRatings_validInput_successfulResult() {
    try {
      doReturn(new User(JsonUtils.getItemFromFile("john_andrews12.json")))
          .when(this.dbAccessManager).getUser(any(String.class));

      final ResultStatus resultStatus = this.getResult();

      assertTrue(resultStatus.success);
      assertEquals(2, mockingDetails(this.dbAccessManager).getInvocations().size());
      verify(this.dbAccessManager, times(1)).updateUser(any(UpdateItemData.class));
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

      this.updateUserChoiceRatingsInputs.set(1, "new-id"); // new cat id
      this.updateUserChoiceRatingsInputs.set(4, false); // don't update the db
      this.updateUserChoiceRatingsInputs.add("TestName"); // categoryName
      this.updateUserChoiceRatingsInputs.add(true); // isNewCategory
      this.inputClasses.add(String.class);
      this.inputClasses.add(boolean.class);

      final ResultStatus<UpdateItemData> resultStatus = (ResultStatus<UpdateItemData>) this
          .getResult();
      assertTrue(resultStatus.success);

      //here we're making sure the category name DOES get into the update data
      assertTrue(resultStatus.data.getValueMap().containsKey(":categoryName"));

      assertEquals(1, mockingDetails(this.dbAccessManager).getInvocations().size());
      verify(this.dbAccessManager, times(1)).getUser(any(String.class));
      verify(this.metrics, times(1)).commonClose(true);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateUserChoiceRatings_validInputWithNameChange_successfulResult() {
    try {
      doReturn(new User(JsonUtils.getItemFromFile("john_andrews12.json")))
          .when(this.dbAccessManager)
          .getUser(any(String.class));

      this.updateUserChoiceRatingsInputs.add("NewName"); // category name
      this.inputClasses.add(String.class);

      final ResultStatus resultStatus = this.getResult();

      assertTrue(resultStatus.success);
      assertEquals(2, mockingDetails(this.dbAccessManager).getInvocations().size());
      verify(this.dbAccessManager, times(1)).updateUser(any(UpdateItemData.class));
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

      this.updateUserChoiceRatingsInputs.set(1, "new-id"); // new cat id
      this.updateUserChoiceRatingsInputs.add("TestName"); // categoryName
      this.inputClasses.add(String.class);

      final ResultStatus<UpdateItemData> resultStatus = (ResultStatus<UpdateItemData>) this
          .getResult();
      assertTrue(resultStatus.success);

      //here we're making sure the category name didn't get updated when using the wrong method
      assertFalse(resultStatus.data.getValueMap().containsKey(":categoryName"));

      assertEquals(2, mockingDetails(this.dbAccessManager).getInvocations().size());
      verify(this.dbAccessManager, times(1)).updateUser(any(UpdateItemData.class));
      verify(this.dbAccessManager, times(1)).getUser(any(String.class));
      verify(this.metrics, times(1)).commonClose(true);
    } catch (final Exception e) {
      System.out.println(e);
      fail();
    }
  }

  @Test
  public void updateUserChoiceRatings_mapRatingValues_failureResult()
      throws Exception {
    this.updateUserChoiceRatingsInputs
        .set(3, ImmutableMap.of("1", 10)); // greater than 5
    ResultStatus resultStatus = this.getResult();
    assertFalse(resultStatus.success);

    this.updateUserChoiceRatingsInputs
        .set(3, ImmutableMap.of("1", -5)); // less than 0
    resultStatus = this.getResult();
    assertFalse(resultStatus.success);

    this.updateUserChoiceRatingsInputs
        .set(3, ImmutableMap.of("1", "not a number")); // NaN
    resultStatus = this.getResult();
    assertFalse(resultStatus.success);

    verifyNoInteractions(this.dbAccessManager);
    verify(this.metrics, times(3)).commonClose(false);
  }

  @Test
  public void updateUserChoiceRatings_noDbConnection_failureResult()
      throws Exception {
    doThrow(InvalidAttributeValueException.class).when(this.dbAccessManager)
        .getUser(any(String.class));

    final ResultStatus resultStatus = this.getResult();

    assertFalse(resultStatus.success);
    assertEquals(1, mockingDetails(this.dbAccessManager).getInvocations().size());
    verify(this.dbAccessManager, times(1)).getUser(any(String.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  private ResultStatus<?> getResult() throws Exception {
    try {
      return (ResultStatus<?>) this
          .findAndCallMethod(this.updateUserChoiceRatingsHandler, "handle",
              this.updateUserChoiceRatingsInputs, this.inputClasses);
    } catch (final Exception e) {
      throw e;
    }
  }

  private Object findAndCallMethod(final Object object, final String methodName,
      final List<Object> argumentList, final List<Class> argumentTypes)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method m = object.getClass().getMethod(methodName, argumentTypes.toArray(new Class[0]));
    return m.invoke(object, argumentList.toArray());
  }
}
