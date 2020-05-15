import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/categories_widgets/categories_edit.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';

class CategoryRowGroup extends StatefulWidget {
  final Category category;
  final VoidCallback onSelect;
  final bool selected;
  final Function updateOwnedCategories;
  final bool canSelect;
  final int index; // used for integration tests

  CategoryRowGroup(
      this.category, this.selected, this.updateOwnedCategories, this.canSelect,
      {this.onSelect, this.index});

  @override
  _CategoryRowGroupState createState() => new _CategoryRowGroupState();
}

class _CategoryRowGroupState extends State<CategoryRowGroup> {
  int groupNum;
  bool activeUserOwnsCategory;

  @override
  void initState() {
    this.groupNum = 0;
    this.activeUserOwnsCategory = false;
    for (Category category in Globals.user.ownedCategories) {
      if (category.categoryId == widget.category.categoryId) {
        this.activeUserOwnsCategory = true;
      }
    }
    if (!this.activeUserOwnsCategory) {
      // find the num of other groups this category is in if its not active user's category
      for (String groupId in Globals.user.groups.keys) {
        if (widget.category.groups.containsKey(groupId) &&
            groupId != Globals.currentGroup.groupId) {
          this.groupNum++;
        }
      }
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      key: Key("category_row_group:container:${widget.category.categoryId}"),
      height: MediaQuery.of(context).size.height * .07,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Visibility(
            visible: widget.canSelect,
            child: Checkbox(
              value: widget.selected,
              key: Key(
                  "category_row_group:checkbox:${this.activeUserOwnsCategory}:${widget.index}"),
              onChanged: (bool value) {
                selectCategory(value);
              },
            ),
          ),
          Visibility(
            visible: !widget.canSelect,
            child: Padding(
              padding: EdgeInsets.fromLTRB(20, 0, 0, 0),
            ),
          ),
          Expanded(
              child: GestureDetector(
                  onTap: () {
                    if (widget.canSelect) {
                      selectCategory(!widget.selected);
                    }
                  },
                  child: AutoSizeText(
                      (this.groupNum != 0)
                          ? "${widget.category.categoryName}\n(Used in ${this.groupNum} of your other groups)"
                          : widget.category.categoryName,
                      maxLines: 2,
                      style: TextStyle(fontSize: 20),
                      minFontSize: 12,
                      overflow: TextOverflow.ellipsis))),
          Padding(
            padding: EdgeInsets.all(MediaQuery.of(context).size.width * .01),
          ),
          IconButton(
            color: Colors.lightBlue,
            icon: Icon(Icons.edit),
            iconSize: MediaQuery.of(context).size.width * .075,
            tooltip: (this.activeUserOwnsCategory)
                ? "Edit Category"
                : "Edit Ratings",
            onPressed: () {
              Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => EditCategory(
                            category: widget.category,
                          ))).then((_) {
                // in case user copied a category, refresh the owned categories on back press
                widget.updateOwnedCategories();
              });
            },
          ),
          Padding(
            padding: EdgeInsets.all(MediaQuery.of(context).size.width * .015),
          )
        ],
      ),
      decoration: BoxDecoration(
          border: Border(bottom: BorderSide(color: getBorderColor()))),
    );
  }

  void selectCategory(bool value) {
    this.widget.onSelect();
  }
}
