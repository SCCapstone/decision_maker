package handlers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import exceptions.InvalidAttributeValueException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import managers.DbAccessManager;
import managers.S3AccessManager;
import managers.SnsAccessManager;
import models.Group;
import models.GroupForApiResponse;
import models.Metadata;
import models.User;
import models.UserGroup;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.ResultStatus;

public class CreateNewGroupHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;
  private S3AccessManager s3AccessManager;
  private SnsAccessManager snsAccessManager;
  private Metrics metrics;

  @Inject
  public CreateNewGroupHandler(final DbAccessManager dbAccessManager,
      final S3AccessManager s3AccessManager, final SnsAccessManager snsAccessManager,
      final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.s3AccessManager = s3AccessManager;
    this.snsAccessManager = snsAccessManager;
    this.metrics = metrics;
  }

  /**
   * This method handles creating a new group item within the dynamo db. It handles validating all
   * of the input fields and then de-normalizing the data to the necessary locations in the
   * users/categories tables.
   *
   * @param activeUser            The user making the create new group api request.
   * @param name                  The name of the group.
   * @param membersList           The list of usernames to be associated with the group.
   * @param categoriesMap         The map of category ids to names associated with the group.
   * @param defaultVotingDuration The default voting duration for creating events in this group.
   * @param defaultRsvpDuration   The default consider duration for creating events in this gorup.
   * @param isOpen                Whether or not this group is editable by its members or just its
   *                              creator.
   * @param iconData              The byte data for an icon. If null this implies no icon.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String name,
      final List<String> membersList, final Map<String, Object> categoriesMap,
      final Integer defaultVotingDuration, final Integer defaultRsvpDuration, final Boolean isOpen,
      final List<Integer> iconData) {
    final String classMethod = "CreateNewGroupHandler.handle";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final String newGroupId = UUID.randomUUID().toString();
      final String lastActivity = this.dbAccessManager.now();

      final Group newGroup = new Group();
      newGroup.setGroupName(name);
      newGroup.setCategories(categoriesMap);
      newGroup.setDefaultVotingDuration(defaultVotingDuration);
      newGroup.setDefaultRsvpDuration(defaultRsvpDuration);
      newGroup.setOpen(isOpen);
      newGroup.setGroupId(newGroupId);
      newGroup.setGroupCreator(activeUser);
      newGroup.setMembersLeft(Collections.emptyMap());
      newGroup.setEvents(Collections.emptyMap());
      newGroup.setLastActivity(lastActivity);

      //sanity check, add the active user to this mapping to make sure their data is added
      membersList.add(activeUser);
      newGroup.setMembers(this.getMembersMapForInsertion(membersList));

      //TODO update the categories passed in to be a list of ids, then create categories map
      //TODO similar to what we're doing with the members above (currently we're just relying on
      //TODO user input which is bad

      if (iconData != null) { // if it's there, assume it's new image data
        final String newIconFileName = DatabaseManagers.S3_ACCESS_MANAGER
            .uploadImage(iconData, this.metrics)
            .orElseThrow(Exception::new);

        newGroup.setIcon(newIconFileName);
      }

      this.dbAccessManager.putGroup(newGroup);

      this.updateUsersTable(newGroup);
      this.updateCategoriesTable(newGroup);

      resultStatus = new ResultStatus(true,
          JsonUtils.convertObjectToJson(new GroupForApiResponse(newGroup).asMap()));
    } catch (Exception e) {
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  private Map<String, Object> getMembersMapForInsertion(final List<String> members)
      throws InvalidAttributeValueException {
    final Map<String, Object> membersMap = new HashMap<>();

    for (String username : new HashSet<>(members)) {
      final User user = new User(DatabaseManagers.USERS_MANAGER.getMapByPrimaryKey(username));
      membersMap.putIfAbsent(username, user.asMember().asMap());
    }

    return membersMap;
  }

  /**
   * This method updates user items based on the creation of a new group.
   *
   * @param newGroup The group object that is being created.
   */
  private void updateUsersTable(final Group newGroup) {
    final String classMethod = "CreateNewGroupHandler.updateUsersTable";
    this.metrics.commonSetup(classMethod);

    boolean success = true; // assume true, set to false on any failures

    //if this is a new Group we need to create the entire map
    final String updateExpression = "set " + UsersManager.GROUPS + ".#groupId = :userGroupMap";
    final ValueMap valueMap = new ValueMap()
        .withMap(":userGroupMap", UserGroup.fromNewGroup(newGroup).asMap());
    final NameMap nameMap = new NameMap().with("#groupId", newGroup.getGroupId());

    final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
        .withUpdateExpression(updateExpression)
        .withValueMap(valueMap)
        .withNameMap(nameMap);

    for (final String newMember : newGroup.getMembers().keySet()) {
      try {
        this.dbAccessManager.updateUser(newMember, updateItemSpec);
      } catch (Exception e) {
        success = false;
        this.metrics.log(new ErrorDescriptor<>(newMember, classMethod, e));
      }
    }

    //blind send...
    this.sendAddedToGroupNotifications(newGroup);

    this.metrics.commonClose(success);
  }

  private void sendAddedToGroupNotifications(final Group newGroup) {
    final String classMethod = "CreateNewGroupHandler.sendAddedToGroupNotifications";
    metrics.commonSetup(classMethod);

    boolean success = true;

    final Map<String, Object> payload = UserGroup.fromNewGroup(newGroup).asMap();
    payload.putIfAbsent(Group.GROUP_ID, newGroup.getGroupId());

    final Metadata metadata = new Metadata("addedToGroup", payload);

    for (String username : newGroup.getMembers().keySet()) {
      try {
        final User user = new User(DatabaseManagers.USERS_MANAGER.getMapByPrimaryKey(username));

        //don't send a notification to the creator as they know they just created the group
        if (!username.equals(newGroup.getGroupCreator())) {
          if (user.pushEndpointArnIsSet()) {
            //Note: no need to check user's group muted settings since they're just being added
            if (user.getAppSettings().isMuted()) {
              this.snsAccessManager.sendMutedMessage(user.getPushEndpointArn(), metadata);
            } else {
              this.snsAccessManager.sendMessage(user.getPushEndpointArn(),
                  "Added to new group!", newGroup.getGroupName(), newGroup.getGroupId(), metadata);
            }
          }
        }
      } catch (Exception e) {
        success = false;
        metrics.log(new ErrorDescriptor<>(username, classMethod, e));
      }
    }

    metrics.commonClose(success);
  }

  /**
   * This method takes in the group object after being created. The appropriate denormalized data
   * gets updated in the categories table.
   *
   * @param newGroup The group definition after a change has been made.
   */
  private void updateCategoriesTable(final Group newGroup) {
    final String classMethod = "CreateNewGroupHandler.updateCategoriesTable";
    this.metrics.commonSetup(classMethod);

    boolean success = true;

    final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
        .withUpdateExpression("set Groups.#groupId = :groupName")
        .withNameMap(new NameMap().with("#groupId", newGroup.getGroupId()))
        .withValueMap(new ValueMap().withString(":groupName", newGroup.getGroupName()));

    if (newGroup.getCategories().keySet().size() > 0) {
      for (final String categoryId : newGroup.getCategories().keySet()) {
        try {
          this.dbAccessManager.updateCategory(categoryId, updateItemSpec);
        } catch (final Exception e) {
          success = false;
          this.metrics.log(new ErrorDescriptor<>(categoryId, classMethod, e));
        }
      }
    }

    this.metrics.commonClose(success);
  }
}
