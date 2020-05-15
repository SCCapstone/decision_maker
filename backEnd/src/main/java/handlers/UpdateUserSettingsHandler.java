package handlers;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import managers.DbAccessManager;
import managers.S3AccessManager;
import models.AppSettings;
import models.Group;
import models.User;
import models.UserForApiResponse;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.UpdateItemData;
import utilities.WarningDescriptor;

public class UpdateUserSettingsHandler implements ApiRequestHandler {

  private static final int MAX_DISPLAY_NAME_LENGTH = 40;

  private final DbAccessManager dbAccessManager;
  private final S3AccessManager s3AccessManager;
  private final Metrics metrics;

  @Inject
  public UpdateUserSettingsHandler(final DbAccessManager dbAccessManager,
      final S3AccessManager s3AccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.s3AccessManager = s3AccessManager;
    this.metrics = metrics;
  }

  /**
   * This method handles the api request to update a users settings such as display name, icon,
   * favorites, and other app settings.
   *
   * @param activeUser     Common request map from endpoint handler containing api input
   * @param newDisplayName Standard metrics object for profiling and logging
   * @param newAppSettings This is the new app settings being set.
   * @param newFavorites   This is a set of usernames that is the user's new list of favorites.
   * @param newIconData    This is the byte data for an image file. If set, the user is updating.
   * @return Standard result status object giving insight on whether the request was successful
   */
  public ResultStatus handle(final String activeUser, final String newDisplayName,
      final AppSettings newAppSettings, final Set<String> newFavorites,
      final List<Integer> newIconData) {
    final String classMethod = "UpdateUserSettingsHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    /*
     If the user's display name or the icon changes:
       loop through a user's groups and favorites-of and update accordingly
     If a user's Favorites change
       need to reach out and pull new favorites data in
       need to go out and delete favorites of map from removed favorites
     Blind update the app settings to simplify the code
     */

    try {
      final Optional<String> errorMessage = this.userSettingsIsValid(newDisplayName);
      if (!errorMessage.isPresent()) {
        final User oldUser = this.dbAccessManager.getUser(activeUser);

        //as long as this remains a small group of settings, I think it's okay to always overwrite
        //this does imply that the entire appSettings array is sent from the front end though
        String updateUserExpression = "set " + User.APP_SETTINGS + " = :appSettings";
        ValueMap userValueMap = new ValueMap().withMap(":appSettings", newAppSettings.asMap());

        String updateGroupsExpression = null;
        ValueMap groupsValueMap = new ValueMap();
        NameMap groupsNameMap = new NameMap();

        String updateFavoritesOfExpression = null;
        ValueMap favoritesOfValueMap = new ValueMap();
        NameMap favoritesOfNameMap = new NameMap();

        //determine if the display name/icon have changed
        if (!oldUser.getDisplayName().equals(newDisplayName)) {
          updateUserExpression += ", " + User.DISPLAY_NAME + " = :name";
          userValueMap.withString(":name", newDisplayName);

          updateGroupsExpression = this.getUpdateString(updateGroupsExpression,
              Group.MEMBERS + ".#username." + User.DISPLAY_NAME, ":displayName");
          groupsValueMap.withString(":displayName", newDisplayName);
          groupsNameMap.with("#username", activeUser);

          updateFavoritesOfExpression = this
              .getUpdateString(updateFavoritesOfExpression,
                  User.FAVORITES + ".#username." + User.DISPLAY_NAME,
                  ":displayName");
          favoritesOfValueMap.withString(":displayName", newDisplayName);
          favoritesOfNameMap.with("#username", activeUser);
        }

        //ICON is an optional api payload key, if present it's assumed it has the contents of a new file for upload
        if (newIconData != null) {
          //try to create the file in s3, if no filename returned, throw exception
          final String newIconFileName = this.s3AccessManager.uploadImage(newIconData, metrics)
              .orElseThrow(Exception::new);

          updateUserExpression += ", " + User.ICON + " = :icon";
          userValueMap.withString(":icon", newIconFileName);

          updateGroupsExpression = this.getUpdateString(updateGroupsExpression,
              Group.MEMBERS + ".#username2." + User.ICON, ":icon");
          groupsValueMap.withString(":icon", newIconFileName);
          groupsNameMap.with("#username2", activeUser);

          updateFavoritesOfExpression = this
              .getUpdateString(updateFavoritesOfExpression,
                  User.FAVORITES + ".#username2." + User.ICON,
                  ":icon");
          favoritesOfValueMap.withString(":icon", newIconFileName);
          favoritesOfNameMap.with("#username2", activeUser);
        }

        UpdateItemSpec updateUserItemSpec = new UpdateItemSpec()
            .withUpdateExpression(updateUserExpression)
            .withValueMap(userValueMap);

        this.dbAccessManager.updateUser(activeUser, updateUserItemSpec);

        if (updateGroupsExpression != null) {
          final UpdateItemSpec updateGroupItemSpec = new UpdateItemSpec()
              .withUpdateExpression(updateGroupsExpression)
              .withValueMap(groupsValueMap)
              .withNameMap(groupsNameMap);

          for (String groupId : oldUser.getGroups().keySet()) {
            try {
              this.dbAccessManager.updateGroup(groupId, updateGroupItemSpec);
            } catch (final Exception e) {
              this.metrics.log(new ErrorDescriptor<>(groupId, classMethod, e));
            }
          }
        }

        if (updateFavoritesOfExpression != null) {
          final UpdateItemSpec updateFavoritesOfItemSpec = new UpdateItemSpec()
              .withUpdateExpression(updateFavoritesOfExpression)
              .withValueMap(favoritesOfValueMap)
              .withNameMap(favoritesOfNameMap);

          //all of the users that this user is a favorite of need to be updated
          for (String username : oldUser.getFavoriteOf().keySet()) {
            try {
              this.dbAccessManager.updateUser(username, updateFavoritesOfItemSpec);
            } catch (final Exception e) {
              this.metrics.log(new ErrorDescriptor<>(username, classMethod, e));
            }
          }
        }

        this.updateActiveUsersFavorites(newFavorites, oldUser.getFavorites().keySet(), activeUser);

        final UserForApiResponse updatedUser = new UserForApiResponse(
            this.dbAccessManager.getUser(activeUser));
        resultStatus = ResultStatus.successful(JsonUtils.convertObjectToJson(updatedUser.asMap()));
      } else {
        metrics.logWithBody(new WarningDescriptor<>(classMethod, errorMessage.get()));
        resultStatus.resultMessage = errorMessage.get();
      }
    } catch (final Exception e) {
      metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private Optional<String> userSettingsIsValid(final String displayName) {
    String errorMessage = null;

    if (displayName.length() <= 0) {
      errorMessage = "Error: Display name cannot be empty.";
    } else if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
      errorMessage =
          "Error: Display name cannot be longer than " + MAX_DISPLAY_NAME_LENGTH + "characters.";
    }

    return Optional.ofNullable(errorMessage);
  }

  private boolean updateActiveUsersFavorites(final Set<String> newFavorites,
      final Set<String> oldFavorites, final String activeUser) {
    final String classMethod = "UpdateUserSettingsHandler.updateActiveUsersFavorites";
    this.metrics.commonSetup(classMethod);

    boolean hadError = false;

    //If there are missing favorites, go through and remove the favoritesOf for each of these users
    if (!newFavorites.containsAll(oldFavorites)) {
      final Set<String> removedUsernames = new HashSet<>(oldFavorites);
      removedUsernames.removeAll(newFavorites);

      for (String username : removedUsernames) {

        try {
          //remove the active user from their former favorite-user's FavoriteOf map
          final UpdateItemData removeFavoriteOfEntry = new UpdateItemData(username,
              DbAccessManager.USERS_TABLE_NAME)
              .withUpdateExpression("remove " + User.FAVORITE_OF + ".#activeUser")
              .withNameMap(new NameMap().with("#activeUser", activeUser));

          //remove the former favorite-user from the active user's Favorites map
          final UpdateItemData removeFavoritesEntry = new UpdateItemData(activeUser,
              DbAccessManager.USERS_TABLE_NAME)
              .withUpdateExpression("remove " + User.FAVORITES + ".#oldFavoriteUser")
              .withNameMap(new NameMap().with("#oldFavoriteUser", username));

          final List<TransactWriteItem> actions = new ArrayList<>();
          actions.add(new TransactWriteItem().withUpdate(removeFavoriteOfEntry.asUpdate()));
          actions.add(new TransactWriteItem().withUpdate(removeFavoritesEntry.asUpdate()));

          this.dbAccessManager.executeWriteTransaction(actions);
        } catch (final Exception e) {
          hadError = true;
          this.metrics.log(new ErrorDescriptor<>(username, classMethod, e));
        }
      }
    }

    //If there are new favorites, go through and update the favoritesOf for each of these users
    if (!oldFavorites.containsAll(newFavorites)) {
      final Set<String> addedUsernames = new HashSet<>(newFavorites);
      addedUsernames.removeAll(oldFavorites);

      for (String username : addedUsernames) {
        try {
          //add the active user to the new favorite-user's FavoriteOf map
          final UpdateItemData setFavoriteOfEntry = new UpdateItemData(username,
              DbAccessManager.USERS_TABLE_NAME)
              .withUpdateExpression("set " + User.FAVORITE_OF + ".#activeUser = :true")
              .withNameMap(new NameMap().with("#activeUser", activeUser))
              .withValueMap(new ValueMap().with(":true", true));

          //add the new favorite-user to the active user's Favorites map
          final User newFavoriteUser = this.dbAccessManager.getUser(username);

          final UpdateItemData setFavoritesEntry = new UpdateItemData(activeUser,
              DbAccessManager.USERS_TABLE_NAME)
              .withUpdateExpression("set " + User.FAVORITES + ".#newFavoriteUser = :newFavorite")
              .withNameMap(new NameMap().with("#newFavoriteUser", username))
              .withValueMap(
                  new ValueMap().withMap(":newFavorite", newFavoriteUser.asMember().asMap()));

          final List<TransactWriteItem> actions = new ArrayList<>();
          actions.add(new TransactWriteItem().withUpdate(setFavoriteOfEntry.asUpdate()));
          actions.add(new TransactWriteItem().withUpdate(setFavoritesEntry.asUpdate()));

          this.dbAccessManager.executeWriteTransaction(actions);
        } catch (final Exception e) {
          hadError = true;
          this.metrics.log(new ErrorDescriptor<>(username, classMethod, e));
        }
      }
    }

    this.metrics.commonClose(!hadError);
    return (!hadError);
  }

  private String getUpdateString(String current, String key, String valueName) {
    if (current != null) {
      return current + ", " + key + " = " + valueName;
    } else {
      return "set " + key + " = " + valueName;
    }
  }
}
