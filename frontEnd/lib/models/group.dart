import 'package:frontEnd/imports/groups_manager.dart';

class Group {
  final String groupId;
  final String groupName;
  final String icon;
  final String groupCreator;
  final String lastActivity;
  final Map<String, dynamic> members;
  final Map<String, dynamic> categories;
  final Map<String, dynamic> events;
  final int defaultVotingDuration;
  final int defaultRsvpDuration;
  final int nextEventId;

  Group(
      {this.groupId,
      this.groupName,
      this.icon,
      this.groupCreator,
      this.lastActivity,
      this.members,
      this.categories,
      this.events,
      this.defaultVotingDuration,
      this.defaultRsvpDuration,
      this.nextEventId});

  Group.debug(
      this.groupId,
      this.groupName,
      this.icon,
      this.groupCreator,
      this.lastActivity,
      this.members,
      this.categories,
      this.events,
      this.defaultVotingDuration,
      this.defaultRsvpDuration,
      this.nextEventId);

  factory Group.fromJson(Map<String, dynamic> json) {
    return Group(
        groupId: json[GroupsManager.GROUP_ID],
        groupName: json[GroupsManager.GROUP_NAME],
        icon: json[GroupsManager.ICON],
        groupCreator: json[GroupsManager.GROUP_CREATOR],
        lastActivity: json[GroupsManager.LAST_ACTIVITY],
        members: json[GroupsManager.MEMBERS],
        categories: json[GroupsManager.CATEGORIES],
        events: json[GroupsManager.EVENTS],
        defaultVotingDuration:
            int.parse(json[GroupsManager.DEFAULT_VOTING_DURATION]),
        defaultRsvpDuration:
            int.parse(json[GroupsManager.DEFAULT_RSVP_DURATION]),
        nextEventId: int.parse(json[GroupsManager.NEXT_EVENT_ID]));
  }

  @override
  String toString() {
    return "Groupid: $groupId GroupName: $groupName GroupIcon: "
        "$icon GroupCreator: $groupCreator LastActivity: $lastActivity Members: $members Categories: $categories Events: $events"
        "DefaultVotingDuration: $defaultVotingDuration DefaultRsvpDuration: $defaultRsvpDuration NextEventId: $nextEventId";
  }

  Map asMap() {
    return {
      GroupsManager.GROUP_ID: this.groupId,
      GroupsManager.GROUP_NAME: this.groupName,
      GroupsManager.ICON: this.icon,
      GroupsManager.GROUP_CREATOR: this.groupCreator,
      GroupsManager.LAST_ACTIVITY: this.lastActivity,
      GroupsManager.MEMBERS: this.members,
      GroupsManager.CATEGORIES: this.categories,
      GroupsManager.EVENTS: this.events,
      GroupsManager.DEFAULT_VOTING_DURATION: this.defaultVotingDuration,
      GroupsManager.DEFAULT_RSVP_DURATION: this.defaultRsvpDuration,
      GroupsManager.NEXT_EVENT_ID: this.nextEventId
    };
  }
}
