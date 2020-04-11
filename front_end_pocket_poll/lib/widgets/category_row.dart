import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';

class CategoryRow extends StatefulWidget {
  final Category category;
  final VoidCallback onSelect;
  final bool selected;

  CategoryRow(this.category, this.selected, {this.onSelect});

  @override
  _CategoryRow createState() => new _CategoryRow();
}

class _CategoryRow extends State<CategoryRow> {
  String categoryText;
  bool selected;

  @override
  void initState() {
    this.selected = widget.selected;
    // only show owner username in row if the user doesn't own it
    bool isActiveUserCategoryOwner = widget.category.owner == Globals.username;
    if (isActiveUserCategoryOwner) {
      this.categoryText = widget.category.categoryName;
    } else {
      this.categoryText =
          "${widget.category.categoryName} \n(@${widget.category.owner})";
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
            value: this.selected,
            onChanged: (bool value) {
              this.widget.onSelect();
              setState(() {
                this.selected = value;
              });
            },
          ),
          Expanded(
            child: AutoSizeText(
              this.categoryText,
              minFontSize: 12,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
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
