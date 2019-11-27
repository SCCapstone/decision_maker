import 'package:flutter/material.dart';
import 'package:frontEnd/models/category.dart';
import 'category_row.dart';

class CategoryDropdown extends StatelessWidget {
  final List<Category> categoriesTotal;
  final List<Category> categoriesToAdd;
  final Function callback;
  final String dropdownTitle;

  CategoryDropdown(
      this.dropdownTitle, this.categoriesTotal, this.categoriesToAdd,
      {this.callback});

  @override
  Widget build(BuildContext context) {
    List<Widget> categoryRows = new List<Widget>();
    for (Category category in this.categoriesTotal) {
      categoryRows.add(CategoryRow(
          category, categoriesToAdd.contains(category),
          onSelect: () => callback(category)));
    }

    return ExpansionTile(title: Text(dropdownTitle), children: <Widget>[
      SizedBox(
          height: MediaQuery.of(context).size.height * .2,
          child: ListView(shrinkWrap: true, children: categoryRows))
    ]);
  }
}
