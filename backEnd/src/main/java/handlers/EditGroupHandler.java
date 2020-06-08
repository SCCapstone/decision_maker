package handlers;

import static utilities.Config.MAX_DURATION;
import static utilities.Config.MAX_GROUP_MEMBERS;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.google.common.collect.ImmutableMap;
import exceptions.AttributeValueOutOfRangeException;
import exceptions.InvalidAttributeValueException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import managers.DbAccessManager;
import managers.S3AccessManager;
import managers.SnsAccessManager;
import models.Category;
import models.Group;
import models.GroupCategory;
import models.GroupForApiResponse;
import models.Metadata;
import models.User;
import models.UserGroup;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class EditGroupHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;
  private S3AccessManager s3AccessManager;
  private SnsAccessManager snsAccessManager;
  private Metrics metrics;

  @Inject
  public EditGroupHandler(final DbAccessManager dbAccessManager,
      final S3AccessManager s3AccessManager, final SnsAccessManager snsAccessManager,
      final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.s3AccessManager = s3AccessManager;
    this.snsAccessManager = snsAccessManager;
    this.metrics = metrics;
  }

  /**
   * This methods takes in all of the editable group attributes. If the inputs are valid and the
   * requesting user has permissions, then the group gets updated and the necessary data for
   * denormalization is sent to the groups table and the categories table respectively.
   *
   * @param activeUser     The user making the edit group request.
   * @param groupId        The id of the group attempting to be edited.
   * @param name           The updated name of the group.
   * @param membersList    The updated list of usernames associated with the group.
   * @param categoriesList The updated list of category ids associated with the group.
   * @param isOpen         The update is open value for this group.
   * @param iconData       The byte array for a new group icon. If null, the icon is not updated.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String groupId, final String name,
      final List<String> membersList, final List<String> categoriesList, final Boolean isOpen,
      final List<Integer> iconData) {
    final String classMethod = "EditGroupHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      final Group oldGroup = this.dbAccessManager.getGroup(groupId);

      final Optional<String> errorMessage = this
          .editGroupInputIsValid(oldGroup, activeUser, membersList);
      if (!errorMessage.isPresent()) {
        //all validation is successful, build transaction actions
        membersList.add(activeUser); // sanity check, active user is in members list
        final Map<String, Object> membersMapped = this.getMembersMapForInsertion(membersList);
        final Map<String, Object> categoriesMapped = this
            .getCategoriesMapForInsertion(categoriesList);

        String updateExpression =
            "set " + Group.GROUP_NAME + " = :name, " + Group.MEMBERS + " = :members, "
                + Group.CATEGORIES + " = :categories, " + Group.IS_OPEN + " = :isOpen";
        ValueMap valueMap = new ValueMap()
            .withString(":name", name)
            .withMap(":members", membersMapped)
            .withMap(":categories", categoriesMapped)
            .withBoolean(":isOpen", isOpen);

        //assumption - currently we aren't allowing user's to clear a group's image once set
        String newIconFileName = null;
        if (iconData != null) {
          newIconFileName = this.s3AccessManager.uploadImage(iconData, this.metrics)
              .orElseThrow(Exception::new);

          updateExpression += ", " + Group.ICON + " = :icon";
          valueMap.withString(":icon", newIconFileName);
        }

        UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withUpdateExpression(updateExpression)
            .withValueMap(valueMap);

        this.dbAccessManager.updateGroup(groupId, updateItemSpec);

        //clone the old group and update all of the fields since we successfully updated the db
        final Group newGroup = oldGroup.clone();
        newGroup.setGroupName(name);
        newGroup.setMembers(membersMapped);
        newGroup.setCategoriesRawMap(categoriesMapped);
        newGroup.setOpen(isOpen);
        if (newIconFileName != null) {
          newGroup.setIcon(newIconFileName);
        }

        //update mappings in users and categories tables
        this.updateUsersTable(oldGroup, newGroup);
        this.updateCategoriesTable(oldGroup, newGroup);

        final User user = this.dbAccessManager.getUser(activeUser);

        resultStatus = new ResultStatus(true,
            JsonUtils.convertObjectToJson(new GroupForApiResponse(user, newGroup).asMap()));
      } else {
        resultStatus = ResultStatus.failure(errorMessage.get());
        this.metrics.logWithBody(new WarningDescriptor<>(classMethod, errorMessage.get()));
      }
    } catch (Exception e) {
      resultStatus = ResultStatus.failure("Exception in: " + classMethod);
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This method updates user items based on the changed definition of a group
   *
   * @param oldGroup The old group definition before the update. If this param is null, then that
   *                 signals that a new group is being created.
   * @param newGroup The new group definition after the update.
   */
  private void updateUsersTable(final Group oldGroup, final Group newGroup) {
    final String classMethod = "EditGroupHandler.updateUsersTable";
    metrics.commonSetup(classMethod);
    boolean success = true;

    final NameMap nameMap = new NameMap().with("#groupId", newGroup.getGroupId());

    final Set<String> oldGroupMembers = oldGroup.getMembers().keySet();
    final Set<String> newGroupMembers = newGroup.getMembers().keySet();

    //set up added, persisting, and removed user sets
    final Set<String> persistingUsernames = new HashSet<>(newGroupMembers);
    persistingUsernames.retainAll(oldGroupMembers); // keep the old usernames

    final Set<String> addedUsernames = new HashSet<>(newGroupMembers);
    addedUsernames.removeAll(oldGroupMembers); // remove old from new to get added

    final Set<String> removedUsernames = new HashSet<>(oldGroupMembers);
    removedUsernames.removeAll(newGroupMembers); // remove new from old to get removed

    String updateExpression;
    ValueMap valueMap;
    UpdateItemSpec updateItemSpec;

    //since this group already exists, we're just updating the mappings that have changed for existing users
    //for simplicity in the code, we'll always update the group name
    updateExpression =
        "set " + User.GROUPS + ".#groupId." + Group.GROUP_NAME + " = :groupName";
    valueMap = new ValueMap().withString(":groupName", newGroup.getGroupName());

    if (newGroup.iconIsSet() && !newGroup.getIcon().equals(oldGroup.getIcon())) {
      updateExpression += ", " + User.GROUPS + ".#groupId." + Group.ICON + " = :groupIcon";
      valueMap.withString(":groupIcon", newGroup.getIcon());
    }

    updateItemSpec = new UpdateItemSpec()
        .withUpdateExpression(updateExpression)
        .withValueMap(valueMap)
        .withNameMap(nameMap);

    for (final String oldMember : persistingUsernames) {
      try {
        this.dbAccessManager.updateUser(oldMember, updateItemSpec);
      } catch (final Exception e) {
        success = false;
        metrics.log(new ErrorDescriptor<>(oldMember, classMethod, e));
      }
    }

    //for new users, we need to add the entire group map
    updateExpression = "set " + User.GROUPS + ".#groupId = :userGroupMap";
    valueMap = new ValueMap().withMap(":userGroupMap", UserGroup.fromNewGroup(newGroup).asMap());

    updateItemSpec = new UpdateItemSpec()
        .withUpdateExpression(updateExpression)
        .withValueMap(valueMap)
        .withNameMap(nameMap);

    for (final String newMember : addedUsernames) {
      try {
        this.dbAccessManager.updateUser(newMember, updateItemSpec);
      } catch (Exception e) {
        success = false;
        metrics.log(new ErrorDescriptor<>(newMember, classMethod, e));
      }
    }

    //update user objects of all of the users removed - if oldGroup is null, nothing to remove from
    this.removeGroupFromUsersAndSendNotifications(removedUsernames, oldGroup);
    this.sendAddedToGroupNotifications(addedUsernames, newGroup);

    metrics.commonClose(success);
  }

  private void sendAddedToGroupNotifications(final Set<String> addedUsernames,
      final Group newGroup) {
    final String classMethod = "EditGroupHandler.sendAddedToGroupNotifications";
    metrics.commonSetup(classMethod);

    boolean success = true;

    final Map<String, Object> payload = UserGroup.fromNewGroup(newGroup).asMap();
    payload.putIfAbsent(Group.GROUP_ID, newGroup.getGroupId());

    final Metadata metadata = new Metadata("addedToGroup", payload);

    for (String username : addedUsernames) {
      try {
        final User user = this.dbAccessManager.getUser(username);

        //don't send a notification to the creator as they know they just created the group
        if (!username.equals(newGroup.getGroupCreator())) {
          if (user.pushEndpointArnIsSet()) {
            //Note: no need to check user's group muted settings since they're just being added
            if (user.getAppSettings().isMuted()) {
              this.snsAccessManager.sendMutedMessage(user.getPushEndpointArn(), metadata);
            } else {
              this.snsAccessManager.sendMessage(user.getPushEndpointArn(),
                  "Added to new group!", "'" + newGroup.getGroupName() + "'", newGroup.getGroupId(),
                  metadata);
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
   * This method removes a given group from each user that is currently linked to the group. This
   * includes both the active members and the members that have left the group.
   *
   * @param removedUsernames The set of usernames that are being removed from the group.
   * @param group            The group object that the users are being removed from.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  private ResultStatus removeGroupFromUsersAndSendNotifications(final Set<String> removedUsernames,
      final Group group) {
    final String classMethod = "EditGroupHandler.removeGroupFromUsersAndSendNotifications";
    this.metrics.commonSetup(classMethod);

    //assume true and set to false on any failures
    ResultStatus resultStatus = new ResultStatus(true,
        "Members removed and notified successfully.");

    final Metadata metadata = new Metadata("removedFromGroup",
        ImmutableMap.of(Group.GROUP_ID, group.getGroupId()));

    //remove the group from all of the active members user items
    UpdateItemSpec updateItemSpec = new UpdateItemSpec()
        .withUpdateExpression("remove " + User.GROUPS + ".#groupId")
        .withNameMap(new NameMap().with("#groupId", group.getGroupId()));

    for (final String username : removedUsernames) {
      try {
        //pull the user before deleting so we have their group isMuted mapping for the push notice
        final User removedUser = this.dbAccessManager.getUser(username);

        //actually do the removal of the group map
        this.dbAccessManager.updateUser(username, updateItemSpec);

        //if the delete went through, send the notification
        if (removedUser.pushEndpointArnIsSet()) {
          if (removedUser.getAppSettings().isMuted() || removedUser.getGroups()
              .get(group.getGroupId()).isMuted()) {
            this.snsAccessManager.sendMutedMessage(removedUser.getPushEndpointArn(), metadata);
          } else {
            this.snsAccessManager
                .sendMessage(removedUser.getPushEndpointArn(), "Removed from group",
                    "'" + group.getGroupName() + "'", group.getGroupId(), metadata);
          }
        }
      } catch (Exception e) {
        resultStatus = new ResultStatus(false, "Exception removing group from user");
        this.metrics.log(new ErrorDescriptor<>(username, classMethod, e));
      }
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  /**
   * This method takes in the group object before and after being updated. The appropriate
   * denormalized data gets added to, updated in, or removed from the categories table.
   *
   * @param oldGroup The group definition before changes were made.
   * @param newGroup The group definition after changes have been made.
   */
  private void updateCategoriesTable(final Group oldGroup, final Group newGroup) {
    final String classMethod = "EditGroupHandler.updateCategoriesTable";
    this.metrics.commonSetup(classMethod);

    boolean success = true;

    final Set<String> categoriesToUpdate = new HashSet<>();

    UpdateItemSpec updateItemSpec;
    final NameMap nameMap = new NameMap().with("#groupId", newGroup.getGroupId());
    final ValueMap valueMap = new ValueMap().withString(":groupName", newGroup.getGroupName());

    //If the group name was changed all group categories need updating to get the name
    if (!newGroup.getGroupName().equals(oldGroup.getGroupName())) {
      categoriesToUpdate.addAll(newGroup.getCategories().keySet());
    }

    //if the categories aren't the same, something needs to be removed/added
    if (!oldGroup.getCategories().keySet().equals(newGroup.getCategories().keySet())) {
      // Make copies of the key sets and use removeAll to figure out where they differ
      final Set<String> newCategoryIds = new HashSet<>(newGroup.getCategories().keySet());
      final Set<String> removedCategoryIds = new HashSet<>(oldGroup.getCategories().keySet());

      newCategoryIds.removeAll(oldGroup.getCategories().keySet()); // remove old to see added
      removedCategoryIds.removeAll(newGroup.getCategories().keySet()); // remove new to see removed

      //add these newly added categories for update
      categoriesToUpdate.addAll(newCategoryIds);

      //actually do the group removal from the categories
      updateItemSpec = new UpdateItemSpec()
          .withUpdateExpression("remove " + Category.GROUPS + ".#groupId")
          .withNameMap(nameMap);
      for (final String categoryId : removedCategoryIds) {
        try {
          this.dbAccessManager.updateCategory(categoryId, updateItemSpec);
        } catch (final Exception e) {
          success = false;
          this.metrics.log(new ErrorDescriptor<>(categoryId, classMethod, e));
        }
      }
    }

    //actually do the update to the categories to add the group association
    updateItemSpec = new UpdateItemSpec()
        .withUpdateExpression("set " + Category.GROUPS + ".#groupId = :groupName")
        .withNameMap(nameMap)
        .withValueMap(valueMap);
    for (final String categoryId : categoriesToUpdate) {
      try {
        this.dbAccessManager.updateCategory(categoryId, updateItemSpec);
      } catch (final Exception e) {
        success = false;
        this.metrics.log(new ErrorDescriptor<>(categoryId, classMethod, e));
      }
    }

    this.metrics.commonClose(success);
  }

  /**
   * This function takes the old definition of a group and checks to see if proposed edits are
   * valid.
   *
   * @param oldGroup   this is the old group definition that is attempting to be edited
   * @param activeUser the user doing the edit
   * @param members    the new list of members for the group
   * @return A nullable errorMessage. If null, then there was no error and it is valid
   */
  private Optional<String> editGroupInputIsValid(final Group oldGroup, final String activeUser,
      final List<String> members) {

    String errorMessage = null;

    if (oldGroup.isOpen()) {
      if (!oldGroup.getMembers().containsKey(activeUser)) {
        errorMessage = this.getUpdatedErrorMessage(errorMessage, "Error: Bad permissions.");
      }
    } else {
      if (!oldGroup.getGroupCreator().equals(activeUser)) {
        errorMessage = this.getUpdatedErrorMessage(errorMessage, "Error: Bad permissions.");
      }
    }

    if (!members.contains(oldGroup.getGroupCreator())) {
      errorMessage = this.getUpdatedErrorMessage(errorMessage,
          "Error: Group creator cannot be removed from group.");
    }

    //NOTE this could potentially be a bad error since not all usernames are guaranteed to exist.
    // That being said, it should be assumed all names are valid from front end validation. This
    // also saves potentially unnecessary db hits.
    if (new HashSet<>(members).size() > MAX_GROUP_MEMBERS) {
      errorMessage = this.getUpdatedErrorMessage(errorMessage, "Error: Too many members.");
    }

    //make a copy so we don't actually update the member map on the old group
    final Set<String> membersLeftCopy = new HashSet<>(oldGroup.getMembersLeft().keySet());
    membersLeftCopy.retainAll(members); // calculates the intersection of members left and members
    if (membersLeftCopy.size() > 0) {
      errorMessage = this.getUpdatedErrorMessage(errorMessage,
          "Error: Error: Cannot add a user that left.");
    }

    return Optional.ofNullable(errorMessage);
  }

  private String getUpdatedErrorMessage(final String current, final String update) {
    String invalidString;
    if (current == null) {
      invalidString = update;
    } else {
      invalidString = current + "\n" + update;
    }

    return invalidString;
  }

  private Map<String, Object> getMembersMapForInsertion(final List<String> members)
      throws InvalidAttributeValueException, AttributeValueOutOfRangeException {
    final Map<String, Object> membersMap = new HashMap<>();

    for (final String username : new HashSet<>(members)) {
      final User user = this.dbAccessManager.getUser(username);
      membersMap.putIfAbsent(username, user.asMember().asMap());
    }

    return membersMap;
  }

  private Map<String, Object> getCategoriesMapForInsertion(final List<String> categoryIds) {
    final Map<String, Object> categoriesMap = new HashMap<>();

    for (final String categoryId : new HashSet<>(categoryIds)) {
      final Category category = this.dbAccessManager.getCategory(categoryId);
      categoriesMap.putIfAbsent(categoryId, new GroupCategory(category.asMap()).asMap());
    }

    return categoriesMap;
  }
}
