import 'package:flutter/material.dart';
import 'package:frontEnd/categories_widgets/categories_home.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/category.dart';

import 'category_row.dart';

class CategoryPick extends StatefulWidget {
  final Map<String, String>
      selectedCategories; // map of categoryIds -> categoryName

  CategoryPick(this.selectedCategories);

  @override
  _CategoryPickState createState() => _CategoryPickState();
}

class _CategoryPickState extends State<CategoryPick> {
  List<Widget> categoryRows = new List<Widget>();

  @override
  void initState() {
    this.getCategories();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Select Categories"),
      ),
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
                              "No categories found to add. Navigate to the categories homepage to create some.",
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
                                      this.getCategories();
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

  void getCategories() {
    for (Category category in Globals.user.ownedCategories) {
      this.categoryRows.add(CategoryRow(category,
          widget.selectedCategories.keys.contains(category.categoryId),
          onSelect: () => selectCategory(category)));
    }
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
