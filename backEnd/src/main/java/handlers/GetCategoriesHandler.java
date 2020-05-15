package handlers;

import exceptions.InvalidAttributeValueException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import managers.DbAccessManager;
import models.Category;
import models.CategoryRatingTuple;
import models.EventWithCategoryChoices;
import models.Group;
import models.GroupWithCategoryChoices;
import models.User;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class GetCategoriesHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;
  private Metrics metrics;

  @Inject
  public GetCategoriesHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  public ResultStatus handle(final String activeUser, final List<String> categoryIds) {
    final String classMethod = "GetCategoriesHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final List<Map<String, Object>> categoryRatingTuples = this
          .getCategoryRatingTuples(new HashSet<>(categoryIds), activeUser);
      resultStatus = ResultStatus.successful(JsonUtils.convertObjectToJson(categoryRatingTuples));
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    return resultStatus;
  }

  public ResultStatus handle(final String activeUser) {
    final String classMethod = "GetCategoriesHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final User user = this.dbAccessManager.getUser(activeUser);
      final List<Map<String, Object>> categoryRatingTuples = this
          .getCategoryRatingTuples(user.getOwnedCategories().keySet(), activeUser);
      resultStatus = ResultStatus.successful(JsonUtils.convertObjectToJson(categoryRatingTuples));
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    return resultStatus;
  }

  public ResultStatus handle(final String activeUser, final String groupId) {
    final String classMethod = "GetCategoriesHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Group group = this.dbAccessManager.getGroup(groupId);

      if (group.getMembers().containsKey(activeUser)) {
        final List<Map<String, Object>> categoryRatingTuples = this
            .getCategoryRatingTuples(group.getCategories().keySet(), activeUser);
        resultStatus = ResultStatus.successful(JsonUtils.convertObjectToJson(categoryRatingTuples));
      } else {
        this.metrics.logWithBody(new WarningDescriptor<Map>(classMethod, "User not in group"));
        resultStatus = ResultStatus.failure("Error: User not in group");
      }
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    return resultStatus;
  }

  public ResultStatus handle(final String activeUser, final String groupId, final String eventId) {
    final String classMethod = "GetCategoriesHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final GroupWithCategoryChoices group = new GroupWithCategoryChoices(
          this.dbAccessManager.getGroupItem(groupId).asMap());

      if (group.getMembers().containsKey(activeUser)) {
        final User user = this.dbAccessManager.getUser(activeUser);
        final EventWithCategoryChoices event = group.getEventsWithCategoryChoices().get(eventId);

        if (event.getCategoryChoices() != null) {
          final Category category = new Category(event.asMap());

          final CategoryRatingTuple categoryRatingTuple = new CategoryRatingTuple(category,
              user.getCategoryRatings().get(category.getCategoryId())
                  .getRatings(category.getVersion()));

          resultStatus = ResultStatus.successful(
              JsonUtils.convertObjectToJson(Collections.singletonList(categoryRatingTuple)));
        } else {
          this.metrics.logWithBody(new WarningDescriptor<>(classMethod, "No choices on event."));
          resultStatus = ResultStatus.failure("Error: No choices on event.");
        }
      } else {
        this.metrics.logWithBody(new WarningDescriptor<>(classMethod, "User not in group"));
        resultStatus = ResultStatus.failure("Error: User not in group");
      }
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    return resultStatus;
  }

  private List<Map<String, Object>> getCategoryRatingTuples(final Set<String> categoryIds,
      final String activeUser) throws InvalidAttributeValueException {
    final String classMethod = "GetCategoriesHandler.getCategories";

    final User user = this.dbAccessManager.getUser(activeUser);

    final List<Map<String, Object>> categoryRatingTuples = new ArrayList<>();
    for (String id : categoryIds) {
      try {
        final Category category = this.dbAccessManager.getCategory(id);
        final CategoryRatingTuple categoryRatingTuple = new CategoryRatingTuple(category,
            user.getCategoryRatings().get(id).getRatings(category.getVersion()));
        categoryRatingTuples.add(categoryRatingTuple.asMap());
      } catch (Exception e) {
        this.metrics.log(new ErrorDescriptor<>(id, classMethod, e));
      }
    }

    return categoryRatingTuples;
  }
}
