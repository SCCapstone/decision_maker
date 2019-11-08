import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_webview_plugin/flutter_webview_plugin.dart';

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
    _onUrlChanged = flutterWebviewPlugin.onUrlChanged.listen((String url) {
      if (mounted) {
        setState(() {
          int codeIndex = url.indexOf("code");

          if (url.startsWith("https://www.google.com") && codeIndex != -1) {
            //login was successful, parse and store code
            String code = url.substring(codeIndex + 5);

            //if storage is successful run these lines, otherwise we'll probably want to do sometype of error page
            flutterWebviewPlugin.close();
            Route route = MaterialPageRoute(builder: (context) => MyApp());
            Navigator.pushReplacement(context, route);
          }
        });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    String loginEndpoint = "https://pocket-poll.auth.us-east-2.amazoncognito.com/login?client_id=7eh4otm1r5p351d1u9j3h3rf1o&response_type=code&redirect_uri=https://google.com";

    return new WebviewScaffold(
        url: loginEndpoint,
        appBar: new AppBar(
          title: new Text("Login / Signup"),
        )
    );
  }
}