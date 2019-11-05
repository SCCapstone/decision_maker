//TODO add unit testing https://github.com/SCCapstone/decision_maker/issues/80
import 'response_item.dart';

class UsersManager {
  static final tableName = "users";
  static final String apiEndpoint = "https://9zh1udqup3.execute-api.us-east-2.amazonaws.com/beta/usersendpoint";

  static ResponseItem insertNewUser() {
    bool success = false;
    String actionMessage = "function not implemented";

    return new ResponseItem(success, actionMessage);
  }
}