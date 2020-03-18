import 'dart:async';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/categories_widgets/categories_create.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'categories_list.dart';

class CategoriesHome extends StatefulWidget {
  CategoriesHome({Key key}) : super(key: key);

  @override
  _CategoriesHomeState createState() => new _CategoriesHomeState();
}

class _CategoriesHomeState extends State<CategoriesHome> {
  int sortVal;
  List<Category> categories;

  @override
  void initState() {
    this.sortVal = Globals.user.appSettings.categorySort;
    this.categories = new List<Category>();
    getCategories();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        centerTitle: true,
        title: AutoSizeText(
          "My Categories",
          maxLines: 1,
          style: TextStyle(fontSize: 35),
          minFontSize: 12,
          overflow: TextOverflow.ellipsis,
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
                  updateSort();
                });
              }
            },
            itemBuilder: (BuildContext context) => <PopupMenuEntry<int>>[
              PopupMenuItem<int>(
                value: Globals.alphabeticalSort,
                child: Text(Globals.alphabeticalSortString),
              ),
              PopupMenuItem<int>(
                value: Globals.alphabeticalReverseSort,
                child: Text(Globals.alphabeticalReverseSortString),
              ),
            ],
          ),
          Padding(
            padding: EdgeInsets.all(MediaQuery.of(context).size.height * .007),
          ),
        ],
      ),
      body: Center(
        child: Column(
          children: <Widget>[
            Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .015),
            ),
            Expanded(
              child: Container(
                width: MediaQuery.of(context).size.width * .80,
                height: MediaQuery.of(context).size.height * .75,
                child: Container(
                  child: CategoryList(
                    categories: this.categories,
                    sortType: this.sortVal,
                    refreshPage: this.refreshList,
                  ),
                ),
              ),
            ),
            Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .015),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        child: Icon(Icons.add),
        onPressed: () {
          if (Globals.user.ownedCategories.length >= Globals.maxCategories) {
            showErrorMessage(
                "Error",
                "You cannot create more than ${Globals.user.ownedCategories.length} categories at this time.",
                context);
          } else {
            Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => CreateCategory()),
            ).then((_) => this.getCategories());
          }
        },
      ),
    );
  }

  void updateSort() {
    // TODO make API call to update sort method
  }

  void getCategories() {
    this.categories.clear();
    for (Category category in Globals.user.ownedCategories) {
      this.categories.add(category);
    }
  }

  Future<Null> refreshList() async {
    getCategories();
    setState(() {});
  }
}
