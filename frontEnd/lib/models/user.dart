import 'package:frontEnd/imports/users_manager.dart';

import 'app_settings.dart';

class User {
  final String username;
  String firstName;
  String lastName;
  final AppSettings appSettings;
  final Map<String, dynamic> groups;
  final Map<String, dynamic> categories;

  User(
      {this.username,
       this.firstName,
       this.lastName,
       this.appSettings,
       this.groups,
       this.categories});

  User.debug(this.username, this.firstName, this.lastName, this.appSettings, this.groups, this.categories);

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
        username: json[UsersManager.USERNAME],
        firstName: json[UsersManager.FIRST_NAME],
        lastName: json[UsersManager.LAST_NAME],
        appSettings: AppSettings.fromJson(json[UsersManager.APP_SETTINGS]),
        groups: json[UsersManager.GROUPS],
        categories: json[UsersManager.CATEGORIES]);
  }

  @override
  String toString() {
    return "Username: $username FirstName: $firstName LastName: "
        "$lastName AppSettings: $appSettings Groups: $groups "
        "Categories: $categories";
  }
}
