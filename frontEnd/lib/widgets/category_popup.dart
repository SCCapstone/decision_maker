import 'package:flutter/material.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/models/category.dart';

import 'category_row.dart';

class CategoryPopup extends StatefulWidget {
  final Map<String, String>
      selectedCategories; // map of categoryIds -> categoryName
  final Function handlePopupClosed;

  CategoryPopup(this.selectedCategories, {this.handlePopupClosed});

  @override
  _CategoryPopupState createState() => _CategoryPopupState();
}

class _CategoryPopupState extends State<CategoryPopup> {
  List<Widget> categoryRows = new List<Widget>();
  Future<ResultStatus> resultFuture;

  @override
  void initState() {
    resultFuture = CategoriesManager.getAllCategoriesList();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    // displays a popup for editing the group's members
    return AlertDialog(
      title: Text("Add Categories"),
      actions: <Widget>[
        FlatButton(
          child: Text("Done"),
          onPressed: () {
            Navigator.of(context, rootNavigator: true).pop('dialog');
            widget.handlePopupClosed();
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
                    future: resultFuture,
                    builder: (BuildContext context, AsyncSnapshot snapshot) {
                      if (snapshot.hasData) {
                        ResultStatus<List<Category>> result = snapshot.data;
                        List<Category> categories = result.data;
                        for (Category category in categories) {
                          if (category.owner == Globals.username) {
                            this.categoryRows.add(CategoryRow(
                                category,
                                widget.selectedCategories.keys
                                    .contains(category.categoryId),
                                onSelect: () => selectCategory(category)));
                          }
                        }
                        if (this.categoryRows.length > 0) {
                          return Container(
                            height: MediaQuery.of(context).size.height * .25,
                            child: ListView(
                              shrinkWrap: true,
                              children: categoryRows,
                            ),
                          );
                        } else {
                          // TODO add a button to let them make categories from here
                          return Container(
                            height: MediaQuery.of(context).size.height * .25,
                            child: Text(
                                "No categories found to add. Navigate to the categories page to create some."),
                          );
                        }
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
      if (widget.selectedCategories.keys.contains(category.categoryId)) {
        widget.selectedCategories.remove(category.categoryId);
      } else {
        widget.selectedCategories
            .putIfAbsent(category.categoryId, () => category.categoryName);
      }
    });
  }
}
