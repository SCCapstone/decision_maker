import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/categories_widgets/categories_home.dart';
import 'package:front_end_pocket_poll/imports/categories_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/widgets/category_row_group.dart';

class GroupCategories extends StatefulWidget {
  final Map<String, String>
      selectedCategories; // map of categoryIds -> categoryName
  final bool canEdit;

  GroupCategories({this.selectedCategories, this.canEdit});

  @override
  _GroupCategoriesState createState() => _GroupCategoriesState();
}

class _GroupCategoriesState extends State<GroupCategories> {
  bool loading = true;
  bool errorLoading = false;
  int sortVal;
  Widget errorWidget;
  List<CategoryRowGroup> ownedCategoryRows = new List<CategoryRowGroup>();
  List<CategoryRowGroup> groupCategoryRows = new List<CategoryRowGroup>();

  @override
  void initState() {
    this.sortVal = Globals.user.appSettings.categorySort;
    getCategories();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    if (loading) {
      return categoriesLoading();
    } else if (errorLoading) {
      return errorWidget;
    } else {
      return Scaffold(
        appBar: AppBar(
          centerTitle: false,
          title: (widget.canEdit)
              ? Text(
                  "Add Categories",
                  style: TextStyle(
                      fontSize:
                          DefaultTextStyle.of(context).style.fontSize * 0.5),
                )
              : Text(
                  "View Categories",
                  style: TextStyle(
                      fontSize:
                          DefaultTextStyle.of(context).style.fontSize * 0.5),
                ),
          actions: <Widget>[
            PopupMenuButton<int>(
              child: Icon(
                Icons.sort,
                size: MediaQuery.of(context).size.height * .04,
                color: Colors.black,
              ),
              tooltip: "Sort Categories",
              onSelected: (int result) {
                if (this.sortVal != result) {
                  // prevents useless updates if sort didn't change
                  this.sortVal = result;
                  setState(() {
                    sortOwnedCategoryRows();
                    sortGroupCategoryRows();
                    updateSort();
                  });
                }
              },
              itemBuilder: (BuildContext context) => <PopupMenuEntry<int>>[
                PopupMenuItem<int>(
                  value: Globals.alphabeticalSort,
                  child: Text(
                    Globals.alphabeticalSortString,
                    style: TextStyle(
                        // if it is selected, underline it
                        decoration: (this.sortVal == Globals.alphabeticalSort)
                            ? TextDecoration.underline
                            : null),
                  ),
                ),
                PopupMenuItem<int>(
                  value: Globals.alphabeticalReverseSort,
                  child: Text(Globals.alphabeticalReverseSortString,
                      style: TextStyle(
                          // if it is selected, underline it
                          decoration:
                              (this.sortVal == Globals.alphabeticalReverseSort)
                                  ? TextDecoration.underline
                                  : null)),
                ),
              ],
            ),
            Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .007),
            ),
          ],
        ),
        body: Column(
          children: <Widget>[
            Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .015),
            ),
            Visibility(
              visible: groupCategoryRows.isEmpty &&
                  ownedCategoryRows.isEmpty &&
                  !widget.canEdit,
              child: AutoSizeText(
                "There are no categories currently associated with this group. "
                "Ask the creator of the group (@${Globals.currentGroup.groupCreator}) to add some!",
                minFontSize: 15,
                maxLines: 3,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(fontSize: 20),
              ),
            ),
            Visibility(
                visible: ownedCategoryRows.isNotEmpty,
                child: (widget.canEdit)
                    ? AutoSizeText(
                        "My Categories",
                        minFontSize: 15,
                        maxLines: 1,
                        style: TextStyle(fontSize: 26),
                      )
                    : AutoSizeText(
                        "Categories Added By Me",
                        minFontSize: 15,
                        maxLines: 1,
                        style: TextStyle(fontSize: 26),
                      )),
            Visibility(
              visible: ownedCategoryRows.isEmpty && widget.canEdit,
              child: Padding(
                padding: EdgeInsets.fromLTRB(
                    MediaQuery.of(context).size.width * .07,
                    0,
                    MediaQuery.of(context).size.width * .07,
                    0),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .015),
                    ),
                    Text(
                        "No categories found to add. Navigate to the categories homepage to create some."),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .007),
                    ),
                    RaisedButton(
                      child: Text("Create Categories"),
                      onPressed: () {
                        Navigator.push(
                                context,
                                MaterialPageRoute(
                                    builder: (context) => CategoriesHome()))
                            .then((val) {
                          setState(() {
                            this.loading = true;
                          });
                          this.getCategories();
                        });
                      },
                    )
                  ],
                ),
              ),
            ),
            ConstrainedBox(
              constraints: BoxConstraints(
                  maxHeight: MediaQuery.of(context).size.height * .40),
              child: Scrollbar(
                child: ListView.builder(
                    shrinkWrap: true,
                    itemCount: ownedCategoryRows.length,
                    itemBuilder: (BuildContext context, int index) {
                      return ownedCategoryRows[index];
                    }),
              ),
            ),
            Visibility(
              visible: (groupCategoryRows.isNotEmpty &&
                  ownedCategoryRows.isNotEmpty),
              child: Padding(
                padding:
                    EdgeInsets.all(MediaQuery.of(context).size.height * .05),
              ),
            ),
            Visibility(
              visible: (groupCategoryRows.isNotEmpty),
              child: AutoSizeText(
                "Categories Added By Members",
                minFontSize: 15,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(fontSize: 26),
              ),
            ),
            Expanded(
              child: Container(
                height: MediaQuery.of(context).size.height * .40,
                child: Scrollbar(
                  child: ListView.builder(
                      itemCount: groupCategoryRows.length,
                      itemBuilder: (BuildContext context, int index) {
                        return groupCategoryRows[index];
                      }),
                ),
              ),
            ),
            Padding(
              padding: EdgeInsets.all(MediaQuery.of(context).size.height * .02),
            )
          ],
        ),
      );
    }
  }

  Widget categoriesLoading() {
    return Scaffold(
        appBar: AppBar(
            centerTitle: false,
            title: (widget.canEdit)
                ? Text("Add Categories",
                    style: TextStyle(
                        fontSize:
                            DefaultTextStyle.of(context).style.fontSize * 0.5))
                : Text("View Categories",
                    style: TextStyle(
                        fontSize: DefaultTextStyle.of(context).style.fontSize *
                            0.5))),
        body: Center(child: CircularProgressIndicator()));
  }

  Widget categoriesError(String errorMsg) {
    return Scaffold(
        appBar: AppBar(
            centerTitle: false,
            title: (widget.canEdit)
                ? Text("Add Categories",
                    style: TextStyle(
                        fontSize:
                            DefaultTextStyle.of(context).style.fontSize * 0.5))
                : Text("View Categories",
                    style: TextStyle(
                        fontSize: DefaultTextStyle.of(context).style.fontSize *
                            0.5))),
        body: Container(
          height: MediaQuery.of(context).size.height * .80,
          child: RefreshIndicator(
            child: ListView(
              children: <Widget>[
                Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .15)),
                Center(child: Text(errorMsg, style: TextStyle(fontSize: 30))),
              ],
            ),
            onRefresh: getCategories,
          ),
        ));
  }

  void updateOwnedCategories() {
    this.ownedCategoryRows.clear();

    for (Category category in Globals.user.ownedCategories) {
      if (widget.canEdit ||
          Globals.currentGroup.categories.containsKey(category.categoryId)) {
        this.ownedCategoryRows.add(new CategoryRowGroup(
            category,
            widget.selectedCategories.keys.contains(category.categoryId),
            false,
            updateOwnedCategories,
            widget.canEdit,
            onSelect: () => selectCategory(category)));
      }
    }
    sortOwnedCategoryRows();
    setState(() {});
  }

  void sortGroupCategoryRows() {
    if (this.sortVal == Globals.alphabeticalSort) {
      this.groupCategoryRows.sort((a, b) => a.category.categoryName
          .toLowerCase()
          .compareTo(b.category.categoryName.toLowerCase()));
    } else if (this.sortVal == Globals.alphabeticalReverseSort) {
      this.groupCategoryRows.sort((a, b) => b.category.categoryName
          .toLowerCase()
          .compareTo(a.category.categoryName.toLowerCase()));
    }
  }

  void sortOwnedCategoryRows() {
    if (this.sortVal == Globals.alphabeticalSort) {
      this.ownedCategoryRows.sort((a, b) => a.category.categoryName
          .toLowerCase()
          .compareTo(b.category.categoryName.toLowerCase()));
    } else if (this.sortVal == Globals.alphabeticalReverseSort) {
      this.ownedCategoryRows.sort((a, b) => b.category.categoryName
          .toLowerCase()
          .compareTo(a.category.categoryName.toLowerCase()));
    }
  }

  void updateSort() {
    //blind send, don't care if it doesn't work since it's just a sort value
    UsersManager.updateSortSetting(
        UsersManager.APP_SETTINGS_CATEGORY_SORT, this.sortVal);
    Globals.user.appSettings.categorySort = this.sortVal;
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

  void getCategories() async {
    ResultStatus<List<Category>> resultStatus =
        await CategoriesManager.getAllCategoriesList();
    loading = false;
    if (resultStatus.success) {
      errorLoading = false;
      List<Category> selectedCats = resultStatus.data;
      for (Category category in selectedCats) {
        if (!Globals.user.ownedCategories.contains(category) &&
            category.groups.containsKey(Globals.currentGroup.groupId)) {
          // separate the categories of the group that the user doesn't own
          groupCategoryRows.add(new CategoryRowGroup(
            category,
            widget.selectedCategories.keys.contains(category.categoryId),
            true,
            updateOwnedCategories,
            widget.canEdit,
            onSelect: () => selectCategory(category),
          ));
        } else if (Globals.user.ownedCategories.contains(category)) {
          // separate the categories the user owns. add every category if the user
          // currently has permission to edit group settings, otherwise only add
          // categories that the user had already added to the group
          if (widget.canEdit ||
              Globals.currentGroup.categories
                  .containsKey(category.categoryId)) {
            ownedCategoryRows.add(new CategoryRowGroup(
                category,
                widget.selectedCategories.keys.contains(category.categoryId),
                false,
                updateOwnedCategories,
                widget.canEdit,
                onSelect: () => selectCategory(category)));
          }
        }
      }
      sortGroupCategoryRows();
      sortOwnedCategoryRows();
      setState(() {});
    } else {
      setState(() {
        errorWidget = categoriesError(resultStatus.errorMessage);
        errorLoading = true;
      });
    }
  }
}