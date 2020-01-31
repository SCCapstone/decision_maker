import 'dart:async';
import 'package:flutter/material.dart';
import 'package:frontEnd/categories_widgets/categories_create.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/imports/globals.dart';
import 'categories_list.dart';

class CategoriesHome extends StatefulWidget {
  Future<List<Category>> categories;

  CategoriesHome({Key key, this.categories}) : super(key: key);

  @override
  _CategoriesHomeState createState() => new _CategoriesHomeState();
}

class _CategoriesHomeState extends State<CategoriesHome> {
  String _sortMethod;

  @override
  void initState() {
    widget.categories = CategoriesManager.getAllCategoriesList();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    Icon navIcon;
    if (Globals.android) {
      navIcon = new Icon(Icons.dehaze);
    } else {
      navIcon = new Icon(Icons.arrow_back);
    }
    return new Scaffold(
      appBar: new AppBar(
        centerTitle: true,
        title: Text(
          "Categories",
          style: TextStyle(fontSize: 35),
        ),
        actions: <Widget>[
          IconButton(
            icon: Icon(Icons.sort),
            onPressed: () {
              // can implement a variable that has the sort type, then setState here
              // TODO implement a sorting algorithm (https://github.com/SCCapstone/decision_maker/issues/31)
            },
          )
        ],
//        leading: IconButton(
//          icon: navIcon,
//          onPressed: () {
//            // TODO link up with nav bar (https://github.com/SCCapstone/decision_maker/issues/78
//          },
//        ),
      ),
      body: Center(
        child: Column(
          children: <Widget>[
            Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .015),
            ),
            new Container(
              width: MediaQuery.of(context).size.width * .80,
              height: MediaQuery.of(context).size.height * .60,
              child: new Container(
                child: FutureBuilder(
                  future: widget.categories,
                  builder: (BuildContext context, AsyncSnapshot snapshot) {
                    if (snapshot.hasData) {
                      List<Category> categories = snapshot.data;
                      return CategoryList(
                          categories: categories,
                          sortType: _sortMethod,
                          refreshPage: this.refreshPage);
                    } else if (snapshot.hasError) {
                      return Text("Error: ${snapshot.error}");
                    }
                    return Center(child: CircularProgressIndicator());
                  },
                ),
              ),
            ),
            Padding(
              padding: EdgeInsets.all(MediaQuery.of(context).size.height * .01),
            ),
            Divider(
              color: Colors.black,
            ),
            Padding(
              padding: EdgeInsets.all(MediaQuery.of(context).size.height * .01),
            ),
            RaisedButton(
                child: Text(
                  "New Category",
                  style: TextStyle(fontSize: 30),
                ),
                onPressed: () {
                  // Navigate to second route when tapped.
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (context) =>
                            CreateCategory()),
                  ).then((_) => this.refreshPage());
                })
          ],
        ),
      ),
    );
  }

  void refreshPage() {
    //TODO look in to updating this so that we don't have to re-query the categories, we could potentially use some global var for this (https://github.com/SCCapstone/decision_maker/issues/106)
    setState(() {
      widget.categories = CategoriesManager.getAllCategoriesList();
    });
  }
}
