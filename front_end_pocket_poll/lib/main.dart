import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:front_end_pocket_poll/widgets/first_login.dart';
import 'package:front_end_pocket_poll/groups_widgets/groups_home.dart';
import 'package:front_end_pocket_poll/widgets/login_page.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'package:front_end_pocket_poll/widgets/internet_loss.dart';
import 'package:provider/provider.dart';

import 'imports/globals.dart';
import 'imports/user_tokens_manager.dart';

void main() => runApp(ChangeNotifierProvider<ThemeNotifier>(
      create: (_) => ThemeNotifier(Globals.darkTheme),
      child: AppStart(),
    ));

class AppStart extends StatelessWidget {
  /*
    Lil hacky, but otherwise when re-trying to gain connection the user can hit the back button
    and be brought back to that loss of internet page.
   */
  @override
  Widget build(BuildContext context) {
    //stop the app from going landscape
    SystemChrome.setPreferredOrientations(
        [DeviceOrientation.portraitUp, DeviceOrientation.portraitDown]);

    return InternetCheck();
  }
}

class InternetCheck extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
        // when app first loads, see if the user has internet or not
        color: Globals.pocketPollGrey,
        child: FutureBuilder<bool>(
            future: internetCheck(),
            builder: (BuildContext context, AsyncSnapshot snapshot) {
              if (!snapshot.hasData) {
                return Center(
                    child: CircularProgressIndicator(
                        valueColor: AlwaysStoppedAnimation<Color>(
                            Globals.pocketPollPrimary)));
              } else {
                if (!snapshot.data) {
                  return MaterialApp(
                      home: InternetLoss(), theme: Globals.darkTheme);
                } else {
                  return PocketPoll();
                }
              }
            }));
  }
}

class PocketPoll extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
        color: Globals.pocketPollGrey,
        child: FutureBuilder<bool>(
            future: hasValidTokensSet(context),
            builder: (BuildContext context, AsyncSnapshot snapshot) {
              final ThemeNotifier themeNotifier =
                  Provider.of<ThemeNotifier>(context);
              if (!snapshot.hasData) {
                return Center(
                    child: CircularProgressIndicator(
                        valueColor: AlwaysStoppedAnimation<Color>(
                            Globals.pocketPollPrimary)));
              } else {
                //If and only if the tokens are not valid or don't exist, open the login page.
                if (!snapshot.data) {
                  return MaterialApp(
                    home: LoginPage(),
                    theme: themeNotifier.getTheme(),
                    title: "Pocket Poll",
                  );
                } else {
                  if (Globals.user.firstLogin) {
                    return MaterialApp(
                      home: FirstLogin(),
                      theme: (Globals.user.appSettings.darkTheme)
                          ? Globals.darkTheme
                          : Globals.lightTheme,
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
              }
            }));
  }
}

// used to change the theme of the material app. Essentially rebuilds the entire app when theme changes
class ThemeNotifier with ChangeNotifier {
  ThemeData _themeData;

  ThemeNotifier(this._themeData);

  getTheme() => _themeData;

  setTheme(ThemeData themeData) async {
    _themeData = themeData;
    notifyListeners();
  }
}
