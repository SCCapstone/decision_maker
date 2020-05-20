import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/models/group_category.dart';
import 'package:front_end_pocket_poll/widgets/category_row.dart';

class EventPickCategory extends StatefulWidget {
  final Category initialSelectedCategory;

  EventPickCategory(this.initialSelectedCategory);

  @override
  _EventPickCategoryState createState() => _EventPickCategoryState();
}

class _EventPickCategoryState extends State<EventPickCategory> {
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
          key: Key("event_pick_category:done_button"),
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
                          key: Key("event_pick_category:category_container"),
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
    setState(() {
      this.selectedCategory = category;
      this.categoryRows.clear();
      buildChoiceRows();
    });
  }
}
