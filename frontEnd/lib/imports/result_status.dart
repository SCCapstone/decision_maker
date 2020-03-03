class ResultStatus<T> {
  bool success;
  bool networkError;
  String errorMessage;
  T data;

  ResultStatus({this.success, this.errorMessage, this.data, this.networkError});
}