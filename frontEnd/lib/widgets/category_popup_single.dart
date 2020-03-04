import 'package:flutter/material.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/result_status.dart';
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
  Future<ResultStatus<List<Category>>> resultFuture;

  @override
  void initState() {
    resultFuture = CategoriesManager.getAllCategoriesFromGroup(
        Globals.currentGroup.groupId);
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
                    future: resultFuture,
                    builder: (BuildContext context, AsyncSnapshot snapshot) {
                      if (snapshot.hasData) {
                        ResultStatus<List<Category>> resultStatus = snapshot.data;
                        if (resultStatus.success) {
                          List<Category> categories = resultStatus.data;
                          for (Category category in categories) {
                            this.categoryRows.add(CategoryRow(category,
                                widget.selectedCategory.contains(category),
                                onSelect: () => selectCategory(category)));
                          }
                          if (this.categoryRows.length > 0) {
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
                          } else {
                            return Container(
                              height: MediaQuery.of(context).size.height * .25,
                              child: Text(
                                  "No categories found in this group. Navigate to the group settings page to add some."),
                            );
                          }
                        } else {
                          return Text(resultStatus.errorMessage);
                        }
                      } else if (snapshot.hasError) {
                        // this shouldn't ever be reached, but put it here to be safe
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
