import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/favorite.dart';

class Member {
  final String username;
  final String icon;
  final String displayName;

  Member({this.username, this.displayName, this.icon});

  Member.debug(this.username, this.displayName, this.icon);

  factory Member.fromJson(Map<String, dynamic> json, String username) {
    return Member(
        username: username,
        displayName: json[UsersManager.DISPLAY_NAME],
        icon: json[GroupsManager.ICON]);
  }

  factory Member.fromFavorite(Favorite favorite) {
    return Member(
        username: favorite.username,
        displayName: favorite.displayName,
        icon: favorite.icon);
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }
    return other is Member && this.username == other.username;
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
