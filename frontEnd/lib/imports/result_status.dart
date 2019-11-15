class ResultStatus {
  final bool success;
  final String resultMessage;

  ResultStatus({this.success, this.resultMessage});

  factory ResultStatus.fromJson(Map<String, dynamic> json) {
    return ResultStatus(
      success: json['success'],
      resultMessage: json['resultMessage'],
    );
  }
}