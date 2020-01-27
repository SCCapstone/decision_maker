import 'package:flutter/material.dart';
import 'package:frontEnd/groups_widgets/groups_home.dart';

import 'imports/user_tokens_manager.dart';
import 'imports/globals.dart';
import 'login_page.dart';
import 'dart:io' show Platform;

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    if (Platform.isAndroid) {
      Globals.android = true;
    } else {
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
                return Center(child: new CircularProgressIndicator());
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
                    home: GroupsHome(),
                  );
                }
              }
            }));
  }
}
