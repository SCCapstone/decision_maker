import 'package:frontEnd/imports/groups_manager.dart';

class GroupLeft {
  final String groupId;
  final String groupName;
  final String icon;

  GroupLeft(
      {this.groupName,
        this.groupId,
        this.icon,});

  GroupLeft.debug(this.groupId,this.groupName, this.icon);

  factory GroupLeft.fromJson(Map<String, dynamic> json, String groupId) {
    return GroupLeft(
        groupId: groupId,
        groupName: json[GroupsManager.GROUP_NAME],
        icon: json[GroupsManager.ICON],);
  }

  @override
  String toString() {
    return "GroupId: $groupId GroupName: $groupName GroupIcon: $icon ";
  }
}
