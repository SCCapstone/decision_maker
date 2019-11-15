import 'dart:async';
import 'package:flutter/material.dart';
import 'package:milestone_3/imports/categories_manager.dart';
import 'package:milestone_3/models/category.dart';
import 'imports/globals.dart';

class CategoriesHome extends StatefulWidget {
  final Future<List<Category>> categories;

  CategoriesHome({Key key, this.categories}) : super(key: key);

  @override
  _CategoriesHomeState createState() => new _CategoriesHomeState();
}

class _CategoriesHomeState extends State<CategoriesHome> {
  String _sortMethod;

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
        leading: IconButton(
          icon: navIcon,
          onPressed: () {
            // TODO link up with nav bar (https://github.com/SCCapstone/decision_maker/issues/78
          },
        ),
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
                          categories: categories, sortType: _sortMethod);
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
              onPressed: () {},
            )
          ],
        ),
      ),
    );
  }
}

class CategoryList extends StatefulWidget {
  final List<Category> categories;
  final String sortType;

  CategoryList({Key key, this.categories, this.sortType}) : super(key: key);

  @override
  _CategoryListState createState() => _CategoryListState();
}

class _CategoryListState extends State<CategoryList> {
  @override
  Widget build(BuildContext context) {
    if (widget.categories.length == 0) {
      return Center(
        child:
            Text("No categories found! Click \"New Category\" to create some!"),
      );
    } else {
      return Scrollbar(
        child: ListView.builder(
          shrinkWrap: true,
          itemCount: widget.categories.length,
          itemBuilder: (context, index) {
            bool defaultCategory = false;
            if (widget.categories[index].owner.isEmpty) {
              defaultCategory = true;
            }
            return CategoryRow(widget.categories[index], index, defaultCategory,
                onDelete: () => removeItem(index));
          },
        ),
      );
    }
  }

  void removeItem(int index) {
    // removes an item from the local list of categories used in the CategoryList state
    setState(() {
      widget.categories.remove(widget.categories[index]);
    });
  }
}

class CategoryRow extends StatelessWidget {
  final Category category;
  final VoidCallback onDelete;
  final int index;
  final bool defaultCategory;

  CategoryRow(this.category, this.index, this.defaultCategory, {this.onDelete});

  @override
  Widget build(BuildContext context) {
    if (defaultCategory) {
      return Container(
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: <Widget>[
            Text(
              category.categoryName,
              style: TextStyle(fontSize: 20),
            ),
            RaisedButton(
              child: Text(
                "Edit",
                style: TextStyle(),
              ),
              onPressed: () {},
            )
          ],
        ),
        decoration:
            new BoxDecoration(border: new Border(bottom: new BorderSide())),
      );
    } else {
      return Container(
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: <Widget>[
            Flexible(
              child: Text(
                category.categoryName,
                style: TextStyle(fontSize: 20),
              ),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                RaisedButton(
                  child: Text(
                    "Edit",
                    style: TextStyle(),
                  ),
                  onPressed: () {},
                ),
                Padding(
                  padding:
                      EdgeInsets.all(MediaQuery.of(context).size.height * .007),
                ),
                RaisedButton(
                  child: Text(
                    "Delete",
                    style: TextStyle(),
                  ),
                  /*
                    TODO delete the category from DB and if success, 
                     then remove from local list (https://github.com/SCCapstone/decision_maker/issues/97) 
                   */
                  onPressed: () {
                    this.onDelete(); // this deletes it from the local list
                  },
                ),
              ],
            ),
          ],
        ),
        decoration:
            new BoxDecoration(border: new Border(bottom: new BorderSide())),
      );
    }
  }
}
