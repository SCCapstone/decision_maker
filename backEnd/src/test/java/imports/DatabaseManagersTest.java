package imports;

import static junit.framework.TestCase.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class DatabaseManagersTest {
  //this is mainly just to get 100% coverage, there is no need for testing this class as all it
  //holds is static class instances
  @Test
  public void constructor_DatabaseManagers_successfulResult() {
    final DatabaseManagers databaseManagers = new DatabaseManagers();
    assertTrue(databaseManagers != null);
  }
}
