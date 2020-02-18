import 'package:flutter/material.dart';
import 'package:frontEnd/categories_widgets/categories_edit.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/category.dart';

class CategoriesListItem extends StatelessWidget {
  final Category category;
  final VoidCallback onDelete;
  final VoidCallback afterEditCallback;
  final int index;
  final bool isOwner;

  CategoriesListItem(this.category, this.index, this.isOwner,
      {this.onDelete, this.afterEditCallback});

  @override
  Widget build(BuildContext context) {
    return Container(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Expanded(
            child: Text(
              category.categoryName,
              style: TextStyle(
                  fontSize: DefaultTextStyle.of(context).style.fontSize * 1.5),
            ),
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: <Widget>[
              RaisedButton(
                color: Colors.lightBlue,
                child: Text(
                  "Edit",
                  style: TextStyle(),
                ),
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (context) =>
                            EditCategory(category: this.category)),
                  ).then((_) => this.afterEditCallback());
                },
              ),
              Padding(
                padding:
                    EdgeInsets.all(MediaQuery.of(context).size.height * .015),
                child: Visibility(
                  visible: isOwner,
                  child: RaisedButton(
                    color: Colors.red,
                    child: Text(
                      "Delete",
                      style: TextStyle(),
                    ),
                    onPressed: () {
                      confirmDelete(context);
                    },
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
      decoration: BoxDecoration(
          border: Border(
              bottom: BorderSide(
                  color: (Globals.user.appSettings.darkTheme)
                      ? Colors.white
                      : Colors.black))),
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
                "\"${category.categoryName}\"?"),
          );
        });
  }
}
