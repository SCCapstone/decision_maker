import 'package:flutter/material.dart';

import 'add_value_pair.dart';
import 'includes/dev_testing_manager.dart';
import 'includes/pair.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.green,
      ),
      home: MyAppContents(dbPairs: this.getAllPairsWidget()),
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