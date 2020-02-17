import 'dart:io' show Platform;

import 'package:dynamic_theme/dynamic_theme.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/groups_widgets/groups_home.dart';
import 'package:frontEnd/login_page.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:provider/provider.dart';

import 'imports/globals.dart';
import 'imports/user_tokens_manager.dart';

void main() => runApp(MyApp());
//ChangeNotifierProvider<ThemeNotifier>(
//        create: (_) => ThemeNotifier(Globals.darkTheme),
//        child: MyApp(),
//      )

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    if (Platform.isAndroid) {
      Globals.android = true;
    } else {
      Globals.android = false;
    }
//    final ThemeNotifier themeNotifier = Provider.of<ThemeNotifier>(context);
//    return MaterialApp(
//      title: "Pocket Poll",
//      theme: themeNotifier.getTheme(),
//      home: HomePage(),
//    );
//    return DynamicTheme(
//      defaultBrightness: Brightness.dark,
//      data: (brightness) => Globals.darkTheme,
//      themedWidgetBuilder: (context, theme) {
//        return MaterialApp(
//          title: "Pocket Poll",
//          theme: theme,
//          home: HomePage(),
//        );
//      },
//    );
    return Container(
        //We use a FutureBuilder here since the display of the widget depends on
        //the asynchronous function hasValidTokensSet being able to fully execute
        //and return a Future<bool>.
        color: Theme.of(context).scaffoldBackgroundColor,
        child: FutureBuilder<bool>(
            future: hasValidTokensSet(),
            builder: (BuildContext context, AsyncSnapshot snapshot) {
              print("main");
              if (!snapshot.hasData) {
                return Center(child: new CircularProgressIndicator());
              } else {
                //If and only if the tokens are not valid or don't exist, open the login page.
                if (!snapshot.data) {
                  return DynamicTheme(
                    defaultBrightness: Brightness.dark,
                    data: (brightness) => Globals.darkTheme,
                    themedWidgetBuilder: (context, theme) {
                      return MaterialApp(
                        title: "Pocket Poll",
                        theme: theme,
                        home: SignInPage(),
                      );
                    },
                  );
                } else {
                  return DynamicTheme(
                    defaultBrightness: (Globals.user.appSettings.darkTheme)
                        ? Brightness.dark
                        : Brightness.light,
                    data: (brightness) => (Globals.user.appSettings.darkTheme)
                        ? Globals.darkTheme
                        : Globals.lightTheme,
                    themedWidgetBuilder: (context, theme) {
                      return MaterialApp(
                        title: "Pocket Poll",
                        theme: theme,
                        home: GroupsHome(),
                      );
                    },
                  );
                }
              }
            }));
  }
}

class HomePage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
        //We use a FutureBuilder here since the display of the widget depends on
        //the asynchronous function hasValidTokensSet being able to fully execute
        //and return a Future<bool>.
        color: Theme.of(context).scaffoldBackgroundColor,
        child: FutureBuilder<bool>(
            future: hasValidTokensSet(),
            builder: (BuildContext context, AsyncSnapshot snapshot) {
              print("main");
              if (!snapshot.hasData) {
                return Center(child: new CircularProgressIndicator());
              } else {
                //If and only if the tokens are not valid or don't exist, open the login page.
                if (!snapshot.data) {
                  return SignInPage();
                } else {
                  return GroupsHome();
                }
              }
            }));
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
