import 'package:flutter/material.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/category.dart';
import 'categories_list_item.dart';

class CategoryList extends StatefulWidget {
  final List<Category> categories;
  final String sortType;
  final Function refreshPage;

  CategoryList({Key key, this.categories, this.sortType, this.refreshPage})
      : super(key: key);

  @override
  _CategoryListState createState() => _CategoryListState();
}

class _CategoryListState extends State<CategoryList> {
  @override
  Widget build(BuildContext context) {
    if (widget.categories.length == 0) {
      return Center(
        child: Text(
            "No categories found! Click the button below to create some!",
            style: TextStyle(fontSize: 30)),
      );
    } else {
      return Scrollbar(
          child: ListView.builder(
              shrinkWrap: true,
              itemCount: widget.categories.length,
              itemBuilder: (BuildContext context, int index) {
                bool isOwner = false;
                if (widget.categories[index].owner == Globals.username) {
                  isOwner = true;
                }
                return CategoriesListItem(
                    widget.categories[index], index, isOwner,
                    onDelete: () => removeItem(index),
                    afterEditCallback: widget.refreshPage);
              }));
    }
  }

  void removeItem(int index) {
    // removes an item from the local list of categories used in the CategoryList state
    setState(() {
      Category category = widget.categories[index];
      CategoriesManager.deleteCategory(category.categoryId, context);
      widget.categories.remove(widget.categories[index]);
    });
  }
}
