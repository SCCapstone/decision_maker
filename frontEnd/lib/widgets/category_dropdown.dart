import 'package:flutter/material.dart';
import 'package:frontEnd/models/category.dart';
import 'category_row.dart';

class CategoryDropdown extends StatelessWidget {
  final List<Category> categories;
  final Function callback;
  final String dropdownTitle;

  CategoryDropdown(this.dropdownTitle, this.categories, {this.callback});

  @override
  Widget build(BuildContext context) {
    return ExpansionTile(
      title: Text(dropdownTitle),
      children: <Widget>[
        SizedBox(
          height: MediaQuery.of(context).size.height * .2,
          child: ListView.builder(
            shrinkWrap: true,
            itemCount: categories.length,
            itemBuilder: (context, index) {
              return CategoryRow(categories[index], index,
                  onSelect: () => callback(categories[index]));
            },
          ),
        ),
      ],
    );
  }
}
