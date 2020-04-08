import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/categories_widgets/categories_edit.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';

class CategoriesListItem extends StatelessWidget {
  final Category category;
  final VoidCallback deleteCategory;
  final VoidCallback refreshCategoryList;
  final int index; // used for having a unique key for each row for tests

  CategoriesListItem(this.category,
      {this.deleteCategory, this.refreshCategoryList, this.index});

  @override
  Widget build(BuildContext context) {
    return Container(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Expanded(
              child: AutoSizeText(
            this.category.categoryName,
            maxLines: 1,
            style: TextStyle(fontSize: 24),
            minFontSize: 12,
            overflow: TextOverflow.ellipsis,
          )),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: <Widget>[
              IconButton(
                color: Colors.lightBlue,
                icon: Icon(Icons.edit),
                iconSize: MediaQuery.of(context).size.width * .075,
                tooltip: "Edit Category",
                key: Key(
                    "categories_list_item:category_edit_button:${this.index}"),
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (context) => EditCategory(
                              category: this.category,
                            )),
                  ).then((_) => this.refreshCategoryList());
                },
              ),
              Padding(
                padding:
                    EdgeInsets.all(MediaQuery.of(context).size.height * .015),
                child: IconButton(
                  color: Colors.red,
                  icon: Icon(Icons.delete),
                  iconSize: MediaQuery.of(context).size.width * .075,
                  tooltip: "Delete Category",
                  key: Key(
                      "categories_list_item:category_delete_button:${this.index}"),
                  onPressed: () {
                    confirmDelete(context);
                  },
                ),
              ),
            ],
          ),
        ],
      ),
      decoration: BoxDecoration(
          border: Border(bottom: BorderSide(color: getBorderColor()))),
    );
  }

  void confirmDelete(BuildContext context) {
    showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: Text("Delete"),
            actions: <Widget>[
              FlatButton(
                child: Text("Yes"),
                key: Key(
                    "categories_list_item:category_edit_button_confirm:${this.index}"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                  this.deleteCategory();
                },
              ),
              FlatButton(
                child: Text("No"),
                key: Key(
                    "categories_list_item:category_edit_button_denial:${this.index}"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                },
              )
            ],
            content: Text("Are you sure you wish to delete the category "
                "\"${category.categoryName}\"?\n\n"
                "This will remove it from all groups it is apart of."),
          );
        });
  }
}
