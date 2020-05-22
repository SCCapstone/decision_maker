import 'package:front_end_pocket_poll/imports/categories_manager.dart';

import 'group_category.dart';

class Category {
  final String categoryName;
  final String categoryId;
  final Map<String, int> choices; // choice label -> sort order
  final Map<String, String> groups; // groupId -> groupName
  final String owner;

  Category(
      {this.categoryId,
      this.categoryName,
      this.choices,
      this.groups,
      this.owner});

  Category.debug(this.categoryId, this.categoryName, this.choices, this.groups,
      this.owner);

  factory Category.fromJson(Map<String, dynamic> json) {
    Map<String, String> groupsMap = new Map<String, String>();
    Map<String, dynamic> groupsRaw = json[CategoriesManager.GROUPS];
    for (String groupId in groupsRaw.keys) {
      groupsMap.putIfAbsent(groupId, () => groupsRaw[groupId].toString());
    }

    Map<String, int> choicesMap = new Map<String, int>();
    Map<String, dynamic> choicesRaw = json[CategoriesManager.CHOICES];
    for (String choiceNum in choicesRaw.keys) {
      choicesMap.putIfAbsent(
          choiceNum, () => int.parse(choicesRaw[choiceNum].toString()));
    }

    return Category(
        categoryId: json[CategoriesManager.CATEGORY_ID],
        categoryName: json[CategoriesManager.CATEGORY_NAME],
        choices: choicesMap,
        groups: groupsMap,
        owner: json[CategoriesManager.OWNER]);
  }

  factory Category.fromJsonUser(Map<String, dynamic> json) {
    return Category(
        categoryId: json['CategoryId'], categoryName: json['CategoryName']);
  }

  GroupCategory asGroupCategory() {
    return new GroupCategory(
        categoryName: this.categoryName, owner: this.owner);
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }
    return other is Category && this.categoryId == other.categoryId;
  }

  @override
  int get hashCode {
    return categoryId.hashCode;
  }

  @override
  String toString() {
    return "CategoryId: $categoryId CategoryName: $categoryName Choices: "
        "$choices Groups: $groups Owner: $owner";
  }
}
