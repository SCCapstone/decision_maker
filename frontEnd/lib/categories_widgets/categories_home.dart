import 'dart:async';
import 'package:flutter/material.dart';
import 'package:frontEnd/categories_widgets/categories_create.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/models/category.dart';
import 'categories_list.dart';

class CategoriesHome extends StatefulWidget {
  CategoriesHome({Key key}) : super(key: key);

  @override
  _CategoriesHomeState createState() => new _CategoriesHomeState();
}

class _CategoriesHomeState extends State<CategoriesHome> {
  String _sortMethod;
  Future<List<Category>> categories;

  @override
  void initState() {
    this.categories = CategoriesManager.getAllCategoriesList(context);
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
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
                    child: FutureBuilder(
                      future: this.categories,
                      builder: (BuildContext context, AsyncSnapshot snapshot) {
                        if (snapshot.hasData) {
                          List<Category> categories = snapshot.data;
                          return CategoryList(
                              categories: categories,
                              sortType: _sortMethod,
                              refreshPage: this.refreshList);
                        } else if (snapshot.hasError) {
                          return Text("Error: ${snapshot.error}");
                        }
                        return Center(child: CircularProgressIndicator());
                      },
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

  Future<Null> refreshList() async {
    //TODO look in to updating this so that we don't have to re-query the categories, we could potentially use some global var for this (https://github.com/SCCapstone/decision_maker/issues/106)
    setState(() {
      this.categories = CategoriesManager.getAllCategoriesList(context);
    });
  }
}
