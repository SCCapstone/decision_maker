import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/user.dart';

class Favorite {
  final String username;
  final String icon;
  final String displayName;

  Favorite({this.username, this.displayName, this.icon});

  Favorite.debug(this.username, this.displayName, this.icon);

  factory Favorite.fromJson(Map<String, dynamic> json, String username) {
    return Favorite(
        username: username,
        displayName: json[UsersManager.DISPLAY_NAME],
        icon: json[UsersManager.ICON]);
  }

  factory Favorite.fromUser(final User user) {
    return Favorite(
        username: user.username,
        displayName: user.displayName,
        icon: user.icon);
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }
    return other is Favorite && this.username == other.username;
  }

  @override
  int get hashCode {
    return username.hashCode;
  }

  Map asMap() {
    return {
      UsersManager.USERNAME: username,
      UsersManager.DISPLAY_NAME: displayName,
      UsersManager.ICON: icon
    };
  }

  @override
  String toString() {
    return "Username: $username DisplayName: $displayName Icon: $icon";
  }
}
