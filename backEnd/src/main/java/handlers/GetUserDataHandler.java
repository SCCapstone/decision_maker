package handlers;

import com.amazonaws.services.dynamodbv2.document.Item;
import java.util.Collections;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.AppSettings;
import models.User;
import models.UserForApiResponse;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.ResultStatus;

public class GetUserDataHandler implements ApiRequestHandler {

  private static final String DEFAULT_DISPLAY_NAME = "New User";

  private DbAccessManager dbAccessManager;
  private Metrics metrics;

  @Inject
  public GetUserDataHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method gets another user's data based on the passed in username. If the requested user's
   * data does not exist, the magic string of 'User not found.' is returned.
   *
   * @param username The username to get the data of.
   * @return Standard result status object giving insight on whether the request was successful
   */
  public ResultStatus handleUsername(final String username) {
    final String classMethod = "GetUserDataHandler.GetUserDataHandler";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Item user = this.dbAccessManager.getUserItem(username);
      if (user != null) {
        resultStatus = ResultStatus
            .successful(JsonUtils.convertObjectToJson(new UserForApiResponse(user).asMap()));
      } else {
        resultStatus = ResultStatus.successful("User not found.");
      }
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This method gets the active user's data. If the active user's data does not exist, we assume
   * this is their first login and we enter a new user object in the db.
   *
   * @param activeUser The user that made the api request, trying to get data about themselves.
   * @return Standard result status object giving insight on whether the request was successful
   */
  public ResultStatus handleActiveUser(final String activeUser) {
    final String classMethod = "GetUserDataHandler.handleActiveUser";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      UserForApiResponse userForApiResponse;
      Item user = this.dbAccessManager.getUserItem(activeUser);

      if (user == null) {
        user = new Item()
            .withString(User.USERNAME, activeUser)
            .withString(User.DISPLAY_NAME, DEFAULT_DISPLAY_NAME)
            .withNull(User.ICON)
            .withMap(User.APP_SETTINGS, AppSettings.defaultSettings().asMap())
            .withMap(User.CATEGORY_RATINGS, Collections.emptyMap())
            .withMap(User.OWNED_CATEGORIES, Collections.emptyMap())
            .withMap(User.GROUPS, Collections.emptyMap())
            .withMap(User.GROUPS_LEFT, Collections.emptyMap())
            .withNumber(User.OWNED_GROUPS_COUNT, 0)
            .withMap(User.FAVORITES, Collections.emptyMap())
            .withMap(User.FAVORITE_OF, Collections.emptyMap());

        this.dbAccessManager.putUser(user);

        userForApiResponse = new UserForApiResponse(user, true);
      } else {
        userForApiResponse = new UserForApiResponse(user);
      }

      resultStatus = ResultStatus
          .successful(JsonUtils.convertObjectToJson(userForApiResponse.asMap()));
    } catch (Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
