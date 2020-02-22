import 'dart:io' show Platform;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:frontEnd/groups_widgets/groups_home.dart';
import 'package:frontEnd/login_page.dart';
import 'package:provider/provider.dart';

import 'imports/globals.dart';
import 'imports/user_tokens_manager.dart';

void main() => runApp(ChangeNotifierProvider<ThemeNotifier>(
      create: (_) => ThemeNotifier(Globals.darkTheme),
      child: MyApp(),
    ));

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    if (Platform.isAndroid) {
      Globals.android = true;
    } else {
      Globals.android = false;
    }
    return Container(
        //We use a FutureBuilder here since the display of the widget depends on
        //the asynchronous function hasValidTokensSet being able to fully execute
        //and return a Future<bool>.
        color: Color(0xff303030),
        child: AnnotatedRegion(
          // make the bottom navigation bar black instead of default white
          value: SystemUiOverlayStyle.dark,
          child: FutureBuilder<bool>(
              future: hasValidTokensSet(),
              builder: (BuildContext context, AsyncSnapshot snapshot) {
                final ThemeNotifier themeNotifier =
                    Provider.of<ThemeNotifier>(context);
                if (!snapshot.hasData) {
                  return Center(
                      child: CircularProgressIndicator(
                          valueColor: AlwaysStoppedAnimation<Color>(
                              Color(0xff5ce080))));
                } else {
                  //If and only if the tokens are not valid or don't exist, open the login page.
                  if (!snapshot.data) {
                    return MaterialApp(
                      home: SignInPage(),
                      theme: themeNotifier.getTheme(),
                      title: "Pocket Poll",
                    );
                  } else {
                    return MaterialApp(
                      home: GroupsHome(),
                      theme: (Globals.user.appSettings.darkTheme)
                          ? Globals.darkTheme
                          : Globals.lightTheme,
                      title: "Pocket Poll",
                    );
                  }
                }
              }),
        ));
  }
}

class ThemeNotifier with ChangeNotifier {
  ThemeData _themeData;

  ThemeNotifier(this._themeData);

  getTheme() => _themeData;

  setTheme(ThemeData themeData) async {
    _themeData = themeData;
    notifyListeners();
  }
}
