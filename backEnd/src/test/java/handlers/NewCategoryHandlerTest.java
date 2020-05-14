package handlers;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.UpdateItemData;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class NewCategoryHandlerTest {

  private NewCategoryHandler newCategoryHandler;

  @Mock
  private DbAccessManager dbAccessManager;

  @Mock
  private UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler;

  @Mock
  private Metrics metrics;

  //Notice: Do not change this variable definition unless you want to break and fix tests
  private final Item newCategoryGoodUser = new Item().withMap(User.OWNED_CATEGORIES,
      (ImmutableMap.of("catId", "catName", "catId2", "catName2")));

  private final Item newCategoryInvalidUser = new Item().withMap(User.OWNED_CATEGORIES,
      new HashMap<String, String>() {{
        put("duplicateName", "Category Name"); // duplicate the name in the input
        //insert the max allowed amount -> this will force the addition to try to eclipse this
        for (int i = 0; i < NewCategoryHandler.MAX_NUMBER_OF_CATEGORIES; i++) {
          put("catId" + (new Integer(i)).toString(), "catName" + (new Integer(i)).toString());
        }
      }});

  private ResultStatus<UpdateItemData> updateUserChoiceRatingResult = new ResultStatus<>(true,
      new UpdateItemData("ActiveUser", DbAccessManager.USERS_TABLE_NAME), "usersManagerWorks");

  private ArrayList<Object> addNewCategoryInputs = new ArrayList<Object>() {{
    add("ActiveUser"); // active user
    add("Category Name"); // category name
    add(Maps.newHashMap(ImmutableMap.of("0", "label 1", "2", "label 2"))); // choices
    add(Maps.newHashMap(ImmutableMap.of("0", 5, "2", 4))); // ratings
  }};

  private ArrayList<Class> inputClasses = new ArrayList<Class>() {{
    add(String.class);
    add(String.class);
    add(Map.class);
    add(Map.class);
  }};

  @BeforeEach
  private void init() {
    this.newCategoryHandler = new NewCategoryHandler(this.dbAccessManager,
        this.updateUserChoiceRatingsHandler, this.metrics);
  }

  @Test
  public void addNewCategory_validInput_successfulResult() throws Exception {
    doReturn(this.updateUserChoiceRatingResult).when(this.updateUserChoiceRatingsHandler)
        .handle(any(String.class), any(String.class), any(Map.class), eq(false), any(String.class),
            eq(true));
    doReturn(new User(newCategoryGoodUser)).when(this.dbAccessManager)
        .getUser(any(String.class));

    final ResultStatus resultStatus = this.getResult();

    assertTrue(resultStatus.success);
    assertEquals(2, mockingDetails(this.dbAccessManager).getInvocations().size());
    verify(this.dbAccessManager, times(1)).executeWriteTransaction(any(List.class));
    verify(this.dbAccessManager, times(1)).getUser(any(String.class));
    verify(this.metrics, times(2)).commonClose(true);
  }

  @Test
  public void addNewCategory_validInputBadUserRatingsUpdate_failureResult() throws Exception {
    doReturn(new ResultStatus(false, "usersManagerBroken"))
        .when(this.updateUserChoiceRatingsHandler)
        .handle(any(String.class), any(String.class), any(Map.class), eq(false), any(String.class),
            eq(true));
    doReturn(new User(newCategoryGoodUser)).when(this.dbAccessManager)
        .getUser(any(String.class));

    final ResultStatus resultStatus = this.getResult();

    assertFalse(resultStatus.success);
    assertEquals(1, mockingDetails(this.dbAccessManager).getInvocations().size());
    verify(this.dbAccessManager, times(1)).getUser(any(String.class));
    verify(this.metrics, times(1)).commonClose(true);
    verify(this.metrics, times(1)).commonClose(false);
  }

  @Test
  public void addNewCategory_validInputBadUserGet_failureResult() throws Exception {
    doThrow(NullPointerException.class).when(this.dbAccessManager)
        .getUser(any(String.class));

    ResultStatus resultStatus = this.getResult();

    assertFalse(resultStatus.success);
    verifyNoInteractions(this.updateUserChoiceRatingsHandler);
    assertEquals(1, mockingDetails(this.dbAccessManager).getInvocations().size());
    verify(this.dbAccessManager, times(1)).getUser(any(String.class));
    verify(this.metrics, times(2)).commonClose(false);
    verify(this.metrics, times(0)).commonClose(true);
  }

  @Test
  public void addNewCategory_invalidInput_failureResult() throws Exception {
    this.addNewCategoryInputs.set(2, Collections.emptyMap());
    doReturn(new User(newCategoryInvalidUser)).when(this.dbAccessManager)
        .getUser(any(String.class));

    ResultStatus resultStatus = this.getResult();

    assertFalse(resultStatus.success);
    verifyNoInteractions(this.updateUserChoiceRatingsHandler);
    assertEquals(1, mockingDetails(this.dbAccessManager).getInvocations().size());
    verify(this.dbAccessManager, times(1)).getUser(any(String.class));
    verify(this.metrics, times(2)).commonClose(false);
    verify(this.metrics, times(0)).commonClose(true);
  }

  @Test
  public void addNewCategory_invalidInput2_failureResult() throws Exception {
    //testing no empty choice labels and no empty category names
    this.addNewCategoryInputs.set(2, ImmutableMap.of("0", "")); // empty choice label
    this.addNewCategoryInputs.set(1, ""); // empty name
    doReturn(new User(newCategoryGoodUser)).when(this.dbAccessManager)
        .getUser(any(String.class));

    ResultStatus resultStatus = this.getResult();

    assertFalse(resultStatus.success);
    verifyNoInteractions(this.updateUserChoiceRatingsHandler);
    assertEquals(1, mockingDetails(this.dbAccessManager).getInvocations().size());
    verify(this.dbAccessManager, times(1)).getUser(any(String.class));
    verify(this.metrics, times(2)).commonClose(false);
    verify(this.metrics, times(0)).commonClose(true);
  }

  @Test
  public void addNewCategory_exceptionOnTransaction_failureResult() throws Exception {
    doThrow(NullPointerException.class).when(this.dbAccessManager)
        .executeWriteTransaction(any(List.class));
    doReturn(this.updateUserChoiceRatingResult).when(this.updateUserChoiceRatingsHandler)
        .handle(any(String.class), any(String.class), any(Map.class), eq(false), any(String.class),
            eq(true));
    doReturn(new User(newCategoryGoodUser)).when(this.dbAccessManager)
        .getUser(any(String.class));

    ResultStatus resultStatus = this.getResult();

    assertFalse(resultStatus.success);
    assertEquals(2, mockingDetails(this.dbAccessManager).getInvocations().size());
    verify(this.dbAccessManager, times(1)).getUser(any(String.class));
    verify(this.dbAccessManager, times(1)).executeWriteTransaction(any(List.class));
    verify(this.metrics, times(1)).commonClose(false);
    verify(this.metrics, times(1)).commonClose(true);
  }

  private ResultStatus<?> getResult() throws Exception {
    try {
      return (ResultStatus<?>) this
          .findAndCallMethod(this.newCategoryHandler, "handle",
              this.addNewCategoryInputs, this.inputClasses);
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
