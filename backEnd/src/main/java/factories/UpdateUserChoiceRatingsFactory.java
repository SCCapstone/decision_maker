package factories;

import handlers.UpdateUserChoiceRatingsHandler;
import utilities.Metrics;

public interface UpdateUserChoiceRatingsFactory {

  UpdateUserChoiceRatingsHandler create(Metrics metrics);
}
