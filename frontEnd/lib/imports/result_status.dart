class ResultStatus {
  bool success;
  bool networkError;
  String errorMessage;
  dynamic data;

  ResultStatus({this.success, this.errorMessage, this.data, this.networkError});
}