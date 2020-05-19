import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/models/category.dart';

import 'category_row.dart';

class CategoryPopupSingle extends StatefulWidget {
  final Category initialSelectedCategory;

  CategoryPopupSingle(this.initialSelectedCategory);

  @override
  _CategoryPopupSingleState createState() => _CategoryPopupSingleState();
}

class _CategoryPopupSingleState extends State<CategoryPopupSingle> {
  List<CategoryRow> categoryRows;
  Category selectedCategory;

  bool loading;
  bool errorLoading;
  Widget errorWidget;

  @override
  void initState() {
    this.selectedCategory = widget.initialSelectedCategory;
    this.categoryRows = new List<CategoryRow>();
    buildChoiceRows();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text("Select Category"),
      actions: <Widget>[
        FlatButton(
          child: Text("DONE"),
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
                  child: (this.categoryRows.isNotEmpty)
                      ? Container(
                          height: MediaQuery.of(context).size.height * .25,
                          key: Key("category_popup_single:category_container"),
                          child: ListView.builder(
                            shrinkWrap: true,
                            itemCount: this.categoryRows.length,
                            itemBuilder: (context, index) {
                              return this.categoryRows[index];
                            },
                          ),
                        )
                      : Container(
                          height: MediaQuery.of(context).size.height * .25,
                          child: RichText(
                            text: TextSpan(
                              style: TextStyle(
                                  color: (Globals.user.appSettings.darkTheme)
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
                                    text:
                                        " found in the top right corner of the group's page to add some."),
                              ],
                            ),
                          ))),
            ],
          ),
        ),
      ),
    );
  }

  // builds choice rows using the current group group categories
  void buildChoiceRows() {
    int index = 0; // used for integration testing
    for (String catId in Globals.currentGroup.categories.keys) {
      this.categoryRows.add(CategoryRow(
            new Category(
                categoryName:
                    Globals.currentGroup.categories[catId].categoryName,
                owner: Globals.currentGroup.categories[catId].owner),
            this.selectedCategory != null &&
                this.selectedCategory.categoryId == catId,
            onSelect: () => selectCategory(new Category(
                categoryId: catId,
                categoryName:
                    Globals.currentGroup.categories[catId].categoryName)),
            index: index,
          ));
      index++;
    }
    // sorting alphabetically
    this.categoryRows.sort((a, b) {
      return a.category.categoryName
          .toLowerCase()
          .compareTo(b.category.categoryName.toLowerCase());
    });
  }

  // selects a category for an event. Note only one category can be selected
  void selectCategory(Category category) {
    setState(() {
      this.selectedCategory = category;
      this.categoryRows.clear();
      buildChoiceRows();
    });
  }
}
