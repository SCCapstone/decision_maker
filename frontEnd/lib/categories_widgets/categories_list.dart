import 'package:flutter/material.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'categories_list_item.dart';

class CategoryList extends StatefulWidget {
  final List<Category> categories;
  final int sortType;
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
                if (widget.sortType == Globals.alphabeticalSort) {
                  CategoriesManager.sortByAlphaAscending(widget.categories);
                } else {
                  CategoriesManager.sortByAlphaDescending(widget.categories);
                }
                return CategoriesListItem(widget.categories[index], index,
                    onDelete: () => removeItem(index),
                    afterEditCallback: widget.refreshPage);
              }));
    }
  }

  void removeItem(int index) async {
    // removes an item from the local list of categories used in the CategoryList state
    Category category = widget.categories[index];

    showLoadingDialog(context, "Deleting category...", true);
    ResultStatus status =
        await CategoriesManager.deleteCategory(category.categoryId);
    Navigator.of(context, rootNavigator: true).pop('dialog');

    if (status.success) {
      setState(() {
        Globals.activeUserCategories.remove(widget.categories[index]);
        Globals.user.ownedCategories.remove(widget.categories[index]);
        widget.categories.remove(widget.categories[index]);
      });
    } else {
      showErrorMessage("Error", status.errorMessage, context);
    }
  }
}
