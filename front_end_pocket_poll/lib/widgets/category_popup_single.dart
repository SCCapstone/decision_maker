import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/categories_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/models/category.dart';

import 'category_row.dart';

class CategoryPopupSingle extends StatefulWidget {
  final Category initialSelectedCategory;

  CategoryPopupSingle(this.initialSelectedCategory);

  @override
  _CategoryPopupSingleState createState() => _CategoryPopupSingleState();
}

class _CategoryPopupSingleState extends State<CategoryPopupSingle> {
  List<Widget> categoryRows;
  Category selectedCategory;
  Future<ResultStatus<List<Category>>> resultFuture;
  bool loading;
  bool errorLoading;
  Widget errorWidget;

  @override
  void initState() {
    this.selectedCategory = widget.initialSelectedCategory;
    this.categoryRows = new List<Widget>();
    this.resultFuture = CategoriesManager.getAllCategoriesFromGroup(
        Globals.currentGroup.groupId);
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text("Select Category"),
      actions: <Widget>[
        FlatButton(
          child: Text("Done"),
          key: Key("category_popup_single:done_button"),
          onPressed: () {
            Navigator.of(context, rootNavigator: true)
                .pop(this.selectedCategory);
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
                    future: this.resultFuture,
                    builder: (BuildContext context, AsyncSnapshot snapshot) {
                      if (snapshot.hasData) {
                        ResultStatus<List<Category>> resultStatus =
                            snapshot.data;
                        if (resultStatus.success) {
                          List<Category> categories = resultStatus.data;

                          if (categories.length > 0) {
                            CategoriesManager.sortByAlphaAscending(categories);
                            int index = 0; // used for integration testing
                            for (Category category in categories) {
                              this.categoryRows.add(CategoryRow(
                                    category,
                                    this.selectedCategory != null &&
                                        this.selectedCategory.categoryId ==
                                            category.categoryId,
                                    onSelect: () => selectCategory(category),
                                    index: index,
                                  ));
                              index++;
                            }

                            return Container(
                              height: MediaQuery.of(context).size.height * .25,
                              key: Key(
                                  "category_popup_single:category_container"),
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
                                height:
                                    MediaQuery.of(context).size.height * .25,
                                child: RichText(
                                  text: TextSpan(
                                    style: TextStyle(
                                        color:
                                            (Globals.user.appSettings.darkTheme)
                                                ? Colors.white
                                                : Colors.black),
                                    children: [
                                      TextSpan(
                                          text:
                                              "No categories found in this group. Click on this icon:  "),
                                      WidgetSpan(
                                        child: Icon(Icons.settings),
                                      ),
                                      TextSpan(
                                          text: " found in the top right corner of the group's page to add some."),
                                    ],
                                  ),
                                ));
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

  // selects a category for an event. Note only one category can be selected
  void selectCategory(Category category) {
    setState(() {
      this.categoryRows.clear();
      this.selectedCategory = category;
    });
  }
}
