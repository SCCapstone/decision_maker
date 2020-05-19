import 'dart:core';

import 'package:front_end_pocket_poll/models/category.dart';

class CategoryRatingTuple {
  static final String CATEGORY = "Category";
  static final String RATINGS = "Ratings";

  final Category category;
  final Map<String, String> ratings;

  CategoryRatingTuple({this.category, this.ratings});

  CategoryRatingTuple.debug(this.category, this.ratings);

  factory CategoryRatingTuple.fromJson(Map<String, dynamic> json) {
    Map<String, dynamic> ratingsRaw = json[RATINGS];
    Map<String, String> ratingsMap = new Map();
    for (String choiceId in ratingsRaw.keys) {
      ratingsMap.putIfAbsent(choiceId, () => ratingsRaw[choiceId].toString());
    }
    return CategoryRatingTuple(
        category: new Category.fromJson(json[CATEGORY]), ratings: ratingsMap);
  }
}
