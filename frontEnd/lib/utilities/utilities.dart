import 'package:flutter/material.dart';
import 'package:frontEnd/models/category.dart';

void showPopupMessage(String message, BuildContext context) {
  showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text("Response status:"),
          content: Text(message),
        );
      });
}

class CategoryRow extends StatelessWidget {
  final Category category;
  final int index;
  final VoidCallback onSelect;

  CategoryRow(this.category, this.index, {this.onSelect});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * .07,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Expanded(
            child: Text(
              category.categoryName,
              style: TextStyle(fontSize: 20),
            ),
          ),
          RaisedButton(
            child: Text("Hey"),
            onPressed: this.onSelect,
          )
        ],
      ),
      decoration:
          new BoxDecoration(border: new Border(bottom: new BorderSide())),
    );
  }
}

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
