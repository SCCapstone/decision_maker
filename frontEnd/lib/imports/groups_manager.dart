import 'dart:collection';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/groups_widgets/groups_home.dart';
import 'package:frontEnd/imports/response_item.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/utilities/request_fields.dart';
import 'package:frontEnd/utilities/utilities.dart';
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
  static final String CATEGORIES = "Categories";
  static final String DEFAULT_VOTING_DURATION = "DefaultVotingDuration";
  static final String DEFAULT_RSVP_DURATION = "DefaultRsvpDuration";
  static final String NEXT_EVENT_ID = "NextEventId";
  static final String EVENTS = "Events";

  static Future<List<Group>> getGroups({List<String> groupIds}) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "getGroups";
    if (groupIds != null) {
      jsonRequestBody["payload"]
          .putIfAbsent(RequestFields.GROUP_IDS, () => groupIds);
    }

    String response = await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response != "") {
      try {
        Map<String, dynamic> body = jsonDecode(response);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          List<dynamic> responseJson = json.decode(responseItem.resultMessage);
          return responseJson.map((m) => new Group.fromJson(m)).toList();
        } else {
          //TODO add logging (https://github.com/SCCapstone/decision_maker/issues/79)
        }
      } catch (e) {
        //TODO add logging (https://github.com/SCCapstone/decision_maker/issues/79)
      }

      return List<Group>(); // return empty list
    } else {
      //TODO add logging (https://github.com/SCCapstone/decision_maker/issues/79)
      throw Exception("Failed to load groups from the database.");
    }
  }

  static Future<bool> deleteGroup(String groupId, BuildContext context) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "deleteGroup";
    jsonRequestBody["payload"].putIfAbsent(GROUP_ID, () => groupId);

    String response = await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response != "") {
      try {
        Map<String, dynamic> body = jsonDecode(response);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          showPopupMessage(responseItem.resultMessage, context);
        } else {
          showPopupMessage("Error deleting the group (1).", context);
        }

        return responseItem.success;
      } catch (e) {
        showPopupMessage("Error deleting the group (2).", context);
      }
    } else {
      showPopupMessage("Unable to delete group.", context);
    }

    return false;
  }

  static Future<bool> createNewGroup(
      Group group, File iconFile, BuildContext context) async {
    bool retVal = false;
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "createNewGroup";
    jsonRequestBody["payload"] = group.asMap();

    jsonRequestBody["payload"].remove(ICON);
    if (iconFile != null) {
      jsonRequestBody["payload"]
          .putIfAbsent(ICON, () => iconFile.readAsBytesSync());
    }

    //update this to just be the list of usernames
    //since that is all we need to pass to the backend
    jsonRequestBody["payload"][MEMBERS] = group.members.keys.toList();

    String response = await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response != "") {
      try {
        Map<String, dynamic> body = jsonDecode(response);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          retVal = true;
        } else {
          showPopupMessage("Error creating group (1).", context);
        }
      } catch (e) {
        showPopupMessage("Error creating group (2).", context);
      }
    } else {
      showPopupMessage("Unable to create group.", context);
    }
    return retVal;
  }

  static void editGroup(
      Group group, File iconFile, BuildContext context) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "editGroup";
    jsonRequestBody["payload"] = group.asMap();

    jsonRequestBody["payload"].remove(ICON);
    if (iconFile != null) {
      jsonRequestBody["payload"]
          .putIfAbsent(ICON, () => iconFile.readAsBytesSync());
    }

    //update this to just be the list of usernames
    //since that is all we need to pass to the backend
    jsonRequestBody["payload"][MEMBERS] = group.members.keys.toList();

    String response = await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response != "") {
      try {
        Map<String, dynamic> body = jsonDecode(response);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          showPopupMessage(responseItem.resultMessage, context);
        } else {
          showPopupMessage("Error saving group data (1).", context);
        }
      } catch (e) {
        showPopupMessage("Error saving group data (2).", context);
      }
    } else {
      showPopupMessage("Unable to save group.", context);
    }
  }

  static Future<bool> newEvent(
      String groupId, Event event, BuildContext context) async {
    bool retVal = false;
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "newEvent";
    jsonRequestBody["payload"] = event.asMap();
    jsonRequestBody["payload"].putIfAbsent(GROUP_ID, () => groupId);

    String response = await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response != "") {
      try {
        Map<String, dynamic> body = jsonDecode(response);
        ResponseItem responseItem = new ResponseItem.fromJson(body);
        if (responseItem.success) {
          retVal = true;
        } else {
          showPopupMessage("Error creating event (1).", context);
        }
      } catch (e) {
        showPopupMessage("Error creating event (2).", context);
      }
    } else {
      showPopupMessage("Unable to create event.", context);
    }
    return retVal;
  }

  static Map<String, Event> getGroupEvents(Group group) {
    Map<String, Event> events = new Map<String, Event>();
    for (String eventId in group.events.keys) {
      Event event = new Event.fromJson(group.events[eventId]);
      Map<String, String> optInList = event.optedIn.cast();
      // if user has opted in, display the event to them
      events.putIfAbsent(eventId, () => event);
    }
    // sorting based on create time for now, most recently created at the top
    var sortedKeys = events.keys.toList(growable: false)
      ..sort((k1, k2) =>
          events[k2].createdDateTime.compareTo(events[k1].createdDateTime));
    LinkedHashMap sortedMap = new LinkedHashMap.fromIterable(sortedKeys,
        key: (k) => k, value: (k) => events[k]);
    return sortedMap.cast();
  }

  static void optInOutOfEvent(String groupId, String eventId,
      bool participating, BuildContext context) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "optUserInOut";
    jsonRequestBody["payload"]
        .putIfAbsent(GroupsManager.GROUP_ID, () => groupId);
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.EVENT_ID, () => eventId);
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.PARTICIPATING, () => participating);
    //TODO get display name globally and maybe allow for it to be editable somewhere (https://github.com/SCCapstone/decision_maker/issues/148)
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.DISPLAY_NAME, () => Globals.username);

    String response = await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response != "") {
      try {
        Map<String, dynamic> body = jsonDecode(response);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          showPopupMessage(responseItem.resultMessage, context);
        } else {
          showPopupMessage("Error opting in/out (1).", context);
        }
      } catch (e) {
        showPopupMessage("Error opting in/out (2).", context);
      }
    } else {
      showPopupMessage("Unable to opt in/out.", context);
    }
  }

  static void voteForChoice(String groupId, String eventId, String choiceId,
      int voteVal, BuildContext context) async {
    Map<String, dynamic> jsonRequestBody = getEmptyApiRequest();
    jsonRequestBody["action"] = "voteForChoice";
    jsonRequestBody["payload"]
        .putIfAbsent(GroupsManager.GROUP_ID, () => groupId);
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.EVENT_ID, () => eventId);
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.CHOICE_ID, () => choiceId);
    jsonRequestBody["payload"]
        .putIfAbsent(RequestFields.VOTE_VALUE, () => voteVal);

    String response = await makeApiRequest(apiEndpoint, jsonRequestBody);

    if (response != "") {
      try {
        Map<String, dynamic> body = jsonDecode(response);
        ResponseItem responseItem = new ResponseItem.fromJson(body);

        if (responseItem.success) {
          showPopupMessage(responseItem.resultMessage, context);
        } else {
          showPopupMessage("Error voting yes/no (1).", context);
        }
      } catch (e) {
        showPopupMessage("Error voting yes/no (2).", context);
      }
    } else {
      showPopupMessage("Unable to vote yes/no.", context);
    }
  }

  static List<Group> sortByDate(List<Group> groups) {
    List<Group> retGroups = groups;
    retGroups.sort((a, b) => DateTime.parse(b.lastActivity)
        .compareTo(DateTime.parse(a.lastActivity)));
    return retGroups;
  }

  static List<Group> sortByAlpha(List<Group> groups) {
    List<Group> retGroups = groups;
    retGroups.sort((a, b) => a.groupName.compareTo(b.groupName));
    return retGroups;
  }
}
