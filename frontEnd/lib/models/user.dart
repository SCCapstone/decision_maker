import 'package:frontEnd/imports/users_manager.dart';

class User {
  final String username;
  final String firstName;
  final String lastName;
  final bool darkTheme;
  final bool muted;
  final Map<String, dynamic> groups;
  final Map<String, dynamic> categories;

  User(
      {this.username,
      this.firstName,
      this.lastName,
      this.darkTheme,
      this.muted,
      this.groups,
      this.categories});

  User.debug(this.username, this.firstName, this.lastName, this.darkTheme,
      this.muted, this.groups, this.categories);

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
        username: json[UsersManager.USERNAME],
        firstName: json[UsersManager.FIRST_NAME],
        lastName: json[UsersManager.LAST_NAME],
        darkTheme: json[UsersManager.APP_SETTING_DARK_THEME],
        muted: json[UsersManager.APP_SETTING_MUTED],
        groups: json[UsersManager.GROUPS],
        categories: json[UsersManager.CATEGORIES]);
  }

  @override
  String toString() {
    return "Username: $username FirstName: $firstName LastName: "
        "$lastName DarkTheme: $darkTheme Muted: $muted Groups: $groups "
        "Categories: $categories";
  }
}
