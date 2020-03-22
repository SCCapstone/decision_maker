import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/models/favorite.dart';
import 'package:frontEnd/models/user_group.dart';

import 'app_settings.dart';
import 'group_left.dart';

class User {
  final String username;
  String displayName;
  String icon;
  final AppSettings appSettings;
  final Map<String, UserGroup> groups;
  final Map<String, Map<String, String>> userRatings;
  final Map<String, GroupLeft> groupsLeft;
  final List<Category> ownedCategories;
  List<Favorite> favorites;
  final bool firstLogin;

  User(
      {this.username,
      this.displayName,
      this.appSettings,
      this.groups,
      this.userRatings,
      this.groupsLeft,
      this.ownedCategories,
      this.favorites,
      this.icon,
      this.firstLogin});

  User.debug(
      this.username,
      this.displayName,
      this.appSettings,
      this.groups,
      this.userRatings,
      this.groupsLeft,
      this.ownedCategories,
      this.favorites,
      this.icon,
      this.firstLogin);

  factory User.fromJson(Map<String, dynamic> json) {
    Map<String, UserGroup> groupsMap = new Map<String, UserGroup>();
    Map<String, dynamic> groupsRaw = json[UsersManager.GROUPS];
    for (String groupId in groupsRaw.keys) {
      UserGroup userGroup =
          new UserGroup.fromJson(json[UsersManager.GROUPS][groupId], groupId);
      groupsMap.putIfAbsent(groupId, () => userGroup);
    }

    Map<String, GroupLeft> groupsLeftMap = new Map<String, GroupLeft>();
    Map<String, dynamic> groupsLeftRaw = json[UsersManager.GROUPS_LEFT];
    for (String groupId in groupsLeftRaw.keys) {
      groupsLeftMap.putIfAbsent(groupId,
          () => new GroupLeft.fromJson(groupsLeftRaw[groupId], groupId));
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
    bool firstLogin = false;
    if (json.containsKey(UsersManager.FIRST_LOGIN)) {
      firstLogin = json[UsersManager.FIRST_LOGIN];
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
        icon: json[UsersManager.ICON],
        firstLogin: firstLogin);
  }

  Map asMap() {
    // need this for encoding to work properly
    Map<String, dynamic> groupsMap = new Map<String, dynamic>();
    for (String groupId in this.groups.keys) {
      groupsMap.putIfAbsent(groupId, () => this.groups[groupId].asMap());
    }
    Map<String, dynamic> groupsLeftMap = new Map<String, dynamic>();
    for (String groupId in this.groupsLeft.keys) {
      groupsLeftMap.putIfAbsent(
          groupId, () => this.groupsLeft[groupId].asMap());
    }

    return {
      UsersManager.USERNAME: username,
      UsersManager.DISPLAY_NAME: displayName,
      UsersManager.APP_SETTINGS: appSettings.asMap(),
      UsersManager.GROUPS: groupsMap,
      UsersManager.GROUPS_LEFT: groupsLeftMap,
      UsersManager.USER_RATINGS: userRatings,
      UsersManager.OWNED_CATEGORIES: ownedCategories,
      UsersManager.FAVORITES: favorites,
      UsersManager.ICON: icon
    };
  }

  @override
  String toString() {
    return "Username: $username DisplayName: $displayName AppSettings: $appSettings Groups: $groups "
        " GroupsLeft: $groupsLeft UserRatings: $userRatings OwnedCategories: $ownedCategories "
        "Favorites: $favorites Icon: $icon FirstLogin: $firstLogin";
  }
}
