package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.CreateNewGroupHandler;
import handlers.DatabaseManagers;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import models.Group;
import models.GroupForApiResponse;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class CreateNewGroupController implements ApiRequestController {

  @Inject
  public CreateNewGroupHandler createNewGroupHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "GroupsManager.createNewGroup";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();
    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, GROUP_NAME, MEMBERS, CATEGORIES,
            DEFAULT_VOTING_DURATION, DEFAULT_RSVP_DURATION, IS_OPEN);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);

        if (jsonMap.containsKey(ICON)) { // if it's there, assume it's new image data
          final String newIconFileName = DatabaseManagers.S3_ACCESS_MANAGER
              .uploadImage((List<Integer>) jsonMap.get(ICON), metrics)
              .orElseThrow(Exception::new);

          jsonMap.put(ICON, newIconFileName); // put overwrites current value (byte array->filename)
        }

        final List<String> members = (List<String>) jsonMap.get(MEMBERS);
        //sanity check, add the active user to this mapping to make sure his data is added
        members.add(activeUser);

        final Map<String, Object> membersMapped = this.getMembersMapForInsertion(members);
        jsonMap.put(MEMBERS, membersMapped); // put overwrites current value

        //TODO update the categories passed in to be a list of ids, then create categories map
        //TODO similar to what we're doing with the members above (currently we're just relying on
        //TODO user input which is bad

        final String newGroupId = UUID.randomUUID().toString();
        final String lastActivity = LocalDateTime.now(ZoneId.of("UTC"))
            .format(this.getDateTimeFormatter());

        final Group newGroup = new Group(jsonMap);
        newGroup.setGroupId(newGroupId);
        newGroup.setGroupCreator(activeUser);
        newGroup.setMembersLeft(Collections.emptyMap());
        newGroup.setEvents(Collections.emptyMap());
        newGroup.setLastActivity(lastActivity);
        this.putItem(newGroup);

        //old group being null signals we're creating a new group
        //updatedEventId being null signals this isn't an event update
        this.updateUsersTable(null, newGroup, null, false, metrics);
        this.updateCategoriesTable(null, newGroup, metrics);

        resultStatus = new ResultStatus(true,
            JsonUtils.convertObjectToJson(new GroupForApiResponse(newGroup).asMap()));
      } catch (Exception e) {
        resultStatus.resultMessage = "Exception in " + classMethod;
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
      }
    } else {
      metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Required request keys not found"));
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }
    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }
}
