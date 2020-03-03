class ResultStatus<T> {
  bool success;
  String errorMessage;
  T data;

  ResultStatus({this.success, this.errorMessage, this.data});
}