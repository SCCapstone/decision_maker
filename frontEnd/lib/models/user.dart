import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/models/favorite.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/models/user_group.dart';

import 'app_settings.dart';

class User {
  final String username;
  String displayName;
  String icon;
  final AppSettings appSettings;
  final Map<String, UserGroup> groups;
  final Map<String, Map<String, String>> userRatings;
  final Map<String, Group> groupsLeft;
  final List<Category> ownedCategories;
  List<Favorite> favorites;

  User(
      {this.username,
      this.displayName,
      this.appSettings,
      this.groups,
      this.userRatings,
      this.groupsLeft,
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
      this.ownedCategories,
      this.favorites,
      this.icon);

  factory User.fromJson(Map<String, dynamic> json) {
    Map<String, UserGroup> groupsMap = new Map<String, UserGroup>();
    Map<String, dynamic> groupsRaw = json[UsersManager.GROUPS];
    for (String groupId in groupsRaw.keys) {
      UserGroup userGroup =
          new UserGroup.fromJson(json[UsersManager.GROUPS][groupId], groupId);
      groupsMap.putIfAbsent(groupId, () => userGroup);
    }

    Map<String, Group> groupsLeftMap = new Map<String, Group>();
    Map<String, dynamic> groupsLeftRaw = json[UsersManager.GROUPS_LEFT];
    for (String groupId in groupsLeftRaw.keys) {
      Group group = new Group(
          groupId: groupId,
          groupName: groupsLeftRaw[groupId][GroupsManager.GROUP_NAME],
          icon: groupsLeftRaw[groupId][GroupsManager.ICON]);
      groupsLeftMap.putIfAbsent(groupId, () => group);
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

    Map<String, Map<String, String>> userRatings =
        new Map<String, Map<String, String>>();
    Map<String, dynamic> userRatingsRaw = json[UsersManager.USER_RATINGS];
    if (userRatingsRaw != null) {
      for (String categoryId in userRatingsRaw.keys) {
        Map<String, String> categoryRatings = new Map<String, String>();
        for (String choiceId in userRatingsRaw[categoryId].keys) {
          categoryRatings.putIfAbsent(
              choiceId, () => userRatingsRaw[categoryId][choiceId].toString());
        }
        userRatings.putIfAbsent(categoryId, () => categoryRatings);
      }
    }

    return User(
        username: json[UsersManager.USERNAME],
        displayName: json[UsersManager.DISPLAY_NAME],
        appSettings: AppSettings.fromJson(json[UsersManager.APP_SETTINGS]),
        groups: groupsMap,
        userRatings: userRatings,
        groupsLeft: groupsLeftMap,
        ownedCategories: categoryList,
        favorites: favoriteList,
        icon: json[UsersManager.ICON]);
  }

  @override
  String toString() {
    return "Username: $username DisplayName: $displayName AppSettings: $appSettings Groups: $groups "
        " GroupsLeft: $groupsLeft UserRatings: $userRatings OwnedCategories: $ownedCategories "
        "Favorites: $favorites Icon: $icon";
  }
}
