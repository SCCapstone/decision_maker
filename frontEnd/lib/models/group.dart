class Group {
  final String groupId;
  final String groupName;
  String groupIcon;
  final String groupCreator;
  final Map<String, dynamic> members;
  final Map<String, dynamic> categories;
  final Map<String, dynamic> events;
  final int defaultPollPassPercent;
  final int defaultPollDuration;
  final int nextEventId;

  Group(
      {this.groupId,
      this.groupName,
      this.groupIcon,
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
      this.groupIcon,
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
        groupIcon: json['GroupIcon'],
        groupCreator: json['GroupCreator'],
        members: json['Members'],
        categories: json['Categories'],
        events: json['Events'],
        defaultPollPassPercent: json['DefaultPollPassPercent'],
        defaultPollDuration: json['DefaultPollDuration'],
        nextEventId: json['NextEventId']);
  }

  @override
  String toString() {
    return "Groupid: $groupId GroupName: $groupName GroupIcon: "
        "$groupIcon GroupCreator: $groupCreator Members: $members Categories: $categories Events $events"
        "DefaultPollPassPercent: $defaultPollPassPercent DefaultPollDuration: $defaultPollDuration "
        "NextEventId $nextEventId";
  }
}
