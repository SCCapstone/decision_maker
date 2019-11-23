import 'package:flutter/material.dart';
import 'package:frontEnd/models/category.dart';
import 'category_row.dart';

class CategoryDropdown extends StatelessWidget {
  final List<Category> categoriesTotal;
  final List<Category> categoriesToAdd;
  final Function callback;
  final String dropdownTitle;
  bool checked;

  CategoryDropdown(this.dropdownTitle, this.categoriesTotal, this.categoriesToAdd, {this.callback});

  @override
  Widget build(BuildContext context) {
    return ExpansionTile(
      title: Text(dropdownTitle),
      children: <Widget>[
        SizedBox(
          height: MediaQuery.of(context).size.height * .2,
          child: ListView.builder(
            shrinkWrap: true,
            itemCount: categoriesTotal.length,
            itemBuilder: (context, index) {
              if(categoriesToAdd.contains(categoriesTotal[index])){
                return CategoryRow(categoriesTotal[index], index, true,
                    onSelect: () => callback(categoriesTotal[index]));
              }
              else{
                // user hasn't selected it, so don't check the checkbox
                return CategoryRow(categoriesTotal[index], index, false,
                    onSelect: () => callback(categoriesTotal[index]));
              }
            },
          ),
        ),
      ],
    );
  }
}
