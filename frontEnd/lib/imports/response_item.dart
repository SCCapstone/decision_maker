class ResponseItem {
  final bool success;
  final String resultMessage;

  ResponseItem({this.success, this.resultMessage});

  factory ResponseItem.fromJson(Map<String, dynamic> json) {
    return ResponseItem(
      success: (json['success'] == "true" ? true : false),
      resultMessage: (json['resultMessage'] != null ? json['resultMessage'] : "null"),
    );
  }
}
