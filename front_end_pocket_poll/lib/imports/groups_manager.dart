import 'dart:convert';
import 'dart:io';

import 'package:front_end_pocket_poll/imports/categories_manager.dart';
import 'package:front_end_pocket_poll/imports/events_manager.dart';
import 'package:front_end_pocket_poll/imports/response_item.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/models/categoryRatingTuple.dart';
import 'package:front_end_pocket_poll/models/event.dart';
import 'package:front_end_pocket_poll/models/group_interface.dart';
import 'package:front_end_pocket_poll/models/user_group.dart';
import 'package:front_end_pocket_poll/utilities/request_fields.dart';
import 'package:front_end_pocket_poll/models/group.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'api_manager.dart';

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
  static final String TOTAL_NUMBER_OF_EVENTS = "TotalNumberOfEvents";
  static final int BATCH_SIZE = 25;
  static final String IS_OPEN = "IsOpen";

  static final String getGroupAction = "getGroup";
  static final String deleteGroupAction = "deleteGroup";
  static final String createGroupAction = "createNewGroup";
  static final String editGroupAction = "editGroup";
  static final String newEventAction = "newEvent";
  static final String leaveGroupAction = "leaveGroup";
  static final String rejoinGroupAction = "rejoinGroup";
  static final String optInAction = "optUserInOut";
  static final String voteAction = "voteForChoice";

  static Future<ResultStatus<Group>> getGroup(String groupId,
      {int batchNumber}) async {
    ResultStatus<Group> retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = getGroupAction;
    jsonRequestBody[RequestFields.PAYLOAD].putIfAbsent(GROUP_ID, () => groupId);

    if (batchNumber == null) {
      // batchNumber is used to get a discrete number of events from the DB
      batchNumber = 0;
    }

    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(RequestFields.BATCH_NUMBER, () => batchNumber);

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
          retVal.errorMessage = "Unable to load group.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to load group.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to load group. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to load group.";
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
          retVal.errorMessage = "Unable to delete group.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to delete group.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to delete group. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to delete group.";
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
          retVal.errorMessage = "Unable to create group.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to create group.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to create group. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to create group.";
    }
    return retVal;
  }

  static Future<ResultStatus<Group>> editGroup(Group group, File iconFile,
      {int batchNumber}) async {
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

    if (batchNumber == null) {
      // batchNumber is used to get a discrete number of events from the DB
      batchNumber = 0;
    }

    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(RequestFields.BATCH_NUMBER, () => batchNumber);

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
          retVal.errorMessage = "Unable to update group."
              "${responseItem.resultMessage}";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to update group.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to update group. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to update group.";
    }
    return retVal;
  }

  static Future<ResultStatus<Group>> newEvent(
      String groupId, Event event) async {
    ResultStatus<Group> retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = newEventAction;
    jsonRequestBody[RequestFields.PAYLOAD] = event.asMap();
    jsonRequestBody[RequestFields.PAYLOAD].putIfAbsent(GROUP_ID, () => groupId);
    jsonRequestBody[RequestFields.PAYLOAD].putIfAbsent(
        EventsManager.UTC_EVENT_START_SECONDS,
        () => getUtcSecondsSinceEpoch(event.eventStartDateTime));

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
          retVal.errorMessage = "Unable to create event.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to create event.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to create event. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to create event.";
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
          retVal.errorMessage = "Unable to leave group.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to leave group.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to leave group. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to leave group.";
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
          retVal.errorMessage = "Unable to rejoin group.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to rejoin group.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to rejoin group. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to rejoin group.";
    }

    return retVal;
  }

  // used to consider a user for an event
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

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal.success = true;
        } else {
          retVal.errorMessage = "Unable to consider.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to consider.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to consider. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to consider.";
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
          retVal.errorMessage = "Unable to vote.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to vote.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to vote. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to vote.";
    }
    return retVal;
  }

  // sorts a list of groups by date modified (ascending)
  static void sortByDateNewest(List<UserGroup> groups) {
    groups.sort((a, b) => DateTime.parse(b.lastActivity)
        .compareTo(DateTime.parse(a.lastActivity)));
  }

  // sorts a list of groups by date modified (descending)
  static void sortByDateOldest(List<UserGroup> groups) {
    groups.sort((a, b) => DateTime.parse(a.lastActivity)
        .compareTo(DateTime.parse(b.lastActivity)));
  }

  // sorts a list of groups alphabetically (ascending)
  static void sortByAlphaAscending(List<GroupInterface> groups) {
    groups.sort((a, b) => a
        .getGroupName()
        .toUpperCase()
        .compareTo(b.getGroupName().toUpperCase()));
  }

  // sorts a list of groups alphabetically (descending)
  static void sortByAlphaDescending(List<GroupInterface> groups) {
    groups.sort((a, b) => b
        .getGroupName()
        .toUpperCase()
        .compareTo(a.getGroupName().toUpperCase()));
  }

  static Future<ResultStatus<List<CategoryRatingTuple>>> getAllCategoriesList(
      final String groupId) async {
    ResultStatus<List<CategoryRatingTuple>> retVal =
        new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = CategoriesManager.getAction;
    jsonRequestBody[RequestFields.PAYLOAD].putIfAbsent(GROUP_ID, () => groupId);

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          List<dynamic> responseJson = json.decode(responseItem.resultMessage);
          retVal.data = responseJson
              .map((m) => new CategoryRatingTuple.fromJson(m))
              .toList();
          retVal.success = true;
        } else {
          retVal.errorMessage = "Unable to load categories.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to load categories.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to load categories. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to load categories.";
    }
    return retVal;
  }

  static Future<ResultStatus<Category>> getEventCategory(
      final String groupId, final String eventId) async {
    ResultStatus<Category> retVal = new ResultStatus(success: false);

    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody[RequestFields.ACTION] = CategoriesManager.getAction;
    jsonRequestBody[RequestFields.PAYLOAD].putIfAbsent(GROUP_ID, () => groupId);
    jsonRequestBody[RequestFields.PAYLOAD]
        .putIfAbsent(RequestFields.EVENT_ID, () => eventId);

    ResultStatus<String> response =
        await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response.success) {
      try {
        Map<String, dynamic> body = jsonDecode(response.data);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          List<dynamic> responseJson = json.decode(responseItem.resultMessage);
          retVal.data = new Category.fromJson(responseJson.first());
          retVal.success = true;
        } else {
          retVal.errorMessage = "Unable to load categories.";
        }
      } catch (e) {
        retVal.errorMessage = "Unable to load categories.";
      }
    } else if (response.networkError) {
      retVal.errorMessage =
          "Network error. Unable to load categories. Check internet connection.";
    } else {
      retVal.errorMessage = "Unable to load categories.";
    }
    return retVal;
  }
}
