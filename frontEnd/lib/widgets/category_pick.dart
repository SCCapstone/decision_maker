import 'package:flutter/material.dart';
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
    for (Category category in Globals.user.ownedCategories) {
      this.categoryRows.add(CategoryRow(category,
          widget.selectedCategories.keys.contains(category.categoryId),
          onSelect: () => selectCategory(category)));
    }
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
                child: Visibility(
                  visible: this.categoryRows.isNotEmpty,
                  child: Container(
                    height: MediaQuery.of(context).size.height * .25,
                    child: ListView(
                      shrinkWrap: false,
                      children: categoryRows,
                    ),
                  ),
                ),
              ),
            ),
            Visibility(
              visible: this.categoryRows.isEmpty,
              child: Container(
                height: MediaQuery.of(context).size.height * .25,
                child: Text(
                    "No categories found to add. Navigate to the categories page to create some."),
              ),
            )
          ],
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
