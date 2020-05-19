import 'package:front_end_pocket_poll/imports/categories_manager.dart';

import 'group_category.dart';

class Category {
  final String categoryName;
  final String categoryId;
  final Map<String, String> choices;
  final Map<String, String> groups; // groupId -> groupName
  final int nextChoiceNum;
  final int categoryVersion;
  final String owner;

  Category(
      {this.categoryId,
      this.categoryName,
      this.choices,
      this.groups,
      this.nextChoiceNum,
      this.categoryVersion,
      this.owner});

  Category.debug(this.categoryId, this.categoryName, this.choices, this.groups,
      this.nextChoiceNum, this.categoryVersion, this.owner);

  factory Category.fromJson(Map<String, dynamic> json) {
    Map<String, String> groupsMap = new Map<String, String>();
    Map<String, dynamic> groupsRaw = json[CategoriesManager.GROUPS];
    for (String groupId in groupsRaw.keys) {
      groupsMap.putIfAbsent(groupId, () => groupsRaw[groupId].toString());
    }

    Map<String, String> choicesMap = new Map<String, String>();
    Map<String, dynamic> choicesRaw = json[CategoriesManager.CHOICES];
    for (String choiceNum in choicesRaw.keys) {
      choicesMap.putIfAbsent(choiceNum, () => choicesRaw[choiceNum].toString());
    }

    return Category(
        categoryId: json[CategoriesManager.CATEGORY_ID],
        categoryName: json[CategoriesManager.CATEGORY_NAME],
        choices: choicesMap,
        groups: groupsMap,
        nextChoiceNum: json[CategoriesManager.NEXT_CHOICE_NO],
        categoryVersion: json[CategoriesManager.CATEGORY_VERSION],
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
        "$choices Groups: $groups NextChoiceNum: $nextChoiceNum Owner: $owner CategoryVersion: $categoryVersion";
  }
}
