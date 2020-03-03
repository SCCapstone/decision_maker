import 'dart:async';
import 'package:flutter/material.dart';
import 'package:frontEnd/categories_widgets/categories_create.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'categories_list.dart';

class CategoriesHome extends StatefulWidget {
  CategoriesHome({Key key}) : super(key: key);

  @override
  _CategoriesHomeState createState() => new _CategoriesHomeState();
}

class _CategoriesHomeState extends State<CategoriesHome> {
  String _sortMethod;
  List<Category> categories;
  bool loading;
  bool errorLoading;
  Widget errorWidget; // global var to pass in error message from manager

  @override
  void initState() {
    loading = true;
    errorLoading = false;
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
          centerTitle: true,
          title: Text(
            "Categories",
            style: TextStyle(
                fontSize: DefaultTextStyle.of(context).style.fontSize * 0.75),
          ),
//        actions: <Widget>[
//          IconButton(
//            icon: Icon(Icons.sort),
//            onPressed: () {
//              // can implement a variable that has the sort type, then setState here
//              // TODO implement a sorting algorithm (https://github.com/SCCapstone/decision_maker/issues/31)
//            },
//          )
//        ],
        ),
        body: RefreshIndicator(
          onRefresh: refreshList,
          child: Center(
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
                        categories: categories,
                        sortType: _sortMethod,
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
        ),
        floatingActionButton: FloatingActionButton(
          child: Icon(Icons.add),
          onPressed: () {
            // Navigate to second route when tapped.
            Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => CreateCategory()),
            ).then((_) => this.refreshList());
          },
        ),
      );
    }
  }

  void getCategories() async {
    ResultStatus<List<Category>> status =
        await CategoriesManager.getAllCategoriesList();
    if (status.success) {
      errorLoading = false;
      categories = status.data;
    } else {
      errorLoading = true;
      errorWidget = loadingError(status.errorMessage);
    }
    setState(() {
      loading = false;
    });
  }

  Widget categoriesLoading() {
    return Scaffold(
        appBar: AppBar(
            centerTitle: true,
            title: Text(
              "Categories",
              style: TextStyle(
                  fontSize: DefaultTextStyle.of(context).style.fontSize * 0.75),
            )),
        body: Center(child: CircularProgressIndicator()));
  }

  Widget loadingError(String errorMsg) {
    return Scaffold(
        appBar: AppBar(
            centerTitle: true,
            title: Text(
              "Categories",
              style: TextStyle(
                  fontSize: DefaultTextStyle.of(context).style.fontSize * 0.75),
            )),
        body: Container(
          height: MediaQuery.of(context).size.height * .80,
          child: RefreshIndicator(
            onRefresh: refreshList,
            child: ListView(
              children: <Widget>[
                Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .15)),
                Center(child: Text(errorMsg, style: TextStyle(fontSize: 30))),
              ],
            ),
          ),
        ));
  }

  Future<Null> refreshList() async {
    getCategories();
    //TODO look in to updating this so that we don't have to re-query the categories, we could potentially use some global var for this (https://github.com/SCCapstone/decision_maker/issues/106)
    setState(() {});
  }
}
