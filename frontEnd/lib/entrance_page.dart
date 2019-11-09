//Written by Jeffrey Arling
//Last updated on November 9th 2019

import 'package:flutter/material.dart';

import 'imports/globals.dart';

import 'add_value_pair.dart';
import 'main.dart';

class EntrancePage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    //TODO get cognito token and retrieve username
    String user = "user";
    return Scaffold(
      resizeToAvoidBottomPadding: false,
      appBar: AppBar(title: Text("Demo Entrance Page")
      ),
      body: Container(
        width: MediaQuery.of(context).size.width,
        height: MediaQuery.of(context).size.height,
        child: Column(
          children: <Widget>[
            Padding(
              padding: EdgeInsets.all(25.0),
            ),
            Text("Hello "+ user),
            Padding(
              padding: EdgeInsets.all(5.0),
            ),
            SizedBox(
              width: 300.0,
              height: 50.0,
              child: RaisedButton(
                color: Globals.secondaryColor,
                textColor: Colors.white,
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(builder: (context) => AddValuePair()),
                  ).then((_) => runApp(MyApp()));
                },
                child: Text("Add pairs")
              ),
            ),
          ],
      ),
      ),

    );

  }

}