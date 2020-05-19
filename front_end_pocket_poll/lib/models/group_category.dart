import 'package:front_end_pocket_poll/imports/categories_manager.dart';

import 'category.dart';

class GroupCategory {
  final String categoryName;
  final String owner;

  GroupCategory({this.categoryName, this.owner});

  GroupCategory.debug(this.categoryName, this.owner);

  factory GroupCategory.fromJson(final Map<String, dynamic> json) {
    return GroupCategory(
        categoryName: json[CategoriesManager.CATEGORY_NAME],
        owner: json[CategoriesManager.OWNER]);
  }

  factory GroupCategory.fromCategory(final Category category) {
    return GroupCategory(
      categoryName: category.categoryName,
      owner: category.owner
    );
  }

  Map asMap() {
    return {
      CategoriesManager.CATEGORY_NAME: this.categoryName,
      CategoriesManager.OWNER: this.owner
    };
  }

  @override
  String toString() {
    return "CategoryName: $categoryName Owner: $owner";
  }
}
