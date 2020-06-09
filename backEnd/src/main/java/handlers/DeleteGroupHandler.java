package handlers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.Delete;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import managers.DbAccessManager;
import managers.S3AccessManager;
import managers.SnsAccessManager;
import models.Category;
import models.EventForSorting;
import models.Group;
import models.Metadata;
import models.User;
import utilities.AttributeValueUtils;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.UpdateItemData;

/**
 * This method handles deleting a group item from the db. In addition, it handles removing all of
 * the denormalized data from the users/categories tables.
 */
public class DeleteGroupHandler implements ApiRequestHandler {

  private final DbAccessManager dbAccessManager;
  private final S3AccessManager s3AccessManager;
  private final SnsAccessManager snsAccessManager;
  private final Metrics metrics;

  @Inject
  public DeleteGroupHandler(final DbAccessManager dbAccessManager,
      final S3AccessManager s3AccessManager, final SnsAccessManager snsAccessManager,
      final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.s3AccessManager = s3AccessManager;
    this.snsAccessManager = snsAccessManager;
    this.metrics = metrics;
  }

  public ResultStatus handle(final String activeUser, final String groupId) {
    final String classMethod = "DeleteGroupHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Group group = this.dbAccessManager.getGroup(groupId);
      if (activeUser.equals(group.getGroupCreator())) {

        // Remove the group from the users and categories tables
        final ResultStatus removeGroupFromUsers = this
            .removeGroupFromUsersAndSendNotifications(group);
        final ResultStatus removeFromCategoriesResult = this.removeGroupFromCategories(group);

        if (removeGroupFromUsers.success && removeFromCategoriesResult.success) {
          //build the owned group statement for the active user
          final UpdateItemData updateItemData = new UpdateItemData(activeUser,
              DbAccessManager.USERS_TABLE_NAME)
              .withUpdateExpression(
                  "set " + User.OWNED_GROUPS_COUNT + " = " + User.OWNED_GROUPS_COUNT + " - :val")
              .withValueMap(new ValueMap().withNumber(":val", 1));

          final List<TransactWriteItem> actions = new ArrayList<>();

          actions.add(new TransactWriteItem().withUpdate(updateItemData.asUpdate()));
          actions.add(new TransactWriteItem()
              .withDelete(
                  new UpdateItemData(groupId, DbAccessManager.GROUPS_TABLE_NAME).asDelete()));

          this.dbAccessManager.executeWriteTransaction(actions);

          resultStatus = ResultStatus.successful("Group deleted successfully!");

          //blind attempt to delete the group's icon and pending events
          //if either fail, we'll get a notification and we can manually delete if necessary
          this.s3AccessManager.deleteImage(group.getIcon(), metrics);

          this.deleteAllPendingGroupEvents(group);
        } else {
          resultStatus = removeGroupFromUsers.applyResultStatus(removeFromCategoriesResult);
          this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, resultStatus.resultMessage));
        }
      } else {
        this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, "User is not owner of group"));
        resultStatus = ResultStatus.failure("Error: User is not owner of group.");
      }
    } catch (final Exception e) {
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This event handles removing all of a groups pending events from any of the pending event table
   * partitions
   *
   * @param deletedGroup This is the group object that is being deleted and needs all of its pending
   *                     events deleted.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  private ResultStatus deleteAllPendingGroupEvents(final Group deletedGroup) {
    final Set<String> pendingEventIds = deletedGroup.getEvents().entrySet().stream()
        .filter((e) -> (new EventForSorting(e.getKey(), e.getValue()).isPending()))
        .map(Entry::getKey).collect(Collectors.toSet());

    if (pendingEventIds.isEmpty()) {
      return new ResultStatus(true, "No events to delete");
    }

    final String classMethod = "DeleteGroupHandler.deleteAllPendingGroupEvents";
    this.metrics.commonSetup(classMethod);

    //assume success, we'll set to fail if anything goes wrong
    ResultStatus resultStatus = ResultStatus.successful("Pending events deleted successfully");

    try {
      final List<String> eventIdsList = new ArrayList<>(pendingEventIds);
      final String updateExpression =
          "remove " + IntStream.range(0, eventIdsList.size()).boxed().map(i -> "#groupEventKey" + i)
              .collect(Collectors.joining(", "));

      final NameMap nameMap = new NameMap();
      IntStream.range(0, eventIdsList.size()).boxed()
          .forEach(i -> nameMap.with("#groupEventKey" + i,
              deletedGroup.getGroupId() + DbAccessManager.DELIM + eventIdsList.get(i)));

      final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression(updateExpression)
          .withNameMap(nameMap);

      final int numberOfPartitions = Integer
          .parseInt(System.getenv(DbAccessManager.NUMBER_OF_PARTITIONS_ENV_KEY));
      for (int i = 1; i <= numberOfPartitions; i++) {
        try {
          this.dbAccessManager.updatePendingEvent(Integer.valueOf(i).toString(), updateItemSpec);
        } catch (final Exception e) {
          this.metrics.log(new ErrorDescriptor<>(i, classMethod, e));
          resultStatus = ResultStatus.failure("Exception in " + classMethod);
        }
      }
    } catch (final Exception e) {
      this.metrics.log(
          new ErrorDescriptor<>(String
              .format("GroupId: %s, EventIds: %s", deletedGroup.getGroupId(), pendingEventIds),
              classMethod, e));
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This method removes a given group from each category that is currently linked to the group.
   *
   * @param removedFrom The group object that is being deleted. The linked categories need this
   *                    group removed from their group maps.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  private ResultStatus removeGroupFromCategories(final Group removedFrom) {
    final String classMethod = "DeleteGroupHandler.removeGroupFromCategories";
    this.metrics.commonSetup(classMethod);

    //assume true and set to false if anything fails
    ResultStatus resultStatus = new ResultStatus(true,
        "Group successfully removed from categories table.");

    final String updateExpression = "remove " + Category.GROUPS + ".#groupId";
    final NameMap nameMap = new NameMap().with("#groupId", removedFrom.getGroupId());

    final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
        .withUpdateExpression(updateExpression)
        .withNameMap(nameMap);

    for (String categoryId : removedFrom.getCategories().keySet()) {
      try {
        this.dbAccessManager.updateCategory(categoryId, updateItemSpec);
      } catch (final Exception e) {
        this.metrics.log(new ErrorDescriptor<>(categoryId, classMethod, e));
        resultStatus.resultMessage = "Error: group failed to be removed from category.";
      }
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This method removes a given group from each user that is currently linked to the group. This
   * includes both the active members and the members that have left the group.
   *
   * @param deletedGroup The group object that is being deleted. The linked categories need this
   *                     group removed from their group maps.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  private ResultStatus removeGroupFromUsersAndSendNotifications(final Group deletedGroup) {
    final String classMethod = "DeleteGroupHandler.removeGroupFromUsersAndSendNotifications";
    this.metrics.commonSetup(classMethod);

    //assume true and set to false on any failures
    ResultStatus resultStatus = new ResultStatus(true,
        "Active members removed and notified successfully.");

    final Metadata metadata = new Metadata("removedFromGroup",
        ImmutableMap.of(Group.GROUP_ID, deletedGroup.getGroupId()));

    //remove the group from all of the active members user items
    String updateExpression = "remove " + User.GROUPS + ".#groupId";
    NameMap nameMap = new NameMap().with("#groupId", deletedGroup.getGroupId());
    UpdateItemSpec updateItemSpec = new UpdateItemSpec()
        .withUpdateExpression(updateExpression)
        .withNameMap(nameMap);

    for (final String username : deletedGroup.getMembers().keySet()) {
      try {
        //pull the user before deleting so we have their group isMuted mapping for the push notice
        final User removedUser = this.dbAccessManager.getUser(username);

        //actually do the removal of the group map
        this.dbAccessManager.updateUser(username, updateItemSpec);

        //if the delete went through, send the notification
        if (removedUser.pushEndpointArnIsSet() && !deletedGroup.getGroupCreator()
            .equals(username)) {
          if (removedUser.getAppSettings().isMuted() || removedUser.getGroups()
              .get(deletedGroup.getGroupId()).isMuted()) {
            this.snsAccessManager.sendMutedMessage(removedUser.getPushEndpointArn(), metadata);
          } else {
            this.snsAccessManager.sendMessage(removedUser.getPushEndpointArn(), "Group Deleted",
                "'" + deletedGroup.getGroupName() + "'", deletedGroup.getGroupId(), metadata);
          }
        }
      } catch (Exception e) {
        resultStatus = new ResultStatus(false, "Exception removing group from user");
        this.metrics.log(new ErrorDescriptor<>(username, classMethod, e));
      }
    }

    //remove the group from all of the members left user items
    updateExpression = "remove " + User.GROUPS_LEFT + ".#groupId";
    nameMap = new NameMap().with("#groupId", deletedGroup.getGroupId());
    updateItemSpec = new UpdateItemSpec()
        .withUpdateExpression(updateExpression)
        .withNameMap(nameMap);

    for (final String username : deletedGroup.getMembersLeft().keySet()) {
      try {
        final User removedUser = this.dbAccessManager.getUser(username);

        //actually do the removal of the group map
        this.dbAccessManager.updateUser(username, updateItemSpec);

        //if the delete went through, send the notification
        this.snsAccessManager.sendMutedMessage(removedUser.getPushEndpointArn(), metadata);
      } catch (final Exception e) {
        resultStatus = new ResultStatus(false, "Exception removing group from left user");
        this.metrics.log(new ErrorDescriptor<>(username, classMethod, e));
      }
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
