import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/favorite.dart';

import 'app_settings.dart';

class User {
  final String username;
  String displayName;
  String icon;
  final AppSettings appSettings;
  final Map<String, dynamic> groups;
  final Map<String, dynamic> categories;
  List<Favorite> favorites;

  User(
      {this.username,
      this.displayName,
      this.appSettings,
      this.groups,
      this.categories,
      this.favorites});

  User.debug(this.username, this.displayName, this.appSettings, this.groups,
      this.categories, this.favorites);

  factory User.fromJson(Map<String, dynamic> json) {
    List<Favorite> favoriteList = new List<Favorite>();
    Map<String, dynamic> favoritesRaw = json[UsersManager.FAVORITES];
    for (String username in favoritesRaw.keys) {
      favoriteList.add(Favorite.fromJson(favoritesRaw[username], username));
    }

    return User(
        username: json[UsersManager.USERNAME],
        displayName: json[UsersManager.DISPLAY_NAME],
        appSettings: AppSettings.fromJson(json[UsersManager.APP_SETTINGS]),
        groups: json[UsersManager.GROUPS],
        categories: json[UsersManager.CATEGORIES],
        favorites: favoriteList);
  }

  @override
  String toString() {
    return "Username: $username DisplayName: $displayName AppSettings: $appSettings Groups: $groups "
        "Categories: $categories Favorites: $favorites Icon: $icon";
  }
}
