class Category {
  final String categoryName;
  final int categoryId;
  final Map<int, String> choices;
  final Map<int, String> groups;
  final int nextChoiceNum;
  final int ownerId;

  Category(
      {this.categoryId,
      this.categoryName,
      this.choices,
      this.groups,
      this.nextChoiceNum,
      this.ownerId});

  Category.debug(this.categoryId, this.categoryName, this.choices, this.groups,
      this.nextChoiceNum, this.ownerId);

  factory Category.fromJson(Map<String, dynamic> json) {
    return Category(
        categoryId: json['categoryId'],
        categoryName: json['categoryName'],
        choices: json['choices'],
        groups: json['groups'],
        nextChoiceNum: json['nextChoiceNum'],
        ownerId: json['ownerId']);
  }
}
