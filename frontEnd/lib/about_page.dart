import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:package_info/package_info.dart';

class AboutPage extends StatefulWidget {
  @override
  _AboutPageState createState() => _AboutPageState();
}

class _AboutPageState extends State<AboutPage> {
  String appName;
  String packageName;
  String version;
  String buildNumber;
  bool loading;
  String userSettingsString;
  final ScrollController scrollController = ScrollController();
  final GlobalKey expansionTileKey = GlobalKey();
  double previousOffset;

  @override
  void initState() {
    this.loading = true;
    getPackageInfo();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    if (this.loading) {
      return Scaffold(
        appBar: AppBar(
          title: AutoSizeText("About",
              style: TextStyle(fontSize: 40), maxLines: 1, minFontSize: 20),
          centerTitle: true,
        ),
        body: CircularProgressIndicator(),
      );
    } else {
      return Scaffold(
        appBar: AppBar(
          title: AutoSizeText("About",
              style: TextStyle(fontSize: 40), maxLines: 1, minFontSize: 20),
          centerTitle: true,
        ),
        body: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Scrollbar(
            child: ListView(
              controller: this.scrollController,
              children: <Widget>[
                Padding(
                  padding: const EdgeInsets.fromLTRB(15.0, 8, 15.0, 8),
                  child: Container(
                    height: MediaQuery.of(context).size.height * .3,
                    child: Column(
                      children: <Widget>[
                        AutoSizeText(
                          this.appName,
                          style: TextStyle(fontSize: 30),
                          minFontSize: 14,
                          maxLines: 1,
                        ),
                        Padding(
                          padding: EdgeInsets.all(
                              MediaQuery.of(context).size.height * .004),
                        ),
                        Expanded(
                          child: Image(
                            // TODO put app icon here
                            image: AssetImage('assets/images/placeholder.jpg'),
                            fit: BoxFit.cover,
                          ),
                        )
                      ],
                    ),
                  ),
                ),
                Center(
                  child: AutoSizeText(
                    "Version ${this.version}",
                    style: TextStyle(fontSize: 24),
                    minFontSize: 14,
                    maxLines: 1,
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    children: <Widget>[
                      Expanded(
                        child: AutoSizeText(
                          "View Privacy Policy & Licensing Info",
                          minFontSize: 12,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(fontSize: 20),
                        ),
                      ),
                      Container(
                          width: MediaQuery.of(context).size.width * .20,
                          child: IconButton(
                            icon: Icon(Icons.keyboard_arrow_right),
                            onPressed: () {
                              // TODO
                            },
                          )),
                    ],
                  ),
                ),
                Padding(
                  padding:
                      EdgeInsets.all(MediaQuery.of(context).size.height * .008),
                ),
                Center(
                  child: AutoSizeText(
                    "App Tips and Help",
                    style: TextStyle(
                        fontSize: 30, decoration: TextDecoration.underline),
                    minFontSize: 14,
                    maxLines: 1,
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: ExpansionTile(
                    key: expansionTileKey,
                    onExpansionChanged: (isExpanded) {
                      if (isExpanded) previousOffset = scrollController.offset;
                      scrollToSelectedContent(
                          isExpanded, previousOffset, expansionTileKey);
                    },
                    title: Text("User Settings"),
                    children: <Widget>[
                      ListView(shrinkWrap: true, children: <Widget>[
                        Text(
                          this.userSettingsString,
                          style: TextStyle(fontSize: 15),
                        ),
                        Padding(
                          padding: EdgeInsets.all(
                              MediaQuery.of(context).size.height * .009),
                        ),
                      ]),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }
  }

  void getPackageInfo() async {
    PackageInfo packageInfo = await PackageInfo.fromPlatform();
    this.userSettingsString =
        await rootBundle.loadString('assets/text/userSettings.txt');

    setState(() {
      this.loading = false;
      this.appName = packageInfo.appName;
      this.packageName = packageInfo.packageName;
      this.version = packageInfo.version;
      this.buildNumber = packageInfo.buildNumber;
    });
  }

  void scrollToSelectedContent(
      bool isExpanded, double previousOffset, GlobalKey myKey) {
    final keyContext = myKey.currentContext;

    if (keyContext != null) {
      // make sure that the widget is visible
      final box = keyContext.findRenderObject() as RenderBox;
      this.scrollController.animateTo(
          isExpanded ? (box.size.height) : previousOffset,
          duration: Duration(milliseconds: 500),
          curve: Curves.linear);
    }
  }
}
