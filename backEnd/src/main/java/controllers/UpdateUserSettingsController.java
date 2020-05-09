package controllers;

import exceptions.InvalidAttributeValueException;
import exceptions.MissingApiRequestKeyException;
import handlers.UpdateUserSettingsHandler;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import models.AppSettings;
import models.User;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class UpdateUserSettingsController implements ApiRequestController {

  @Inject
  public UpdateUserSettingsHandler updateUserSettingsHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "UpdateUserSettingsController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, User.DISPLAY_NAME, User.APP_SETTINGS, User.FAVORITES);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String newDisplayName = (String) jsonMap.get(User.DISPLAY_NAME);
        final AppSettings newAppSettings = new AppSettings(
            (Map<String, Object>) jsonMap.get(User.APP_SETTINGS));
        final Set<String> newFavorites = new HashSet<>(
            (List<String>) jsonMap.get(User.FAVORITES)); // note this comes in as list, in db is map
        final List<Integer> newIconData = (List<Integer>) jsonMap.get(User.ICON);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.updateUserSettingsHandler
            .handle(activeUser, newDisplayName, newAppSettings, newFavorites, newIconData);
      } catch (final InvalidAttributeValueException iae) {
        metrics.logWithBody(new ErrorDescriptor<>(classMethod, iae));
        resultStatus = ResultStatus.failure(iae.getMessage());
      } catch (final Exception e) {
        metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
        resultStatus = ResultStatus.failure("Error: Unable to parse request.");
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}
