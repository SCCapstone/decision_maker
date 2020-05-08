package factories;

import handlers.AddNewCategoryHandler;
import handlers.UpdateUserChoiceRatingsHandler;
import utilities.Metrics;

public interface AddNewCategoryFactory {

  AddNewCategoryHandler create(UpdateUserChoiceRatingsHandler updateUserChoiceRatingsHandler,
      Metrics metrics);
}
