import 'dart:io' show Platform;

import 'package:flutter/material.dart';
import 'package:frontEnd/groups_widgets/groups_home.dart';
import 'package:frontEnd/login_page.dart';

import 'imports/globals.dart';
import 'imports/user_tokens_manager.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    if (Platform.isAndroid) {
      Globals.android = true;
    } else {
      Globals.android = false;
    }
    return MaterialApp(
      title: "Pocket Poll",
      theme: ThemeData(
        primarySwatch: Colors.green,
      ),
      home: HomePage(),
    );
  }
}

class HomePage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
          //We use a FutureBuilder here since the display of the widget depends on
          //the asynchronous function hasValidTokensSet being able to fully execute
          //and return a Future<bool>.
          child: FutureBuilder<bool>(
              future: hasValidTokensSet(),
              builder: (BuildContext context, AsyncSnapshot snapshot) {
                //If the function to set the hasValidTokens boolean hasn't finished
                //yet, then display a circular progress indicator.
                if (!snapshot.hasData) {
                  return Center(child: new CircularProgressIndicator());
                } else {
                  //If the tokens are not valid or don't exist, open the login page.
                  //Otherwise, skip the login page.
                  if (!snapshot.data) {
                    return SignInPage();
                  } else {
                    return GroupsHome();
                  }
                }
              })),
    );
  }
}
