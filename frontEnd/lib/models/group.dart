import 'package:frontEnd/imports/groups_manager.dart';

class Group {
  final String groupId;
  final String groupName;
  final String icon;
  final String groupCreator;
  final Map<String, dynamic> members;
  final Map<String, dynamic> categories;
  final Map<dynamic, dynamic> events;
  final int defaultPollPassPercent;
  final int defaultPollDuration;
  final int nextEventId;

  Group(
      {this.groupId,
      this.groupName,
      this.icon,
      this.groupCreator,
      this.members,
      this.categories,
      this.events,
      this.defaultPollPassPercent,
      this.defaultPollDuration,
      this.nextEventId});

  Group.debug(
      this.groupId,
      this.groupName,
      this.icon,
      this.groupCreator,
      this.members,
      this.categories,
      this.events,
      this.defaultPollPassPercent,
      this.defaultPollDuration,
      this.nextEventId);

  factory Group.fromJson(Map<String, dynamic> json) {
    return Group(
        groupId: json['GroupId'],
        groupName: json['GroupName'],
        icon: json['Icon'],
        groupCreator: json['GroupCreator'],
        members: json['Members'],
        categories: json['Categories'],
        events: json['Events'],
        defaultPollPassPercent: int.parse(json['DefaultPollPassPercent']),
        defaultPollDuration: int.parse(json['DefaultPollDuration']),
        nextEventId: int.parse(json['NextEventId']));
  }

  @override
  String toString() {
    return "Groupid: $groupId GroupName: $groupName GroupIcon: "
        "$icon GroupCreator: $groupCreator Members: $members Categories: $categories Events $events"
        "DefaultPollPassPercent: $defaultPollPassPercent DefaultPollDuration: $defaultPollDuration "
        "NextEventId $nextEventId";
  }

  Map asMap() {
    return {
      GroupsManager.GROUP_ID: this.groupId,
      GroupsManager.GROUP_NAME: this.groupName,
      GroupsManager.ICON: this.icon,
      GroupsManager.GROUP_CREATOR: this.groupCreator,
      GroupsManager.MEMBERS: this.members,
      GroupsManager.CATEGORIES: this.categories,
      GroupsManager.EVENTS: this.events,
      GroupsManager.DEFAULT_POLL_PASS_PERCENT: this.defaultPollPassPercent,
      GroupsManager.DEFAULT_POLL_DURATION: this.defaultPollDuration,
      GroupsManager.NEXT_EVENT_ID: this.nextEventId
    };
  }
}
