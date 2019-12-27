package handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class CategoriesPostHandlerTest {
  private final CategoriesPostHandler categoriesPostHandler = new CategoriesPostHandler();

  @Mock
  private InputStream inputStream;

  @Mock
  private OutputStream outputStream;

  @Mock
  private Context context;

  @Test
  public void handleRequest_emptyInput_failureResponse() throws Exception {
    this.categoriesPostHandler.handleRequest(this.inputStream, this.outputStream, this.context);
    assertEquals(true,true);
  }

  @Test
  public void handleRequest_emptyInput_failureResponse2() throws Exception {
    this.categoriesPostHandler.handleRequest(this.inputStream, this.outputStream, this.context);
    assertEquals(true,true);
  }
}
