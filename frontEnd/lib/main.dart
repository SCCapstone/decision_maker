import 'dart:async';

import 'package:flutter/material.dart';
import 'categories_home.dart';

import 'add_value_pair.dart';
import 'create_or_edit_category.dart';
import 'imports/dev_testing_manager.dart';
import 'imports/pair.dart';
import 'imports/user_tokens_manager.dart';
import 'imports/globals.dart';
import 'login_page.dart';
import 'dart:io' show Platform;

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {

  @override
  Widget build(BuildContext context) {
    if(Platform.isAndroid){
      Globals.android = true;
    }
    else{
      Globals.android = false;
    }
    return new Container(
      //We use a FutureBuilder here since the display of the widget depends on
      //the asynchronous function hasValidTokensSet being able to fully execute
      //and return a Future<bool>.
      child: new FutureBuilder<bool>(
        future: hasValidTokensSet(),
        builder: (BuildContext context, AsyncSnapshot snapshot) {
          //If the function to set the hasValidTokens boolean hasn't finished
          //yet, then display a circular progress indicator.
          if (!snapshot.hasData) {
            return Center(
                child: new CircularProgressIndicator()
                );
          } else {
            //If the tokens are not valid or don't exist, open the login page.
            //Otherwise, skip the login page.
            if (!snapshot.data) {
              return MaterialApp(
                title: 'Flutter Demo',
                theme: ThemeData(
                  primarySwatch: Colors.green,
                ),
                home: LoginScreen(),
              );
            } else {
              return MaterialApp(
                title: 'Flutter Demo',
                theme: ThemeData(
                  primarySwatch: Colors.green,
                ),
                  home: CategoriesHome(),
//                home: MyAppContents(dbPairs: this.getAllPairsWidget()),
              );
            }
          }
        }
      )
    );
  }

  Future<Widget> getAllPairsWidget() async {
    List<Pair> allPairs = await DevTestingManager.getAllPairs();
    return new Column(children: allPairs.map((pair) => new Text(pair.key + ": " + pair.value)).toList());
  }
}

class MyAppContents extends StatelessWidget {
  final Future<Widget> dbPairs;

  MyAppContents({Key key, this.dbPairs}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("View Entered Key Value Pairs"),
      ),
      body: Column(
        children: [
          Center(
            child: Padding(
              padding: EdgeInsets.all(20.0),
              child: Column(
                //mainAxisAlignment: MainAxisAlignment.center,
                //crossAxisAlignment: CrossAxisAlignment.center,
                children: <Widget>[
                  Text("Here are all the value pairs that have been entered:"),
                  FutureBuilder<Widget>(
                    future: this.dbPairs,
                    builder: (context, snapshot) {
                      if (snapshot.hasData) {
                        return snapshot.data;
                      } else if (snapshot.hasError) {
                        return Text("${snapshot.error}");
                      }

                      return CircularProgressIndicator();
                    },
                  ),
                ],
              ),
            ),
          ),
          RaisedButton.icon(
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => CreateOrEditCategory(isEdit: false)),
                ).then((_) => runApp(MyApp()));
              },
              icon: Icon(Icons.add),
              label: Text("Add New Category")
          )
        ],
      ),
      floatingActionButton: FloatingActionButton(
        child: Icon(Icons.add),
        onPressed: () {
          // Navigate to second route when tapped.
          Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => AddValuePair()),
          ).then((_) => runApp(MyApp()));
        },
      ),
    );
  }
}