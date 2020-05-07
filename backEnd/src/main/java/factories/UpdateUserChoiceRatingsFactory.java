package factories;

import handlers.UpdateUserChoiceRatingsHandler;
import java.util.Map;
import utilities.Metrics;

public interface UpdateUserChoiceRatingsFactory {

  UpdateUserChoiceRatingsHandler create(Map<String, Object> requestBody, Metrics metrics);
}
