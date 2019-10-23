import 'package:flutter/material.dart';

// this is the first method run when app is run and it has to be in main.dart
void main() {
  runApp(
      new Container(
        //color: Colors.blue,
          decoration: new BoxDecoration(color: Colors.blue),
          child: new Center(
              child: new Directionality(
                  textDirection: TextDirection.ltr, // ltr means left to right
                  child: new Text("Hello World")
              )
          )
      )
  );
}