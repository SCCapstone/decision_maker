import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/users_manager.dart';

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
        icon: json[GroupsManager.ICON]);
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

  @override
  String toString() {
    return "Username: $username DisplayName: $displayName Icon: $icon";
  }
}
