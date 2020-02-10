import 'package:flutter/material.dart';
import 'package:frontEnd/categories_widgets/categories_edit.dart';
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
    if (!isOwner) {
      return Container(
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: <Widget>[
            Text(
              category.categoryName,
              style: TextStyle(fontSize: 20),
            ),
            RaisedButton(
              color: Colors.blueAccent,
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
                );
              },
            )
          ],
        ),
        decoration: BoxDecoration(border: Border(bottom: BorderSide())),
      );
    } else {
      return Container(
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: <Widget>[
            Flexible(
              child: Text(
                category.categoryName,
                style: TextStyle(
                    fontSize:
                        DefaultTextStyle.of(context).style.fontSize * 1.5),
              ),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                RaisedButton(
                  color: Colors.blueAccent,
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
                      EdgeInsets.all(MediaQuery.of(context).size.height * .007),
                ),
                RaisedButton(
                  color: Colors.redAccent,
                  child: Text(
                    "Delete",
                    style: TextStyle(),
                  ),
                  onPressed: () {
                    confirmDelete(
                        context); // this deletes it from the local list
                  },
                ),
              ],
            ),
          ],
        ),
        decoration: BoxDecoration(border: Border(bottom: BorderSide())),
      );
    }
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
