import 'dart:convert';
import 'dart:io';

import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/models/group_interface.dart';
import 'package:frontEnd/models/user_group.dart';
import 'package:frontEnd/utilities/request_fields.dart';
import 'package:frontEnd/models/group.dart';
import 'api_manager.dart';
import 'globals.dart';

class GroupsManager {
  static final String apiEndpoint = "groupsendpoint";

  //breaking style guide for consistency with backend vars
  static final String GROUP_ID = "GroupId";
  static final String GROUP_NAME = "GroupName";
  static final String ICON = "Icon";
  static final String GROUP_CREATOR = "GroupCreator";
  static final String LAST_ACTIVITY = "LastActivity";
  static final String MEMBERS = "Members";
  static final String MEMBERS_LEFT = "MembersLeft";
  static final String CATEGORIES = "Categories";
  static final String DEFAULT_VOTING_DURATION = "DefaultVotingDuration";
  static final String DEFAULT_CONSIDER_DURATION = "DefaultRsvpDuration";
  static final String EVENTS = "Events";
  static final String MUTED = "Muted";
  static final String EVENTS_UNSEEN = "EventsUnseen";

  static final String getGroupsAction = "getGroups";
  static final String deleteGroupAction = "deleteGroup";
  static final String createGroupAction = "createNewGroup";
  static final String editGroupAction = "editGroup";
  static final String newEventAction = "newEvent";
  static final String leaveGroupAction = "leaveGroup";
  static final String rejoinGroupAction = "rejoinGroup";
  static final String optInAction = "optUserInOut";
  static final String voteAction = "voteForChoice";

  static Future<ResultStatus<List<Group>>> getGroups(
      {List<String> groupIds}) async {
    ResultStatus<List<Group>> retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = getGroupsAction;
    if (groupIds != null) {
      jsonRequestBody[RequestFields.PAYLOAD]
          .putIfAbsent(RequestFields.GROUP_IDS, () => groupIds);
    }

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          List<dynamic> responseJson = json.decode(responseItem.resultMessage);
          retVal.success = true;
          retVal.data = responseJson.map((m) => new Group.fromJson(m)).toList();
        } else {
          retVal.errorMessage = "Failed to load groups";
        }
      } catch (e) {
        retVal.errorMessage = "Failed to load groups $e";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Failed to load groups from the database. Check internet connection.";
    } else {
      retVal.errorMessage = "Failed to load groups from the database.";
    }
    return retVal;
  }

  static Future<ResultStatus> deleteGroup(String groupId) async {
    ResultStatus retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = deleteGroupAction;
    jsonRequestBody[RequestFields.PAYLOAD].putIfAbsent(GROUP_ID, () => groupId);

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal.success = true;
        } else {
          retVal.errorMessage = "Error deleting the group (1).";
        }
      } catch (e) {
        retVal.errorMessage = "Error deleting the group (2).";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Failed to delete group. Check internet connection.";
    } else {
      retVal.errorMessage = "Failed to delete group.";
    }

    return retVal;
  }

  static Future<ResultStatus<Group>> createNewGroup(
      Group group, File iconFile) async {
    ResultStatus<Group> retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = createGroupAction;
    jsonRequestBody[RequestFields.PAYLOAD] = group.asMap();

    jsonRequestBody[RequestFields.PAYLOAD].remove(ICON);
    if (iconFile != null) {
      jsonRequestBody[RequestFields.PAYLOAD]
          .putIfAbsent(ICON, () => iconFile.readAsBytesSync());
    }
    jsonRequestBody[RequestFields.PAYLOAD][MEMBERS] =
        group.members.keys.toList();

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal.success = true;
          retVal.data =
              new Group.fromJson(json.decode(responseItem.resultMessage));
        } else {
          retVal.errorMessage = "Error creating group (1).";
        }
      } catch (e) {
        retVal.errorMessage = "Error creating group (2).";
      }
    } else if (retVal.networkError) {
      retVal.errorMessage =
          "Network error. Failed to create group. Check internet connection.";
    } else {
      retVal.errorMessage = "Failed to create group.";
    }
    return retVal;
  }

  static Future<ResultStatus<Group>> editGroup(
      Group group, File iconFile) async {
    ResultStatus<Group> retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = editGroupAction;
    jsonRequestBody[RequestFields.PAYLOAD] = group.asMap();

    jsonRequestBody[RequestFields.PAYLOAD].remove(ICON);
    if (iconFile != null) {
      jsonRequestBody[RequestFields.PAYLOAD]
          .putIfAbsent(ICON, () => iconFile.readAsBytesSync());
    }

    jsonRequestBody[RequestFields.PAYLOAD][MEMBERS] =
        group.members.keys.toList();

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);
        if (responseItem.success) {
          retVal.data =
              new Group.fromJson(json.decode(responseItem.resultMessage));
          retVal.success = true;
        } else {
          retVal.errorMessage = "Error saving group data (1).";
        }
      } catch (e) {
        retVal.errorMessage = "Error saving group data (2).";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Failed to edit group. Check internet connection.";
    } else {
      retVal.errorMessage = "Failed to edit group.";
    }
    return retVal;
  }

  static Future<ResultStatus> newEvent(String groupId, Event event) async {
    ResultStatus retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = newEventAction;
    jsonRequestBody[RequestFields.PAYLOAD] = event.asMap();
    jsonRequestBody[RequestFields.PAYLOAD].putIfAbsent(GROUP_ID, () => groupId);

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);
        if (responseItem.success) {
          retVal.success = true;
        } else {
          retVal.errorMessage = "Error creating event (1).";
        }
      } catch (e) {
        retVal.errorMessage = "Error creating event (2).";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Failed to create event. Check internet connection.";
    } else {
      retVal.errorMessage = "Failed to create event.";
    }
    return retVal;
  }

  static Future<ResultStatus> leaveGroup(String groupId) async {
    ResultStatus retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = leaveGroupAction;
    jsonRequestBody[RequestFields.PAYLOAD].putIfAbsent(GROUP_ID, () => groupId);

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal.success = true;
        } else {
          retVal.errorMessage = "Error leaving the group (1).";
        }
      } catch (e) {
        retVal.errorMessage = "Error leaving the group (2).";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Failed to leave group. Check internet connection.";
    } else {
      retVal.errorMessage = "Failed to leave group.";
    }

    return retVal;
  }

  static Future<ResultStatus> rejoinGroup(String groupId) async {
    ResultStatus retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = rejoinGroupAction;
    jsonRequestBody[RequestFields.PAYLOAD].putIfAbsent(GROUP_ID, () => groupId);

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal.success = true;
        } else {
          retVal.errorMessage = "Error rejoining the group (1).";
        }
      } catch (e) {
        retVal.errorMessage = "Error rejoining the group (2).";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
      "Network error. Failed to rejoin group. Check internet connection.";
    } else {
      retVal.errorMessage = "Failed to rejoin group.";
    }

    return retVal;
  }

  static Future<ResultStatus> optInOutOfEvent(
      String groupId, String eventId, bool participating) async {
    ResultStatus retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = optInAction;
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(GroupsManager.GROUP_ID, () => groupId);
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(RequestFields.EVENT_ID, () => eventId);
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(RequestFields.PARTICIPATING, () => participating);
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(RequestFields.DISPLAY_NAME, () => Globals.username);

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal.success = true;
        } else {
          retVal.errorMessage = "Error considering (1).";
        }
      } catch (e) {
        retVal.errorMessage = "Error considering (2).";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Failed to consider. Check internet connection.";
    } else {
      retVal.errorMessage = "Failed to consdier.";
    }
    return retVal;
  }

  static Future<ResultStatus> voteForChoice(
      String groupId, String eventId, String choiceId, int voteVal) async {
    ResultStatus retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = voteAction;
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(GroupsManager.GROUP_ID, () => groupId);
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(RequestFields.EVENT_ID, () => eventId);
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(RequestFields.CHOICE_ID, () => choiceId);
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(RequestFields.VOTE_VALUE, () => voteVal);

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal.success = true;
        } else {
          retVal.errorMessage = "Error voting yes/no (1).";
        }
      } catch (e) {
        retVal.errorMessage = "Error voting yes/no (2).";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Failed to cast vote. Check internet connection.";
    } else {
      retVal.errorMessage = "Failed to cast vote.";
    }
    return retVal;
  }

  static void sortByDateNewest(List<UserGroup> groups) {
    groups.sort((a, b) => DateTime.parse(b.lastActivity)
        .compareTo(DateTime.parse(a.lastActivity)));
  }

  static void sortByDateOldest(List<UserGroup> groups) {
    groups.sort((a, b) => DateTime.parse(a.lastActivity)
        .compareTo(DateTime.parse(b.lastActivity)));
  }

  static void sortByAlphaAscending(List<GroupInterface> groups) {
    groups.sort((a, b) => a
        .getGroupName()
        .toUpperCase()
        .compareTo(b.getGroupName().toUpperCase()));
  }

  static void sortByAlphaDescending(List<GroupInterface> groups) {
    groups.sort((a, b) => b
        .getGroupName()
        .toUpperCase()
        .compareTo(a.getGroupName().toUpperCase()));
  }
}
