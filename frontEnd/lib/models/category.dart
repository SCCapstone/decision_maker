class Category {
  final String categoryName;
  final String categoryId;
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
        nextChoiceNum: int.parse(json['NextChoiceNo']),
        owner: json["Owner"]);
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }
    return other is Category && this.categoryId == other.categoryId;
  }

  @override
  String toString() {
    return "CategoryId: $categoryId CategoryName: $categoryName Choices: "
        "$choices Groups: $groups NextChoiceNum: $nextChoiceNum Owner: $owner";
  }
}
