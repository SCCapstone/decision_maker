import 'package:front_end_pocket_poll/imports/groups_manager.dart';

import 'group.dart';

class GetGroupResponse {

  //breaking style for consistency with the back end
  static final String USER_INFO = "UserInfo";
  static final String EVENTS_WITHOUT_RATINGS = "EventsWithoutRatings";
  static final String GROUP_INFO = "GroupInfo";

  Map<String, dynamic> eventsUnseen;
  Map<String, dynamic> eventsWithoutRatings;
  Group groupInfo;

  GetGroupResponse();

  setGroupInfo(Map<String, dynamic> groupJson) {
    this.groupInfo = Group.fromJson(groupJson);
  }

  setUserInfo(Map<String, dynamic> userJson) {
    this.eventsUnseen = userJson[GroupsManager.EVENTS_UNSEEN];
    this.eventsWithoutRatings = userJson[EVENTS_WITHOUT_RATINGS];
  }

  factory GetGroupResponse.fromJson(Map<String, dynamic> json) {
    final GetGroupResponse getGroupResponse = new GetGroupResponse();

    getGroupResponse.setUserInfo(json[USER_INFO]);
    getGroupResponse.setGroupInfo(json[GROUP_INFO]);

    return getGroupResponse;
  }
}