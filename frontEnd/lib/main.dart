import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:milestone_3/AddValuePair.dart';
import 'package:http/http.dart' as http;

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  final String pairsApiEndpoint = "https://9zh1udqup3.execute-api.us-east-2.amazonaws.com/beta";

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

  Future<List<Pair>> getAllPairs() async {
    http.Response response = await http.get(this.pairsApiEndpoint);

    if (response.statusCode == 200) {
      List responseJson = json.decode(response.body);
      return responseJson.map((m) => new Pair.fromJson(m)).toList();
    } else {
      throw Exception("Failed to load value pairs from the database.");
    }
  }

  Future<Widget> getAllPairsWidget() async {
    List<Pair> allPairs = await this.getAllPairs();
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

class Pair {
  final String key;
  final String value;

  Pair({this.key, this.value});

  factory Pair.fromJson(Map<String, dynamic> json) {
    return Pair(
      key: json['key'],
      value: json['value'],
    );
  }
}