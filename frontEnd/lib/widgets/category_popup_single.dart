import 'package:flutter/material.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/category.dart';

import 'category_row.dart';

class CategoryPopupSingle extends StatefulWidget {
  final List<Category> selectedCategory; // max of one element in this list
  final Function handlePopupClosed;


  CategoryPopupSingle(this.selectedCategory, {this.handlePopupClosed});

  @override
  _CategoryPopupSingleState createState() => _CategoryPopupSingleState();
}

class _CategoryPopupSingleState extends State<CategoryPopupSingle> {
  List<Widget> categoryRows = new List<Widget>();
  Future<List<Category>> categoriesTotalFuture;

  @override
  void initState() {
    categoriesTotalFuture = CategoriesManager.getAllCategoriesFromGroup(
        Globals.currentGroup.groupId, context);
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    // displays a popup for editing the group's members
    return AlertDialog(
      title: Text("Select Category"),
      actions: <Widget>[
        FlatButton(
          child: Text("Done"),
          onPressed: () {
            widget.handlePopupClosed();
            Navigator.of(context, rootNavigator: true).pop('dialog');
          },
        ),
      ],
      content: SingleChildScrollView(
        scrollDirection: Axis.vertical,
        child: Container(
          width: double.maxFinite,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Scrollbar(
                child: FutureBuilder(
                    future: categoriesTotalFuture,
                    builder: (BuildContext context, AsyncSnapshot snapshot) {
                      if (snapshot.hasData) {
                        List<Category> categories = snapshot.data;
                        categories = snapshot.data;
                        for (Category category in categories) {
                          this.categoryRows.add(CategoryRow(category,
                              widget.selectedCategory.contains(category),
                              onSelect: () => selectCategory(category)));
                        }
                        return Container(
                          height: MediaQuery.of(context).size.height * .25,
                          child: ListView.builder(
                            shrinkWrap: true,
                            itemCount: this.categoryRows.length,
                            itemBuilder: (context, index) {
                              return this.categoryRows[index];
                            },
                          ),
                        );
                      } else if (snapshot.hasError) {
                        return Text("Error: ${snapshot.error}");
                      } else {
                        return Center(child: CircularProgressIndicator());
                      }
                    }),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void selectCategory(Category category) {
    setState(() {
      this.categoryRows.clear();
      if (widget.selectedCategory.contains(category)) {
        widget.selectedCategory.remove(category);
      } else if (widget.selectedCategory.isEmpty) {
        widget.selectedCategory.add(category);
      } else {
        // only one category can be selected
        widget.selectedCategory.clear();
        widget.selectedCategory.add(category);
      }
    });
  }
}
