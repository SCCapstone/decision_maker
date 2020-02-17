import 'package:flutter/material.dart';
import 'package:frontEnd/imports/categories_manager.dart';
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
  Future<List<Category>> categoriesTotalFuture;

  @override
  void initState() {
    categoriesTotalFuture = CategoriesManager.getAllCategoriesList();
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
                    future: categoriesTotalFuture,
                    builder: (BuildContext context, AsyncSnapshot snapshot) {
                      if (snapshot.hasData) {
                        List<Category> categories = snapshot.data;
                        categories = snapshot.data;
                        for (Category category in categories) {
                          this.categoryRows.add(CategoryRow(
                              category,
                              widget.selectedCategories.keys
                                  .contains(category.categoryId),
                              onSelect: () => selectCategory(category)));
                        }
                        return Container(
                          height: MediaQuery.of(context).size.height * .25,
                          child: ListView(
                            shrinkWrap: true,
                            children: categoryRows,
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
      if (widget.selectedCategories.keys.contains(category.categoryId)) {
        widget.selectedCategories.remove(category.categoryId);
      } else {
        widget.selectedCategories
            .putIfAbsent(category.categoryId, () => category.categoryName);
      }
    });
  }
}
