import 'package:flutter/material.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/widgets/category_row_group.dart';

class GroupCategories extends StatefulWidget {
  final Map<String, String>
      selectedCategories; // map of categoryIds -> categoryName

  GroupCategories({this.selectedCategories});

  @override
  _GroupCategoriesState createState() => _GroupCategoriesState();
}

class _GroupCategoriesState extends State<GroupCategories> {
  bool initialLoad = true;
  bool errorLoading = false;
  Widget errorWidget;
  List<Widget> ownedCategoryRows = new List<Widget>();
  List<Widget> groupCategoryRows = new List<Widget>();

  @override
  void initState() {
    getCategories();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    if (initialLoad) {
      return categoriesLoading();
    } else if (errorLoading) {
      return errorWidget;
    } else {
      return Scaffold(
        appBar: AppBar(
          centerTitle: true,
          title: Text(
            "Add Categories",
            style: TextStyle(
                fontSize: DefaultTextStyle.of(context).style.fontSize * 0.5),
          ),
        ),
        body: Column(
          children: <Widget>[
            Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .015),
            ),
            Text(
              "My Categories",
              style: TextStyle(
                  fontSize: DefaultTextStyle.of(context).style.fontSize * 0.5),
            ),
            Visibility(
              visible: ownedCategoryRows.isEmpty,
              child: Column(
                children: <Widget>[
                  Text(
                      "No categories found! Click button below to create some."),
                  Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .007),
                  ),
                  RaisedButton(
                    child: Text("Create Categories"),
                    onPressed: () {
                      //TODO take user to category page
                    },
                  )
                ],
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
              visible: (groupCategoryRows.isNotEmpty),
              child: Padding(
                padding:
                    EdgeInsets.all(MediaQuery.of(context).size.height * .05),
              ),
            ),
            Visibility(
              visible: (groupCategoryRows.isNotEmpty),
              child: Text(
                "Categories Added By Members",
                style: TextStyle(
                    fontSize:
                        DefaultTextStyle.of(context).style.fontSize * 0.5),
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
            centerTitle: true,
            title: Text(
              "Add Categories",
              style: TextStyle(
                  fontSize: DefaultTextStyle.of(context).style.fontSize * 0.5),
            )),
        body: Center(child: CircularProgressIndicator()));
  }

  Widget categoriesError(String errorMsg) {
    return Scaffold(
        appBar: AppBar(
            centerTitle: true,
            title: Text(
              "Add Categories",
              style: TextStyle(
                  fontSize: DefaultTextStyle.of(context).style.fontSize * 0.5),
            )),
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
    initialLoad = false;
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
            onSelect: () => selectCategory(category),
          ));
        } else if (Globals.user.ownedCategories.contains(category)) {
          // separate the categories the user owns
          ownedCategoryRows.add(new CategoryRowGroup(
              category,
              widget.selectedCategories.keys.contains(category.categoryId),
              false,
              onSelect: () => selectCategory(category)));
        }
      }
      setState(() {});
    } else {
      setState(() {
        errorWidget = categoriesError(resultStatus.errorMessage);
        errorLoading = true;
      });
    }
  }
}