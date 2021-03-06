import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/models/group_interface.dart';

class UserGroup implements GroupInterface {
  final String groupId;
  final String groupName;
  final String icon;
  String lastActivity;
  int eventsUnseen;

  bool muted;

  UserGroup(
      {this.groupName,
      this.groupId,
      this.icon,
      this.lastActivity,
      this.muted,
      this.eventsUnseen});

  UserGroup.debug(this.groupId, this.groupName, this.icon, this.lastActivity,
      this.muted, this.eventsUnseen);

  factory UserGroup.fromJson(Map<String, dynamic> json, String groupId) {
    return UserGroup(
        groupId: groupId,
        groupName: json[GroupsManager.GROUP_NAME],
        icon: json[GroupsManager.ICON],
        lastActivity: json[GroupsManager.LAST_ACTIVITY],
        muted: json[GroupsManager.MUTED],
        eventsUnseen: json[GroupsManager.EVENTS_UNSEEN]);
  }

  Map asMap() {
    return {
      GroupsManager.GROUP_ID: groupId,
      GroupsManager.GROUP_NAME: groupName,
      GroupsManager.ICON: icon,
      GroupsManager.LAST_ACTIVITY: lastActivity,
      GroupsManager.MUTED: muted,
      GroupsManager.EVENTS_UNSEEN: eventsUnseen
    };
  }

  @override
  String toString() {
    return "GroupName: $groupName GroupIcon: "
        "$icon LastActivity: $lastActivity Muted: $muted EventsUnsen: $eventsUnseen";
  }

  @override
  String getGroupName() {
    return groupName;
  }
}
