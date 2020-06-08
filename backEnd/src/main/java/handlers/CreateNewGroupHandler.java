package handlers;

import static utilities.Config.MAX_GROUP_MEMBERS;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import exceptions.AttributeValueOutOfRangeException;
import exceptions.InvalidAttributeValueException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import utilities.AttributeValueUtils;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.ResultStatus;
import utilities.UpdateItemData;
import utilities.WarningDescriptor;

public class CreateNewGroupHandler implements ApiRequestHandler {

  public static final Integer MAX_OWNED_GROUPS = 100;

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
   * @param activeUser     The user making the create new group api request.
   * @param name           The name of the group.
   * @param membersList    The list of usernames to be associated with the group.
   * @param categoriesList The list of category ids to be associated with the group.
   * @param isOpen         Whether or not this group is editable by its members or just its
   *                       creator.
   * @param iconData       The byte data for an icon. If null this implies no icon.
   * @return Standard result status object giving insight on whether the request was successful.
   */
  public ResultStatus handle(final String activeUser, final String name,
      final List<String> membersList, final List<String> categoriesList, final Boolean isOpen,
      final List<Integer> iconData) {
    final String classMethod = "CreateNewGroupHandler.handle";
    this.metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      User user = this.dbAccessManager.getUser(activeUser);

      final Optional<String> errorMessage = this.newGroupInputIsValid(user, membersList);
      if (!errorMessage.isPresent()) {
        //build the new group for insertion
        final String newGroupId = UUID.randomUUID().toString();
        final String lastActivity = this.dbAccessManager.now();

        final Group newGroup = new Group();
        newGroup.setGroupName(name);
        newGroup.setOpen(isOpen);
        newGroup.setGroupId(newGroupId);
        newGroup.setGroupCreator(activeUser);
        newGroup.setMembersLeft(Collections.emptyMap());
        newGroup.setEvents(Collections.emptyMap());
        newGroup.setLastActivity(lastActivity);

        //sanity check, add the active user to this mapping to make sure their data is added
        membersList.add(activeUser);
        newGroup.setMembers(this.getMembersMapForInsertion(membersList));

        newGroup.setCategoriesRawMap(this.getCategoriesMapForInsertion(categoriesList));

        if (iconData != null) { // if it's there, assume it's new image data
          final String newIconFileName = this.s3AccessManager.uploadImage(iconData, this.metrics)
              .orElseThrow(Exception::new);

          newGroup.setIcon(newIconFileName);
        }

        //build the owned group statement for the active user
        final UpdateItemData updateItemData = new UpdateItemData(activeUser,
            DbAccessManager.USERS_TABLE_NAME)
            .withUpdateExpression(
                "set " + User.OWNED_GROUPS_COUNT + " = " + User.OWNED_GROUPS_COUNT + " + :val")
            .withValueMap(new ValueMap().withNumber(":val", 1));

        final List<TransactWriteItem> actions = new ArrayList<>();

        actions.add(new TransactWriteItem().withUpdate(updateItemData.asUpdate()));
        actions.add(new TransactWriteItem()
            .withPut(new Put().withTableName(DbAccessManager.GROUPS_TABLE_NAME).withItem(
                AttributeValueUtils.convertMapToAttributeValueMap(newGroup.asMap()))));

        this.dbAccessManager.executeWriteTransaction(actions);

        this.updateUsersTable(newGroup);
        this.updateCategoriesTable(newGroup);

        //get the user with updated properties
        user = this.dbAccessManager.getUserNoCache(activeUser);

        resultStatus = new ResultStatus(true,
            JsonUtils.convertObjectToJson(new GroupForApiResponse(user, newGroup).asMap()));
      } else {
        resultStatus = ResultStatus.failure(errorMessage.get());
        this.metrics.logWithBody(new WarningDescriptor<>(classMethod, errorMessage.get()));
      }
    } catch (Exception e) {
      resultStatus = ResultStatus.failure("Exception in " + classMethod);
      this.metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
    }

    this.metrics.commonClose(resultStatus.success);
    return resultStatus;
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

  /**
   * This function takes the perameters for creating a group and checks if they are valid.
   *
   * @param user        The active user making the api request.
   * @param membersList A list of the usernames to associate with this group.
   * @return A nullable errorMessage. If null, then there was no error and it is valid
   */
  private Optional<String> newGroupInputIsValid(final User user, final List<String> membersList) {

    String errorMessage = null;

    //NOTE this could potentially be a bad error since not all usernames are guaranteed to exist.
    // That being said, it should be assumed all names are valid from front end validation. This
    // also saves potentially unnecessary db hits.
    if (new HashSet<>(membersList).size() > MAX_GROUP_MEMBERS) {
      errorMessage = this.getUpdatedErrorMessage(errorMessage, "Error: Too many members.");
    }

    if (user.getOwnedGroupsCount() >= MAX_OWNED_GROUPS) {
      errorMessage = this.getUpdatedErrorMessage(errorMessage,
          "Error: User cannot own more than " + MAX_OWNED_GROUPS + " at one time.");
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
    final String updateExpression = "set " + User.GROUPS + ".#groupId = :userGroupMap";
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
    this.metrics.commonSetup(classMethod);

    boolean success = true;

    final Map<String, Object> payload = UserGroup.fromNewGroup(newGroup).asMap();
    payload.putIfAbsent(Group.GROUP_ID, newGroup.getGroupId());

    final Metadata metadata = new Metadata("addedToGroup", payload);

    for (String username : newGroup.getMembers().keySet()) {
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
                  "Added to new group!", "'" + newGroup.getGroupName() + "'",
                  newGroup.getGroupId(), metadata);
            }
          }
        }
      } catch (Exception e) {
        success = false;
        this.metrics.log(new ErrorDescriptor<>(username, classMethod, e));
      }
    }

    this.metrics.commonClose(success);
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
