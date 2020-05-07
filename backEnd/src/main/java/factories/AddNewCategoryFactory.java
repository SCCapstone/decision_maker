package factories;

import handlers.AddNewCategoryHandler;
import handlers.UpdateUserChoiceRatingsHandler;
import java.util.Map;
import utilities.Metrics;

public interface AddNewCategoryFactory {

  AddNewCategoryHandler create(UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler,
      Map<String, Object> requestBody, Metrics metrics);
}
