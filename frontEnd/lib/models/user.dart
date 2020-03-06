import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/models/favorite.dart';

import 'app_settings.dart';

class User {
  final String username;
  String displayName;
  String icon;
  final AppSettings appSettings;
  final Map<String, dynamic> groups;
  final Map<String, dynamic> categories;
  final List<Category> ownedCategories;
  List<Favorite> favorites;

  User(
      {this.username,
      this.displayName,
      this.appSettings,
      this.groups,
      this.categories,
      this.ownedCategories,
      this.favorites,
      this.icon});

  User.debug(this.username, this.displayName, this.appSettings, this.groups,
      this.categories, this.ownedCategories, this.favorites, this.icon);

  factory User.fromJson(Map<String, dynamic> json) {
    List<Favorite> favoriteList = new List<Favorite>();
    Map<String, dynamic> favoritesRaw = json[UsersManager.FAVORITES];
    for (String username in favoritesRaw.keys) {
      favoriteList.add(Favorite.fromJson(favoritesRaw[username], username));
    }

    List<Category> categoryList = new List<Category>();
    Map<String, dynamic> categoriesRaw = json[UsersManager.OWNED_CATEGORIES];
    for (String catId in categoriesRaw.keys) {
      categoryList.add(Category.debug(
          catId, categoriesRaw[catId].toString(), null, null, null, null));
    }

    return User(
        username: json[UsersManager.USERNAME],
        displayName: json[UsersManager.DISPLAY_NAME],
        appSettings: AppSettings.fromJson(json[UsersManager.APP_SETTINGS]),
        groups: json[UsersManager.GROUPS],
        categories: json[UsersManager.CATEGORIES],
        ownedCategories: categoryList,
        favorites: favoriteList,
        icon: json[UsersManager.ICON]);
  }

  @override
  String toString() {
    return "Username: $username DisplayName: $displayName AppSettings: $appSettings Groups: $groups "
        "Categories: $categories OwnedCategories: $ownedCategories Favorites: $favorites Icon: $icon";
  }
}
