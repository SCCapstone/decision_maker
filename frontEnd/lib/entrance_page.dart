//Written by Jeffrey Arling
//Last updated on November 9th 2019 by Jeffrey Arling

import 'package:flutter/material.dart';

import 'add_value_pair.dart';
import 'main.dart';

class EntrancePage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    //TODO get cognito token and retrieve username
    String user = "user";
    return Scaffold(
      resizeToAvoidBottomPadding: false,
      appBar: AppBar(title: Text("Demo Entrance Page"),
        leading: InkWell(
          child: Icon(Icons.menu),
          onTap: () {
            Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => AddValuePair()),
            ).then((_) => runApp(MyApp()));
          }
        ),
      ),
      body: Center(
        child: Text("Hello "+ user),
      ),
    );

  }

}