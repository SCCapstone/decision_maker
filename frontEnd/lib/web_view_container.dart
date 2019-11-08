import 'package:flutter/material.dart';
import 'package:flutter_webview_plugin/flutter_webview_plugin.dart';

class WebViewContainer extends StatefulWidget {
  final url;
  WebViewContainer(this.url);
  @override
  createState() => _WebViewContainerState(this.url);
}

class _WebViewContainerState extends State<WebViewContainer> {
  var _url;
  _WebViewContainerState(this._url);

  @override
  Widget build(BuildContext context) {
    return WebviewScaffold(
      url: this._url,
      appBar: new AppBar(
        title: const Text('Login / Signup'),
      ),
      withZoom: true,
      withLocalStorage: true,
      hidden: true,
      initialChild: Container(
        color: Colors.greenAccent,
        child: const Center(
          child: Text('Waiting.....'),
        ),
      ),
    );
  }
}