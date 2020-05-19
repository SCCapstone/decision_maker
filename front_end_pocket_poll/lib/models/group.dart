import 'dart:collection';

import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/models/event.dart';
import 'package:front_end_pocket_poll/models/member.dart';

import 'group_category.dart';

class Group {
  final String groupId;
  final String groupName;
  final String icon;
  final String groupCreator;
  final String lastActivity;
  final Map<String, Member> members;
  final Map<String, bool> membersLeft;
  final Map<String, GroupCategory> categories;
  final Map<String, Event> events;
  final int defaultVotingDuration;
  final int defaultConsiderDuration;
  final int totalNumberOfEvents;
  int currentBatchNum = 0;
  final bool isOpen;

  Group(
      {this.groupId,
      this.groupName,
      this.icon,
      this.groupCreator,
      this.lastActivity,
      this.members,
      this.membersLeft,
      this.categories,
      this.events,
      this.defaultVotingDuration,
      this.defaultConsiderDuration,
      this.totalNumberOfEvents,
      this.isOpen});

  Group.debug(
      this.groupId,
      this.groupName,
      this.icon,
      this.groupCreator,
      this.lastActivity,
      this.members,
      this.membersLeft,
      this.categories,
      this.events,
      this.defaultVotingDuration,
      this.defaultConsiderDuration,
      this.totalNumberOfEvents,
      this.isOpen);

  factory Group.fromJson(Map<String, dynamic> json) {
    // map of eventId -> event
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

    // map of username -> Member
    Map<String, Member> memberMap = new Map<String, Member>();
    for (String username in json[GroupsManager.MEMBERS].keys) {
      Member member =
          new Member.fromJson(json[GroupsManager.MEMBERS][username], username);
      memberMap.putIfAbsent(username, () => member);
    }

    // map of categoryId -> GroupCategory
    Map<String, GroupCategory> categoriesMap = new Map<String, GroupCategory>();
    for (String categoryId in json[GroupsManager.CATEGORIES].keys) {
      GroupCategory groupCategory = new GroupCategory.fromJson(
          json[GroupsManager.CATEGORIES][categoryId]);
      categoriesMap.putIfAbsent(categoryId, () => groupCategory);
    }

    Map<String, bool> membersLeftMap = new Map<String, bool>();
    for (String username in json[GroupsManager.MEMBERS_LEFT].keys) {
      membersLeftMap.putIfAbsent(
          username, () => json[GroupsManager.MEMBERS_LEFT][username]);
    }

    return Group(
        groupId: json[GroupsManager.GROUP_ID],
        groupName: json[GroupsManager.GROUP_NAME],
        icon: json[GroupsManager.ICON],
        groupCreator: json[GroupsManager.GROUP_CREATOR],
        lastActivity: json[GroupsManager.LAST_ACTIVITY],
        members: memberMap,
        membersLeft: membersLeftMap,
        categories: categoriesMap,
        events: events,
        defaultVotingDuration: json[GroupsManager.DEFAULT_VOTING_DURATION],
        defaultConsiderDuration: json[GroupsManager.DEFAULT_CONSIDER_DURATION],
        totalNumberOfEvents: json[GroupsManager.TOTAL_NUMBER_OF_EVENTS],
        isOpen: json[GroupsManager.IS_OPEN]);
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
        "$icon GroupCreator: $groupCreator LastActivity: $lastActivity Members: $members MembersLeft: $membersLeft"
        "Categories: $categories Events: $events DefaultVotingDuration: $defaultVotingDuration"
        "DefaultRsvpDuration: $defaultConsiderDuration TotalNumberOfEvents $totalNumberOfEvents"
        "IsOpen: $isOpen";
  }

  Map asMap() {
    // need this for encoding to work properly
    Map<String, dynamic> eventsMap = new Map<String, dynamic>();
    for (String eventId in this.events.keys) {
      eventsMap.putIfAbsent(eventId, () => this.events[eventId].asMap());
    }
    Map<String, dynamic> membersMap = new Map<String, dynamic>();
    for (String username in this.members.keys) {
      membersMap.putIfAbsent(username, () => this.members[username].asMap());
    }
    Map<String, dynamic> membersLeftMap = new Map<String, dynamic>();
    if (this.membersLeft != null) {
      for (String username in this.membersLeft.keys) {
        membersLeftMap.putIfAbsent(username, () => this.membersLeft[username]);
      }
    }
    return {
      GroupsManager.GROUP_ID: this.groupId,
      GroupsManager.GROUP_NAME: this.groupName,
      GroupsManager.ICON: this.icon,
      GroupsManager.GROUP_CREATOR: this.groupCreator,
      GroupsManager.LAST_ACTIVITY: this.lastActivity,
      GroupsManager.MEMBERS: membersMap,
      GroupsManager.MEMBERS_LEFT: membersLeftMap,
      GroupsManager.CATEGORIES: this.categories,
      GroupsManager.EVENTS: eventsMap,
      GroupsManager.DEFAULT_VOTING_DURATION: this.defaultVotingDuration,
      GroupsManager.DEFAULT_CONSIDER_DURATION: this.defaultConsiderDuration,
      GroupsManager.TOTAL_NUMBER_OF_EVENTS: this.totalNumberOfEvents,
      GroupsManager.IS_OPEN: this.isOpen
    };
  }
}
