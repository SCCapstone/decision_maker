import 'package:front_end_pocket_poll/events_widgets/events_list.dart';
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
  final Map<String, Event> newEvents;
  final Map<String, Event> votingEvents;
  final Map<String, Event> considerEvents;
  final Map<String, Event> closedEvents;
  final Map<String, Event> occurringEvents;
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
      this.newEvents,
      this.votingEvents,
      this.considerEvents,
      this.closedEvents,
      this.occurringEvents,
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
      this.newEvents,
      this.votingEvents,
      this.considerEvents,
      this.closedEvents,
      this.occurringEvents,
      this.defaultVotingDuration,
      this.defaultConsiderDuration,
      this.totalNumberOfEvents,
      this.isOpen);

  factory Group.fromJson(Map<String, dynamic> json) {
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
        newEvents: getEventsMapFromJson(json[GroupsManager.NEW_EVENTS]),
        votingEvents: getEventsMapFromJson(json[GroupsManager.VOTING_EVENTS]),
        considerEvents:
            getEventsMapFromJson(json[GroupsManager.CONSIDER_EVENTS]),
        closedEvents: getEventsMapFromJson(json[GroupsManager.CLOSED_EVENTS]),
        occurringEvents:
            getEventsMapFromJson(json[GroupsManager.OCCURRING_EVENTS]),
        defaultVotingDuration: json[GroupsManager.DEFAULT_VOTING_DURATION],
        defaultConsiderDuration: json[GroupsManager.DEFAULT_CONSIDER_DURATION],
        totalNumberOfEvents: json[GroupsManager.TOTAL_NUMBER_OF_EVENTS],
        isOpen: json[GroupsManager.IS_OPEN]);
  }

  void addEvents(final Map<String, Event> events, final int eventsType) {
    Map<String, Event> groupEventsReference;

    if (eventsType == EventsList.eventsTypeClosed) {
      groupEventsReference = this.closedEvents;
    } else if (eventsType == EventsList.eventsTypeVoting) {
      groupEventsReference = this.votingEvents;
    } else if (eventsType == EventsList.eventsTypeConsider) {
      groupEventsReference = this.considerEvents;
    } else if (eventsType == EventsList.eventsTypeOccurring) {
      groupEventsReference = this.occurringEvents;
    } else if (eventsType == EventsList.eventsTypeNew) {
      groupEventsReference = this.newEvents;
    }

    for (MapEntry<String, Event> eventEntry in events.entries) {
      groupEventsReference.putIfAbsent(eventEntry.key, () => eventEntry.value);
    }
  }

  Map<String, Event> getEventsFromBatchType(final int batchType) {
    if (batchType == EventsList.eventsTypeNew) {
      return this.newEvents;
    } else if (batchType == EventsList.eventsTypeVoting) {
      return this.votingEvents;
    } else if (batchType == EventsList.eventsTypeClosed) {
      return this.closedEvents;
    } else if (batchType == EventsList.eventsTypeConsider) {
      return this.considerEvents;
    } else if (batchType == EventsList.eventsTypeOccurring) {
      return this.occurringEvents;
    }

    return null;
  }

  static Map<String, Event> getEventsMapFromJson(
      final Map<String, dynamic> json) {
    if (json == null) {
      return null;
    }

    // map of eventId -> event
    final Map<String, Event> eventsMap = new Map<String, Event>();
    for (final String eventId in json.keys) {
      eventsMap.putIfAbsent(eventId, () => new Event.fromJson(json[eventId]));
    }
    return eventsMap;
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
        "Categories: $categories NewEvents: $newEvents VotingEvents $votingEvents ConsiderEvents $considerEvents ClosedEvents $closedEvents OccurringEvents $occurringEvents DefaultVotingDuration: $defaultVotingDuration"
        "DefaultRsvpDuration: $defaultConsiderDuration TotalNumberOfEvents $totalNumberOfEvents"
        "IsOpen: $isOpen";
  }

  Map asMap() {
    // need this for encoding to work properly
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
      GroupsManager.NEW_EVENTS: this.getDynamicMapFromEventsMap(this.newEvents),
      GroupsManager.VOTING_EVENTS:
          this.getDynamicMapFromEventsMap(this.votingEvents),
      GroupsManager.CONSIDER_EVENTS:
          this.getDynamicMapFromEventsMap(this.considerEvents),
      GroupsManager.CLOSED_EVENTS:
          this.getDynamicMapFromEventsMap(this.closedEvents),
      GroupsManager.OCCURRING_EVENTS:
          this.getDynamicMapFromEventsMap(this.occurringEvents),
      GroupsManager.DEFAULT_VOTING_DURATION: this.defaultVotingDuration,
      GroupsManager.DEFAULT_CONSIDER_DURATION: this.defaultConsiderDuration,
      GroupsManager.TOTAL_NUMBER_OF_EVENTS: this.totalNumberOfEvents,
      GroupsManager.IS_OPEN: this.isOpen
    };
  }

  Map<String, dynamic> getDynamicMapFromEventsMap(
      final Map<String, Event> eventsMap) {
    Map<String, dynamic> dynamicMap = new Map<String, dynamic>();
    for (String eventId in eventsMap.keys) {
      dynamicMap.putIfAbsent(eventId, () => eventsMap[eventId].asMap());
    }
    return dynamicMap;
  }
}
