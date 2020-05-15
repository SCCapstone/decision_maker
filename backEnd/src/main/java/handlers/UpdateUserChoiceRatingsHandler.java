package handlers;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.Category;
import models.User;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.UpdateItemData;
import utilities.WarningDescriptor;

public class UpdateUserChoiceRatingsHandler implements ApiRequestHandler {

  private final DbAccessManager dbAccessManager;
  private final Metrics metrics;

  @Inject
  public UpdateUserChoiceRatingsHandler(
      final DbAccessManager dbAccessManager,
      final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method takes in the active user, a category id, and a map of choice rating. Using this
   * information if updates a users CategoryRatings attribute map to contain the appropriate ratings
   * for the categoryId. It merges old choice ratings with new ones to preserve historical data.
   *
   * @param activeUser Common request map from endpoint handler containing api input
   * @param categoryId Standard metrics object for profiling and logging
   * @param ratings    This is the map of choice ids to user rate values
   * @param updateDb   This boolean tells the method whether or not it should update the db. If
   *                   false, this generally means the update data will be part of a transaction.
   * @return Standard result status object giving insight on whether the request was successful
   */
  public ResultStatus<UpdateItemData> handle(final String activeUser, final String categoryId,
      final Integer categoryVersion, final Map<String, Object> ratings, final boolean updateDb) {
    return this.handle(activeUser, categoryId, categoryVersion, ratings, updateDb, null, false);
  }

  public ResultStatus<UpdateItemData> handle(final String activeUser, final String categoryId,
      final Integer categoryVersion, final Map<String, Object> ratings, final boolean updateDb,
      final String categoryName) {
    return this
        .handle(activeUser, categoryId, categoryVersion, ratings, updateDb, categoryName, false);
  }

  //Same doc as above with the addition of the 'isNewCategory' param. This param tell the function
  //that the category has just been created and therefore the active user is the owner
  public ResultStatus<UpdateItemData> handle(final String activeUser, final String categoryId,
      final Integer categoryVersion, final Map<String, Object> ratings, final boolean updateDb,
      final String categoryName,
      final boolean isNewCategory) {
    final String classMethod = "UpdateUserChoiceRatingsHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Optional<String> errorMessage = this.userRatingsIsValid(ratings);
      if (!errorMessage.isPresent()) {
        final Map<String, Integer> ratingsMapConverted = ratings.entrySet().stream()
            .collect(collectingAndThen(
                toMap(Entry::getKey, (Map.Entry e) -> Integer.parseInt(e.getValue().toString())),
                HashMap::new));

        String updateExpression;
        NameMap nameMap;
        ValueMap valueMap;

        final User user = this.dbAccessManager.getUser(activeUser);
        if (user.getCategoryRatings().containsKey(categoryId)) {
          //we just need to insert the new version to ratings map
          updateExpression =
              "set " + User.CATEGORY_RATINGS + ".#categoryId.#version" + " = :map";
          nameMap = new NameMap()
              .with("#categoryId", categoryId)
              .with("#version", categoryVersion.toString());
          valueMap = new ValueMap().withMap(":map", ratingsMapConverted);
        } else {
          //this is the first version being saved -> the entire category to ratings map needs insert
          updateExpression = "set " + User.CATEGORY_RATINGS + ".#categoryId = :map";
          nameMap = new NameMap().with("#categoryId", categoryId);
          valueMap = new ValueMap().withMap(":map", ImmutableMap
              .of(categoryVersion.toString(), ratingsMapConverted));
        }

        if ((isNewCategory || user.getOwnedCategories().containsKey(categoryId))
            && categoryName != null) {
          updateExpression += ", " + User.OWNED_CATEGORIES + ".#categoryId = :categoryName";
          valueMap.withString(":categoryName", categoryName);
        }

        final UpdateItemData updateItemData = new UpdateItemData(activeUser,
            DbAccessManager.USERS_TABLE_NAME)
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap)
            .withNameMap(nameMap);

        if (updateDb) {
          this.dbAccessManager.updateUser(updateItemData);
        }

        resultStatus = new ResultStatus<>(true, updateItemData,
            "User ratings updated successfully!");
      } else {
        this.metrics.logWithBody(new WarningDescriptor<>(classMethod, errorMessage.get()));
        resultStatus = ResultStatus.failure(errorMessage.get());
      }
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private Optional<String> userRatingsIsValid(final Map<String, Object> ratings) {
    String errorMessage = null;

    try {
      for (final String choiceId : ratings.keySet()) {
        final int rating = Integer.parseInt(ratings.get(choiceId).toString());

        if (rating < 0 || rating > 5) {
          errorMessage = "Error: invalid rating value.";
          break;
        }
      }
    } catch (final Exception e) {
      errorMessage = "Error: invalid ratings map.";
    }

    return Optional.ofNullable(errorMessage);
  }
}
