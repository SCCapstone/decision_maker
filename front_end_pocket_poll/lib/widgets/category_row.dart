import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';

class CategoryRow extends StatefulWidget {
  final Category category;
  final VoidCallback onSelect;
  final bool selected;
  final int index; // used for integration tests

  CategoryRow(this.category, this.selected,
      {@required this.onSelect, this.index});

  @override
  _CategoryRow createState() => new _CategoryRow();
}

class _CategoryRow extends State<CategoryRow> {
  String categoryText;

  @override
  void initState() {
    // only show owner username in row if the user doesn't own it
    bool isActiveUserCategoryOwner = widget.category.owner == Globals.user.username;
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
      key: Key("category_row:container:${widget.category.categoryId}"),
      height: MediaQuery.of(context).size.height * .07,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Checkbox(
            value: widget.selected,
            key: Key("category_row:checkbox:${widget.index}"),
            onChanged: (bool value) {
              selectCategory();
            },
          ),
          Expanded(
            child: GestureDetector(
              onTap: () {
                selectCategory();
              },
              child: AutoSizeText(
                this.categoryText,
                minFontSize: 12,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(fontSize: 20),
              ),
            ),
          ),
        ],
      ),
      decoration: BoxDecoration(
          border: Border(bottom: BorderSide(color: getBorderColor()))),
    );
  }

  void selectCategory() {
    this.widget.onSelect();
  }
}
