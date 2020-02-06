import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/favorite.dart';

import 'app_settings.dart';

class User {
  final String username;
  String displayName;
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
    try {
      // TODO remove this once the backend adds this field to the object
      Map<String, dynamic> favoritesRaw = json[UsersManager.FAVORITES];
      for (String username in favoritesRaw.keys) {
        String icon = favoritesRaw[GroupsManager.ICON];
        String displayName = favoritesRaw[UsersManager.DISPLAY_NAME];
        Favorite favorite = new Favorite(
            username: username, icon: icon, displayName: displayName);
        favoriteList.add(favorite);
      }
    } catch (e) {
      print(e);
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
        "Categories: $categories Favorites: $favorites";
  }
}
