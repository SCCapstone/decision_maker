class ResponseItem {
  final bool success;
  final String message;

  ResponseItem(this.success, this.message);

  bool getSuccess(){
    return this.success;
  }
}