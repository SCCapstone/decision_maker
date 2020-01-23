import 'package:flutter/material.dart';
import 'package:frontEnd/models/category.dart';
import 'categories_create_or_edit.dart';

class CategoriesListItem extends StatelessWidget {
  final Category category;
  final VoidCallback onDelete;
  final VoidCallback afterEditCallback;
  final int index;
  final bool defaultCategory;

  CategoriesListItem(this.category, this.index, this.defaultCategory,
      {this.onDelete, this.afterEditCallback});

  @override
  Widget build(BuildContext context) {
    if (defaultCategory) {
      return Container(
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: <Widget>[
            Text(
              category.categoryName,
              style: TextStyle(fontSize: 20),
            ),
            RaisedButton(
              child: Text(
                "Edit",
                style: TextStyle(),
              ),
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => CreateOrEditCategory(
                          isEdit: true, category: this.category)),
                );
              },
            )
          ],
        ),
        decoration:
            new BoxDecoration(border: new Border(bottom: new BorderSide())),
      );
    } else {
      return Container(
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: <Widget>[
            Flexible(
              child: Text(
                category.categoryName,
                style: TextStyle(fontSize: 20),
              ),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                RaisedButton(
                  child: Text(
                    "Edit",
                    style: TextStyle(),
                  ),
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                          builder: (context) => CreateOrEditCategory(
                              isEdit: true, category: this.category)),
                    ).then((_) => this.afterEditCallback());
                  },
                ),
                Padding(
                  padding:
                      EdgeInsets.all(MediaQuery.of(context).size.height * .007),
                ),
                RaisedButton(
                  child: Text(
                    "Delete",
                    style: TextStyle(),
                  ),
                  /*
                    TODO delete the category from DB and if success,
                     then remove from local list (https://github.com/SCCapstone/decision_maker/issues/97)
                  */
                  onPressed: () {
                    this.onDelete(); // this deletes it from the local list
                  },
                ),
              ],
            ),
          ],
        ),
        decoration:
            new BoxDecoration(border: new Border(bottom: new BorderSide())),
      );
    }
  }
}
