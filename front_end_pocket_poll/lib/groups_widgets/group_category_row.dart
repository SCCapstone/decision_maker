import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/categories_widgets/category_edit.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/models/category_rating_tuple.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';

class GroupCategoryRow extends StatefulWidget {
  final CategoryRatingTuple categoryRatingTuple;
  final Category category;
  final VoidCallback onSelect;
  final bool selected;
  final Function updateOwnedCategories;
  final bool canSelect;
  final int index; // used for integration tests

  GroupCategoryRow(this.categoryRatingTuple, this.category, this.selected,
      this.updateOwnedCategories, this.canSelect,
      {this.onSelect, this.index});

  @override
  _GroupCategoryRowState createState() => new _GroupCategoryRowState();
}

class _GroupCategoryRowState extends State<GroupCategoryRow> {
  int groupNum;
  bool activeUserOwnsCategory;

  @override
  void initState() {
    this.groupNum = 0;
    this.activeUserOwnsCategory = false;
    if (widget.category == null && widget.categoryRatingTuple != null) {
      // means a tuple was passed in so the user doesn't own the category
      this.activeUserOwnsCategory = false;
    } else if (widget.categoryRatingTuple == null && widget.category != null) {
      this.activeUserOwnsCategory = true;
    }

    if (!this.activeUserOwnsCategory) {
      // find the num of other groups this category is in if its not active user's category
      for (String groupId in Globals.user.groups.keys) {
        if (widget.categoryRatingTuple.category.groups.containsKey(groupId) &&
            groupId != Globals.currentGroupResponse.group.groupId) {
          this.groupNum++;
        }
      }
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      key: Key("group_category_row:container:${widget.category.categoryId}"),
      height: MediaQuery.of(context).size.height * .07,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Visibility(
            visible: widget.canSelect,
            child: Checkbox(
              value: widget.selected,
              key: Key(
                  "group_category_row:checkbox:${this.activeUserOwnsCategory}:${widget.selected}:${widget.index}"),
              onChanged: (bool value) {
                selectCategory();
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
                      selectCategory();
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
                      builder: (context) => CategoryEdit(
                            categoryRatingTuple: widget.categoryRatingTuple,
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

  void selectCategory() {
    this.widget.onSelect();
  }
}
