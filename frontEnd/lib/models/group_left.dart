import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/models/group_interface.dart';

class GroupLeft implements GroupInterface {
  final String groupId;
  final String groupName;
  final String icon;

  GroupLeft({
    this.groupName,
    this.groupId,
    this.icon,
  });

  GroupLeft.debug(this.groupId, this.groupName, this.icon);

  factory GroupLeft.fromJson(Map<String, dynamic> json, String groupId) {
    return GroupLeft(
      groupId: groupId,
      groupName: json[GroupsManager.GROUP_NAME],
      icon: json[GroupsManager.ICON],
    );
  }

  Map asMap() {
    return {
      GroupsManager.GROUP_ID: groupId,
      GroupsManager.GROUP_NAME: groupName,
      GroupsManager.ICON: icon,
    };
  }

  @override
  String toString() {
    return "GroupId: $groupId GroupName: $groupName GroupIcon: $icon ";
  }

  @override
  String getGroupName() {
    return groupName;
  }
}
