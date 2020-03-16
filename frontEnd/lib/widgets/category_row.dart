import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/utilities/utilities.dart';

class CategoryRow extends StatefulWidget {
  final Category category;
  final VoidCallback onSelect;
  bool selected;

  CategoryRow(this.category, this.selected, {this.onSelect});

  @override
  _CategoryRow createState() => new _CategoryRow();
}

class _CategoryRow extends State<CategoryRow> {
  String categoryText;

  @override
  void initState() {
    // only show owner in row if the user doesn't own it
    bool isActiveUserCategoryOwner = widget.category.owner == Globals.username;
    if (isActiveUserCategoryOwner) {
      this.categoryText = widget.category.categoryName;
    } else {
      this.categoryText =
          "${widget.category.categoryName} (@${widget.category.owner})";
    }

    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * .07,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Checkbox(
            value: widget.selected,
            onChanged: (bool value) {
              this.widget.onSelect();
              setState(() {
                widget.selected = value;
              });
            },
          ),
          Expanded(
            child: Text(
              this.categoryText,
              style: TextStyle(fontSize: 20),
            ),
          ),
        ],
      ),
      decoration: BoxDecoration(
          border: Border(bottom: BorderSide(color: getBorderColor()))),
    );
  }
}
