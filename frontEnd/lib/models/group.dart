class Group {
  final String groupId;
  final String groupName;
  final String groupIcon;
  final String groupCreator;
  final Map<String, dynamic> members;
  final Map<String, dynamic> categories;
  final int defaultPollPassPercent;
  final int defaultPollDuration;

  Group(
      {this.groupId,
      this.groupName,
      this.groupIcon,
      this.groupCreator,
      this.members,
      this.categories,
      this.defaultPollPassPercent,
      this.defaultPollDuration});

  Group.debug(
      this.groupId,
      this.groupName,
      this.groupIcon,
      this.groupCreator,
      this.members,
      this.categories,
      this.defaultPollPassPercent,
      this.defaultPollDuration);

  factory Group.fromJson(Map<String, dynamic> json) {
    return Group(
        groupId: json['GroupId'],
        groupName: json['GroupName'],
        groupIcon: json['GroupIcon'],
        groupCreator: json['GroupCreator'],
        members: json['Members'],
        categories: json['Categories'],
        defaultPollPassPercent: json['DefaultPollPassPercent'],
        defaultPollDuration: json['DefaultPollDuration']);
  }

  @override
  String toString() {
    return "Groupid: $groupId GroupName: $groupName GroupIcon: "
        "$groupIcon GroupCreator: $groupCreator Members: $members Categories: $categories "
        "DefaultPollPassPercent: $defaultPollPassPercent DefaultPollDuration: $defaultPollDuration";
  }
}
