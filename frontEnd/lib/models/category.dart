class Category {
  final String categoryName;
  final int categoryId;
  final Map<String, dynamic> choices;
  final Map<String, dynamic> groups;
  final int nextChoiceNum;
  final String owner;

  Category(
      {this.categoryId,
      this.categoryName,
      this.choices,
      this.groups,
      this.nextChoiceNum,
      this.owner});

  Category.debug(this.categoryId, this.categoryName, this.choices, this.groups,
      this.nextChoiceNum, this.owner);

  factory Category.fromJson(Map<String, dynamic> json) {
    return Category(
        categoryId: json['CategoryId'],
        categoryName: json['CategoryName'],
        choices: json['Choices'],
        groups: json['Groups'],
        nextChoiceNum: json['NextChoiceNo'],
        owner: json["Owner"]);
  }

  @override
  String toString() {
    return "CategoryId: $categoryId CategoryName: $categoryName Choices: "
        "$choices Groups: $groups NextChoiceNum: $nextChoiceNum Owner: $owner";
  }
}
