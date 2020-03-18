import 'dart:collection';

import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/models/member.dart';

class Group {
  final String groupId;
  final String groupName;
  final String icon;
  final String groupCreator;
  final String lastActivity;
  final Map<String, Member> members;
  final Map<String, String> categories;
  final Map<String, Event> events;
  final int defaultVotingDuration;
  final int defaultConsiderDuration;
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
      this.defaultConsiderDuration,
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
      this.defaultConsiderDuration,
      this.nextEventId);

  factory Group.fromJson(Map<String, dynamic> json) {
    Map<String, Event> events = new Map<String, Event>();
    for (String eventId in json[GroupsManager.EVENTS].keys) {
      Event event = new Event.fromJson(json[GroupsManager.EVENTS][eventId]);
      events.putIfAbsent(eventId, () => event);
    }
    // sorting based on create time for now, most recently created at the top
    List<String> sortedKeys = events.keys.toList(growable: false)
      ..sort((k1, k2) =>
          events[k2].createdDateTime.compareTo(events[k1].createdDateTime));
    LinkedHashMap sortedMap = new LinkedHashMap.fromIterable(sortedKeys,
        key: (k) => k, value: (k) => events[k]);
    events = sortedMap.cast();

    Map<String, Member> memberMap = new Map<String, Member>();
    for (String username in json[GroupsManager.MEMBERS].keys) {
      Member member = new Member(
          username: username,
          displayName: json[GroupsManager.MEMBERS][username]
              [UsersManager.DISPLAY_NAME],
          icon: json[GroupsManager.MEMBERS][username][UsersManager.ICON]);
      memberMap.putIfAbsent(username, () => member);
    }

    Map<String, String> categoriesMap = new Map<String, String>();
    for (String categoryId in json[GroupsManager.CATEGORIES].keys) {
      categoriesMap.putIfAbsent(categoryId,
          () => json[GroupsManager.CATEGORIES][categoryId].toString());
    }

    return Group(
        groupId: json[GroupsManager.GROUP_ID],
        groupName: json[GroupsManager.GROUP_NAME],
        icon: json[GroupsManager.ICON],
        groupCreator: json[GroupsManager.GROUP_CREATOR],
        lastActivity: json[GroupsManager.LAST_ACTIVITY],
        members: memberMap,
        categories: categoriesMap,
        events: events,
        defaultVotingDuration: json[GroupsManager.DEFAULT_VOTING_DURATION],
        defaultConsiderDuration: json[GroupsManager.DEFAULT_CONSIDER_DURATION],
        nextEventId: json[GroupsManager.NEXT_EVENT_ID]);
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }
    return other is Group && this.groupId == other.groupId;
  }

  @override
  int get hashCode {
    return groupId.hashCode;
  }

  @override
  String toString() {
    return "Groupid: $groupId GroupName: $groupName GroupIcon: "
        "$icon GroupCreator: $groupCreator LastActivity: $lastActivity Members: $members Categories: $categories Events: $events"
        "DefaultVotingDuration: $defaultVotingDuration DefaultRsvpDuration: $defaultConsiderDuration NextEventId: $nextEventId";
  }

  Map asMap() {
    Map<String, dynamic> eventsMap = new Map<String, dynamic>();
    for (String eventId in this.events.keys) {
      eventsMap.putIfAbsent(eventId, () => this.events[eventId].asMap());
    }
    return {
      GroupsManager.GROUP_ID: this.groupId,
      GroupsManager.GROUP_NAME: this.groupName,
      GroupsManager.ICON: this.icon,
      GroupsManager.GROUP_CREATOR: this.groupCreator,
      GroupsManager.LAST_ACTIVITY: this.lastActivity,
      GroupsManager.MEMBERS: this.members,
      GroupsManager.CATEGORIES: this.categories,
      GroupsManager.EVENTS: eventsMap,
      GroupsManager.DEFAULT_VOTING_DURATION: this.defaultVotingDuration,
      GroupsManager.DEFAULT_CONSIDER_DURATION: this.defaultConsiderDuration,
      GroupsManager.NEXT_EVENT_ID: this.nextEventId
    };
  }
}
