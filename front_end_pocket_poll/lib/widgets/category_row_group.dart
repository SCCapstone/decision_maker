import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/categories_widgets/categories_edit.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';

class CategoryRowGroup extends StatefulWidget {
  final Category category;
  final VoidCallback onSelect;
  final bool groupCategory;
  final bool selected;
  final Function updateOwnedCategories;
  final bool canSelect;
  final int index; // used for integration tests

  CategoryRowGroup(this.category, this.selected, this.groupCategory,
      this.updateOwnedCategories, this.canSelect,
      {this.onSelect, this.index});

  @override
  _CategoryRowGroupState createState() => new _CategoryRowGroupState();
}

class _CategoryRowGroupState extends State<CategoryRowGroup> {
  int groupNum;
  bool selectVal;

  @override
  void initState() {
    this.groupNum = 0;
    this.selectVal = widget.selected;
    if (widget.groupCategory) {
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
      height: MediaQuery.of(context).size.height * .07,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Visibility(
            visible: widget.canSelect,
            child: Checkbox(
              value: this.selectVal,
              key: Key(
                  "category_row_group:checkbox:${widget.groupCategory}:${widget.index}"),
              onChanged: (bool value) {
                this.widget.onSelect();
                setState(() {
                  this.selectVal = value;
                });
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
              child: AutoSizeText(
                  (this.groupNum != 0)
                      ? "${widget.category.categoryName}\n(Used in ${this.groupNum} of your other groups)"
                      : widget.category.categoryName,
                  maxLines: 2,
                  style: TextStyle(fontSize: 20),
                  minFontSize: 12,
                  overflow: TextOverflow.ellipsis)),
          Padding(
            padding: EdgeInsets.all(MediaQuery.of(context).size.width * .01),
          ),
          IconButton(
            color: Colors.lightBlue,
            icon: Icon(Icons.edit),
            iconSize: MediaQuery.of(context).size.width * .075,
            tooltip: (widget.groupCategory) ? "Edit Ratings" : "Edit Category",
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
}
