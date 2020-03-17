import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/categories_widgets/categories_edit.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/utilities/utilities.dart';

class CategoryRowGroup extends StatefulWidget {
  final Category category;
  final VoidCallback onSelect;
  final bool groupCategory;
  final bool selected;

  CategoryRowGroup(this.category, this.selected, this.groupCategory,
      {this.onSelect});

  @override
  _CategoryRowGroupState createState() => new _CategoryRowGroupState();
}

class _CategoryRowGroupState extends State<CategoryRowGroup> {
  int groupNum = 0;
  bool selectVal = false;

  @override
  void initState() {
    selectVal = widget.selected;
    if (widget.groupCategory) {
      // find the num of other groups this category is in if its not yours
      for (String groupId in Globals.user.groups.keys) {
        if (widget.category.groups.containsKey(groupId) &&
            groupId != Globals.currentGroup.groupId) {
          groupNum++;
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
          Checkbox(
            value: selectVal,
            onChanged: (bool value) {
              this.widget.onSelect();
              setState(() {
                selectVal = value;
              });
            },
          ),
          Expanded(
              child: AutoSizeText(
            widget.category.categoryName,
            maxLines: 1,
            style: TextStyle(fontSize: 20),
            minFontSize: 12,
            overflow: TextOverflow.ellipsis,
          )),
          Visibility(
            visible: widget.groupCategory,
            child: Expanded(
              child: AutoSizeText(
                (groupNum != 0) ? "Used in ($groupNum) other groups" : "",
                style: TextStyle(fontSize: 15),
                maxLines: 1,
                minFontSize: 8,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ),
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
                            editName: false,
                          )));
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
