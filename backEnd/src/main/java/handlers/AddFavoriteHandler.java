package handlers;

import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.UpdateItemData;

public class AddFavoriteHandler implements ApiRequestHandler {

  private final DbAccessManager dbAccessManager;
  private final Metrics metrics;

  @Inject
  public AddFavoriteHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method is used to add a singular user as a favorite to the activeUser object.
   *
   * @param activeUser       The username of the user making the api request. They are adding a
   *                         favorite to their favorites map.
   * @param favoriteUsername The username of the favorite being added.
   * @return Standard result status object giving insight on whether the request was successful
   */
  public ResultStatus handle(final String activeUser, final String favoriteUsername) {
    final String classMethod = "AddFavoriteHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {

      //add the active user to the new favorite-user's FavoriteOf map
      final UpdateItemData setFavoriteOfEntry = new UpdateItemData(favoriteUsername,
          DbAccessManager.USERS_TABLE_NAME)
          .withUpdateExpression("set " + User.FAVORITE_OF + ".#activeUser = :true")
          .withNameMap(new NameMap().with("#activeUser", activeUser))
          .withValueMap(new ValueMap().with(":true", true));

      //add the new favorite-user to the active user's Favorites map
      final User newFavoriteUser = this.dbAccessManager.getUser(favoriteUsername);

      final UpdateItemData setFavoritesEntry = new UpdateItemData(activeUser,
          DbAccessManager.USERS_TABLE_NAME)
          .withUpdateExpression("set " + User.FAVORITES + ".#newFavoriteUser = :newFavorite")
          .withNameMap(new NameMap().with("#newFavoriteUser", favoriteUsername))
          .withValueMap(
              new ValueMap().withMap(":newFavorite", newFavoriteUser.asMember().asMap()));

      final List<TransactWriteItem> actions = new ArrayList<>();
      actions.add(new TransactWriteItem().withUpdate(setFavoriteOfEntry.asUpdate()));
      actions.add(new TransactWriteItem().withUpdate(setFavoritesEntry.asUpdate()));

      this.dbAccessManager.executeWriteTransaction(actions);

      resultStatus = ResultStatus.successful("Favorite added successfully.");
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
