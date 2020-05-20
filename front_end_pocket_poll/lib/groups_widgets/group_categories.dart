import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/categories_widgets/categories_home.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/models/category_rating_tuple.dart';
import 'package:front_end_pocket_poll/models/group_category.dart';
import 'group_category_row.dart';

class GroupCategories extends StatefulWidget {
  final Map<String, GroupCategory>
      selectedCategories; // map of categoryIds -> GroupCategory
  final bool canEdit;

  GroupCategories({this.selectedCategories, this.canEdit});

  @override
  _GroupCategoriesState createState() => _GroupCategoriesState();
}

class _GroupCategoriesState extends State<GroupCategories> {
  bool loading;
  bool errorLoading;
  int sortVal;
  Widget errorWidget;
  List<GroupCategoryRow> ownedCategoryRows;
  List<GroupCategoryRow> groupCategoryRows;
  List<CategoryRatingTuple> groupCategories;

  @override
  void initState() {
    this.loading = true;
    this.errorLoading = false;
    this.sortVal = Globals.user.appSettings.categorySort;
    this.ownedCategoryRows = new List<GroupCategoryRow>();
    this.groupCategoryRows = new List<GroupCategoryRow>();
    this.getCategoriesAndBuildRows();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    if (this.loading) {
      return categoriesLoading();
    } else if (this.errorLoading) {
      return this.errorWidget;
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
        resizeToAvoidBottomPadding: false,
        key: Key("group_categories:scaffold"),
        body: Column(
          children: <Widget>[
            Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .015),
            ),
            Visibility(
              visible: this.groupCategoryRows.isEmpty &&
                  this.ownedCategoryRows.isEmpty &&
                  !widget.canEdit,
              child: AutoSizeText(
                "There are no categories currently associated with this group. "
                "Ask the creator of the group (@${Globals.currentGroupResponse.group.groupCreator}) to add some!",
                minFontSize: 15,
                maxLines: 3,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(fontSize: 20),
              ),
            ),
            Visibility(
                visible: this.ownedCategoryRows.isNotEmpty,
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
              visible: this.ownedCategoryRows.isEmpty && widget.canEdit,
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
                          this.buildCategoryRows();
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
                    itemCount: this.ownedCategoryRows.length,
                    itemBuilder: (BuildContext context, int index) {
                      return this.ownedCategoryRows[index];
                    }),
              ),
            ),
            Visibility(
              visible: (this.groupCategoryRows.isNotEmpty &&
                  this.ownedCategoryRows.isNotEmpty),
              child: Padding(
                padding:
                    EdgeInsets.all(MediaQuery.of(context).size.height * .05),
              ),
            ),
            Visibility(
              visible: (this.groupCategoryRows.isNotEmpty),
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
                      itemCount: this.groupCategoryRows.length,
                      itemBuilder: (BuildContext context, int index) {
                        return this.groupCategoryRows[index];
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
        body: Center(child: CircularProgressIndicator()),
        key: Key("group_categories:scaffold_loading"));
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
            onRefresh: getCategoriesAndBuildRows,
          ),
        ),
        key: Key("group_categories:scaffold_error"));
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

  // adds this category to the list of selected (i.e. adds it to the group)
  void selectCategory(final Category category) {
    if (widget.selectedCategories.keys.contains(category.categoryId)) {
      widget.selectedCategories.remove(category.categoryId);
    } else {
      widget.selectedCategories
          .putIfAbsent(category.categoryId, () => category.asGroupCategory());
    }
    this.buildCategoryRows();
  }

  /*
    Loads all the categories the group has attached to it. Categories that are
    owned by the active user are set to checked if they are in the group.
   */
  Future<void> getCategories() async {
    ResultStatus<List<CategoryRatingTuple>> resultStatus =
        await GroupsManager.getAllCategoriesList(
            Globals.currentGroupResponse.group.groupId);
    this.loading = false;

    if (resultStatus.success) {
      setState(() {
        this.groupCategories = resultStatus.data;
        this.errorLoading = false;
      });
    } else {
      setState(() {
        this.errorWidget = categoriesError(resultStatus.errorMessage);
        this.errorLoading = true;
      });
    }
  }

  //this method simply loops over the user's categories and the group's
  // categories and builds the appropriate category rows
  void buildCategoryRows() {
    if (!this.errorLoading) {
      this.ownedCategoryRows.clear();
      this.groupCategoryRows.clear();

      int index = 0; // used for integration tests

      //build the users's categories
      for (Category category in Globals.user.ownedCategories) {
        if (widget.canEdit ||
            Globals.currentGroupResponse.group.categories
                .containsKey(category.categoryId)) {
          this.ownedCategoryRows.add(new GroupCategoryRow(
              null,
              category,
              widget.selectedCategories.keys.contains(category.categoryId),
              this.buildCategoryRows,
              widget.canEdit,
              onSelect: () => selectCategory(category),
              index: index));
          index++;
        }
      }

      //build the group categories
      for (CategoryRatingTuple categoryRatingTuple in this.groupCategories) {
        if (!Globals.user.ownedCategories
            .contains(categoryRatingTuple.category)) {
          // separate the categories of the group that the user doesn't own
          this.groupCategoryRows.add(new GroupCategoryRow(
                categoryRatingTuple,
                categoryRatingTuple.category,
                widget.selectedCategories.keys
                    .contains(categoryRatingTuple.category.categoryId),
                this.buildCategoryRows,
                widget.canEdit,
                onSelect: () => selectCategory(categoryRatingTuple.category),
                index: index,
              ));
          index++;
        }
      }

      this.sortGroupCategoryRows();
      this.sortOwnedCategoryRows();
      setState(() {});
    }
  }

  Future<void> getCategoriesAndBuildRows() async {
    await this.getCategories();
    this.buildCategoryRows();
  }
}
