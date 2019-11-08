import 'package:flutter/material.dart';

import 'add_value_pair.dart';
import 'imports/dev_testing_manager.dart';
import 'imports/pair.dart';
import 'imports/user_tokens_manager.dart';
import 'login_page.dart';
import 'web_view_container.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  final WebViewContainer webViewContainer = new WebViewContainer("https://pocket-poll.auth.us-east-2.amazoncognito.com/login?client_id=7eh4otm1r5p351d1u9j3h3rf1o&response_type=code&redirect_uri=https://google.com");

  @override
  Widget build(BuildContext context) {
    if (!hasValidTokensSet()) {
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
        home: MyAppContents(dbPairs: this.getAllPairsWidget()),
      );
    }
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
      body: Center(
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