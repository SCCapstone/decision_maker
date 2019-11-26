class Event {
  final String categoryId;
  final String eventName;
  final String createdDateTime;
  final String eventStartDateTime;
  final int type;
  final int pollDuration;
  final int pollPassPercent;
  final List<String> optedIn;

  Event(
      {this.categoryId,
      this.eventName,
      this.createdDateTime,
      this.eventStartDateTime,
      this.type,
      this.pollDuration,
      this.pollPassPercent,
      this.optedIn});

  Event.debug(
      this.categoryId,
      this.eventName,
      this.createdDateTime,
      this.eventStartDateTime,
      this.type,
      this.pollDuration,
      this.pollPassPercent,
      this.optedIn);

  factory Event.fromJson(Map<String, dynamic> json) {
    return Event(
        categoryId: json['CategoryId'],
        eventName: json['EventName'],
        createdDateTime: json['CreatedDateTime'],
        eventStartDateTime: json['EventStartDateTime'],
        type: json['Type'],
        pollDuration: json['PollDuration'],
        pollPassPercent: json['PollPassPercent'],
        optedIn: json['OptedIn']);
  }

  int compareTo(Event other) {
    DateTime otherTime = DateTime.parse(other.eventStartDateTime);
    DateTime thisTime = DateTime.parse(this.eventStartDateTime);
    int order = thisTime.compareTo(otherTime);
    if (order == 0) order = thisTime.compareTo(otherTime);
    return order;
  }

  @override
  String toString() {
    return "CategoryId: $categoryId EventName: $eventName CreatedDateTime: "
        "$createdDateTime EventStartDateTime: $eventStartDateTime Type: $type PollDuration: $pollDuration "
        "PollPassPercent $pollPassPercent OptedIn: $optedIn";
  }
}
