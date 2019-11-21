import 'package:flutter/material.dart';
import 'package:frontEnd/models/category.dart';

class CategoryRow extends StatefulWidget {
  final Category category;
  final int index;
  final VoidCallback onSelect;
  bool selected;

  CategoryRow(this.category, this.index, this.selected, {this.onSelect});

  @override
  _CategoryRow createState() => new _CategoryRow();
}

class _CategoryRow extends State<CategoryRow> {

  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * .07,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Expanded(
            child: Text(
              widget.category.categoryName,
              style: TextStyle(fontSize: 20),
            ),
          ),
          Checkbox(
            value: widget.selected,
            onChanged: (bool value){
              this.widget.onSelect();
              setState(() {
                widget.selected = value;
              });
            },
          ),
        ],
      ),
      decoration:
      new BoxDecoration(border: new Border(bottom: new BorderSide())),
    );
  }
}