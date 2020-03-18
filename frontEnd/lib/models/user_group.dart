import 'package:frontEnd/imports/groups_manager.dart';

class UserGroup {
  final String groupId;
  final String groupName;
  final String icon;
  final String lastActivity;
  final bool muted;
  final Map<String, bool> eventsUnseen;

  UserGroup(
      {this.groupName,
        this.groupId,
      this.icon,
      this.lastActivity,
      this.muted,
      this.eventsUnseen});

  UserGroup.debug(this.groupId,this.groupName, this.icon, this.lastActivity, this.muted,
      this.eventsUnseen);

  factory UserGroup.fromJson(Map<String, dynamic> json, String groupId) {
    Map<String, bool> eventsUnseenMap = new Map<String, bool>();
    for (String eventId in json[GroupsManager.EVENTS_UNSEEN].keys) {
      eventsUnseenMap.putIfAbsent(
          eventId, () => json[GroupsManager.EVENTS_UNSEEN][eventId]);
    }
    return UserGroup(
      groupId: groupId,
        groupName: json[GroupsManager.GROUP_NAME],
        icon: json[GroupsManager.ICON],
        lastActivity: json[GroupsManager.LAST_ACTIVITY],
        muted: json[GroupsManager.MUTED],
        eventsUnseen: eventsUnseenMap);
  }

  @override
  String toString() {
    return "GroupName: $groupName GroupIcon: "
        "$icon LastActivity: $lastActivity Muted: $muted EventsUnsen: $eventsUnseen";
  }
}
