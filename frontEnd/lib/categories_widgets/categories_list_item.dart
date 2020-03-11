import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/categories_widgets/categories_edit.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/utilities/utilities.dart';

class CategoriesListItem extends StatelessWidget {
  final Category category;
  final VoidCallback onDelete;
  final VoidCallback afterEditCallback;
  final int index;

  CategoriesListItem(this.category, this.index,
      {this.onDelete, this.afterEditCallback});

  @override
  Widget build(BuildContext context) {
    return Container(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Expanded(
              child: AutoSizeText(
            category.categoryName,
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
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (context) => EditCategory(
                              category: this.category,
                              editName: true,
                            )),
                  ).then((_) => this.afterEditCallback());
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
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                  this.onDelete();
                },
              ),
              FlatButton(
                child: Text("No"),
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
