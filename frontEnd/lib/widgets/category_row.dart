import 'package:flutter/material.dart';
import 'package:frontEnd/models/category.dart';

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