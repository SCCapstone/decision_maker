import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/categories_widgets/categories_home.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/models/group_category.dart';

import 'category_row.dart';

class CategoryPick extends StatefulWidget {
  final Map<String, GroupCategory>
      selectedCategories; // map of categoryIds -> GroupCategory

  CategoryPick(this.selectedCategories);

  @override
  _CategoryPickState createState() => _CategoryPickState();
}

class _CategoryPickState extends State<CategoryPick> {
  List<Widget> categoryRows;

  @override
  void initState() {
    this.loadCategoryRows();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Select Categories"),
      ),
      key: Key("category_pick:scaffold"),
      body: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Expanded(
              child: Scrollbar(
                child: Container(
                  height: MediaQuery.of(context).size.height * .25,
                  child: (this.categoryRows.isNotEmpty)
                      ? ListView(
                          shrinkWrap: false,
                          children: categoryRows,
                        )
                      : Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: <Widget>[
                            Text(
                              "No categories found to add! Categories are list of choices that are used to make events."
                              " Navigate to the categories homepage to create some.",
                              style: TextStyle(fontSize: 20),
                            ),
                            RaisedButton(
                                child: Text("Categories Home"),
                                onPressed: () {
                                  Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                          builder: (context) =>
                                              CategoriesHome())).then((val) {
                                    setState(() {
                                      // check if the user actually added any categories, if so populate them
                                      this.loadCategoryRows();
                                    });
                                  });
                                })
                          ],
                        ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  // load categories into the category rows using the categories found on local user object
  void loadCategoryRows() {
    this.categoryRows = new List<Widget>(); // reset the rows

    int index = 0; // used for integration testing
    for (Category category in Globals.user.ownedCategories) {
      this.categoryRows.add(CategoryRow(
            category,
            widget.selectedCategories.keys.contains(category.categoryId),
            onSelect: () => selectCategory(category),
            index: index,
          ));
      index++;
    }
  }

  // select a category to be added or, if it already selected, remove it from the selection
  void selectCategory(final Category category) {
    setState(() {
      if (widget.selectedCategories.keys.contains(category.categoryId)) {
        widget.selectedCategories.remove(category.categoryId);
      } else {
        widget.selectedCategories
            .putIfAbsent(category.categoryId, () => category.asGroupCategory());
      }

      this.loadCategoryRows();
    });
  }
}
