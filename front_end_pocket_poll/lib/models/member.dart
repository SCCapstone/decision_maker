import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/favorite.dart';
import 'package:front_end_pocket_poll/models/user.dart';

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
        icon: json[UsersManager.ICON]);
  }

  factory Member.fromFavorite(Favorite favorite) {
    return Member(
        username: favorite.username,
        displayName: favorite.displayName,
        icon: favorite.icon);
  }

  factory Member.fromUser(User user) {
    return Member(
        username: user.username,
        displayName: user.displayName,
        icon: user.icon);
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
