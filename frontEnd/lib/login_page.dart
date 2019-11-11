import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_webview_plugin/flutter_webview_plugin.dart';
import 'package:milestone_3/imports/user_tokens_manager.dart';
import 'imports/user_tokens_manager.dart';

import 'main.dart';

class LoginScreen extends StatefulWidget {
  @override
  _LoginScreenState createState() => new _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final flutterWebviewPlugin = new FlutterWebviewPlugin();

  StreamSubscription _onDestroy;
  StreamSubscription<String> _onUrlChanged;
  StreamSubscription<WebViewStateChanged> _onStateChanged;

  String token;

  @override
  void dispose() {
    // Every listener should be canceled, the same should be done with this stream.
    _onDestroy.cancel();
    _onUrlChanged.cancel();
    _onStateChanged.cancel();
    flutterWebviewPlugin.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();

    flutterWebviewPlugin.close();

    // Add a listener to on destroy WebView, so you can make came actions.
    _onDestroy = flutterWebviewPlugin.onDestroy.listen((_) {
      print("destroy");
    });

    _onStateChanged =
        flutterWebviewPlugin.onStateChanged.listen((WebViewStateChanged state) {
          print("onStateChanged: ${state.type} ${state.url}");
        });

    // Add a listener to on url changed
    _onUrlChanged = flutterWebviewPlugin.onUrlChanged.listen((String url) async {
      int codeIndex = url.indexOf("code");

      if (url.startsWith(redirectUri) && codeIndex != -1) {
        //login was successful, parse and store code
        String code = url.substring(codeIndex + 5);
        await getUserTokens(code);
        //setState is called after the asynchronous function so that the
        //state is updated after the execution of said function is complete.
        if (mounted) {
          setState(() {
            if (gotTokens) {
              //if storage is successful run these lines, otherwise we'll probably want to do some type of error page
              flutterWebviewPlugin.close();
              Route route = MaterialPageRoute(builder: (context) => MyApp());
              Navigator.pushReplacement(context, route);
            } else {
              flutterWebviewPlugin.close(); //TODO: proper error page (https://github.com/SCCapstone/decision_maker/issues/87)
            }
          });
        }
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    String loginUrl = userPoolUrl + loginEndpoint + "client_id=" + clientId + "&response_type=code&redirect_uri=" + redirectUri;

    return new WebviewScaffold(
        url: loginUrl,
        appBar: new AppBar(
          title: new Text("Login / Signup"),
        )
    );
  }
}