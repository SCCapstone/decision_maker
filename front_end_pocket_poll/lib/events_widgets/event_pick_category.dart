import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/models/group_category.dart';
import 'package:front_end_pocket_poll/widgets/category_row.dart';

class EventPickCategory extends StatefulWidget {
  final Category initialSelectedCategory;
  final Function selectCategory;

  EventPickCategory(this.initialSelectedCategory, this.selectCategory);

  @override
  _EventPickCategoryState createState() => _EventPickCategoryState();
}

class _EventPickCategoryState extends State<EventPickCategory> {
  List<CategoryRow> categoryRows;
  Category selectedCategory;
  final ScrollController controller = new ScrollController();

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
    return Scrollbar(
      child: (this.categoryRows.isNotEmpty)
          ? ListView.builder(
              controller: this.controller,
              shrinkWrap: true,
              itemCount: this.categoryRows.length,
              itemBuilder: (context, index) {
                return this.categoryRows[index];
              },
            )
          : Container(
              height: MediaQuery.of(context).size.height * .1,
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
              )),
      isAlwaysShown: true,
      controller: this.controller,
    );
  }

  // builds choice rows using the current group categories
  void buildChoiceRows() {
    int index = 0; // used for integration testing
    for (MapEntry<String, GroupCategory> catEntry
        in Globals.currentGroupResponse.group.categories.entries) {
      this.categoryRows.add(CategoryRow(
            new Category(
                categoryName: catEntry.value.categoryName,
                owner: catEntry.value.owner),
            this.selectedCategory != null &&
                this.selectedCategory.categoryId == catEntry.key,
            onSelect: () => selectCategory(new Category(
                categoryId: catEntry.key,
                categoryName: catEntry.value.categoryName)),
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
    widget.selectCategory(category);
    setState(() {
      this.selectedCategory = category;
      this.categoryRows.clear();
      buildChoiceRows();
    });
  }
}
