import 'package:frontEnd/imports/users_manager.dart';

import 'app_settings.dart';

class User {
  final String username;
  String displayName;
  final AppSettings appSettings;
  final Map<String, dynamic> groups;
  final Map<String, dynamic> categories;

  User(
      {this.username,
      this.displayName,
      this.appSettings,
      this.groups,
      this.categories});

  User.debug(this.username, this.displayName, this.appSettings, this.groups,
      this.categories);

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
        username: json[UsersManager.USERNAME],
        displayName: json[UsersManager.DISPLAY_NAME],
        appSettings: AppSettings.fromJson(json[UsersManager.APP_SETTINGS]),
        groups: json[UsersManager.GROUPS],
        categories: json[UsersManager.CATEGORIES]);
  }

  @override
  String toString() {
    return "Username: $username DisplayName: $displayName AppSettings: $appSettings Groups: $groups "
        "Categories: $categories";
  }
}
