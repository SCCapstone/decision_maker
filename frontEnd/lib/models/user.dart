import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/models/favorite.dart';
import 'package:frontEnd/models/group.dart';

import 'app_settings.dart';

class User {
  final String username;
  String displayName;
  String icon;
  final AppSettings appSettings;
  final Map<String, Group> groups;
  final Map<String, dynamic> userRatings;
  final Map<String, dynamic> groupsLeft;
  final Map<String, dynamic> categories;
  final List<Category> ownedCategories;
  List<Favorite> favorites;

  User(
      {this.username,
      this.displayName,
      this.appSettings,
      this.groups,
      this.userRatings,
      this.groupsLeft,
      this.categories,
      this.ownedCategories,
      this.favorites,
      this.icon});

  User.debug(
      this.username,
      this.displayName,
      this.appSettings,
      this.groups,
      this.userRatings,
      this.groupsLeft,
      this.categories,
      this.ownedCategories,
      this.favorites,
      this.icon);

  factory User.fromJson(Map<String, dynamic> json) {
    Map<String, Group> groupsMap = new Map<String, Group>();
    Map<String, dynamic> groupsRaw = json[UsersManager.GROUPS];
    for (String groupId in groupsRaw.keys) {
      Group group = new Group(
          groupId: groupId,
          groupName: groupsRaw[groupId][GroupsManager.GROUP_NAME],
          icon: groupsRaw[groupId][GroupsManager.ICON],
          lastActivity: groupsRaw[groupId][GroupsManager.LAST_ACTIVITY]);
      groupsMap.putIfAbsent(groupId, () => group);
    }

    List<Favorite> favoriteList = new List<Favorite>();
    Map<String, dynamic> favoritesRaw = json[UsersManager.FAVORITES];
    for (String username in favoritesRaw.keys) {
      favoriteList.add(Favorite.fromJson(favoritesRaw[username], username));
    }

    List<Category> categoryList = new List<Category>();
    Map<String, dynamic> categoriesRaw = json[UsersManager.OWNED_CATEGORIES];
    for (String catId in categoriesRaw.keys) {
      categoryList.add(Category(
          categoryId: catId,
          categoryName: categoriesRaw[catId].toString(),
          owner: json[UsersManager.USERNAME]));
    }

    return User(
        username: json[UsersManager.USERNAME],
        displayName: json[UsersManager.DISPLAY_NAME],
        appSettings: AppSettings.fromJson(json[UsersManager.APP_SETTINGS]),
        groups: groupsMap,
        userRatings: json[UsersManager.CATEGORIES],
        groupsLeft: json[UsersManager.GROUPS_LEFT],
        categories: json[UsersManager.CATEGORIES],
        ownedCategories: categoryList,
        favorites: favoriteList,
        icon: json[UsersManager.ICON]);
  }

  @override
  String toString() {
    return "Username: $username DisplayName: $displayName AppSettings: $appSettings Groups: $groups "
        " GroupsLeft: $groupsLeft UserRatings: $userRatings Categories $categories OwnedCategories: $ownedCategories "
        "Favorites: $favorites Icon: $icon";
  }
}
