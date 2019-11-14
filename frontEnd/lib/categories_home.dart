import 'dart:async';
import 'package:flutter/material.dart';
import 'package:milestone_3/models/category.dart';
import 'imports/globals.dart';

class CategoriesHome extends StatefulWidget {
  @override
  _CategoriesHomeState createState() => new _CategoriesHomeState();
}

class _CategoriesHomeState extends State<CategoriesHome> {
  @override
  void dispose() {
    // Every listener should be canceled, the same should be done with this stream.
    super.dispose();
  }

  @override
  void initState() {
    // TODO fetch from the database all categories the user has
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
            onPressed: () {},
          )
        ],
        leading: IconButton(
          icon: navIcon,
          onPressed: () {},
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
                  future: null,
                  builder: (BuildContext context, AsyncSnapshot snapshot) {
                    if (!snapshot.hasData) {
                      // TODO remove once the endpoint is working
                      List<Category> categories = new List<Category>();
                      for (int i = 0; i < 15; i++) {
                        Map<int, String> choices = new Map<int, String>();
                        Map<int, String> groups = new Map<int, String>();
                        Category category = new Category.debug(
                            i, "Category", choices, groups, 0, i);
                        categories.add(category);
                      }
                      return CategoryList(categories: categories);
                    } else {
                      if (!snapshot.data) {
                        return Text("Error");
                      } else {
                        return Expanded(
                          child: CategoryList(),
                        );
                      }
                    }
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

  CategoryList({Key key, this.categories}) : super(key: key);

  @override
  _CategoryListState createState() => _CategoryListState();
}

class _CategoryListState extends State<CategoryList> {
  @override
  Widget build(BuildContext context) {
    return Scrollbar(
      child: ListView.builder(
        shrinkWrap: true,
        itemCount: widget.categories.length,
        itemBuilder: (context, index) {
          return CategoryRow(widget.categories[index], index, true,
              onDelete: () => removeItem(index));
        },
      ),
    );
  }

  void removeItem(int index) {
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
                  // TODO delete the category and if success, then remove from list
                  onPressed: () {
                    this.onDelete();
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
