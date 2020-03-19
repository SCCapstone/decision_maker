import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';
import 'package:flutter_html/flutter_html.dart';
import 'package:frontEnd/about_widgets/categories_info.dart';
import 'package:frontEnd/about_widgets/events_info.dart';
import 'package:frontEnd/about_widgets/general_info.dart';
import 'package:frontEnd/about_widgets/groups_info.dart';
import 'package:frontEnd/about_widgets/html_widgets.dart';
import 'package:frontEnd/about_widgets/settings_info.dart';
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
          padding: const EdgeInsets.fromLTRB(8.0, 0, 8.0, 0),
          child: Scrollbar(
            child: ListView(
              controller: this.scrollController,
              children: <Widget>[
                Padding(
                  padding: const EdgeInsets.fromLTRB(15.0, 8, 15.0, 8),
                  child: Container(
                    height: MediaQuery.of(context).size.height * .25,
                    child: Column(
                      children: <Widget>[
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
                    "App Info and Help",
                    style: TextStyle(
                        fontSize: 30, decoration: TextDecoration.underline),
                    minFontSize: 14,
                    maxLines: 1,
                  ),
                ),
                Wrap(
                  runSpacing: -10,
                  children: <Widget>[
                    Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: <Widget>[
                          Expanded(
                            child: GestureDetector(
                              onTap: () {
                                Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                        builder: (context) => GeneralInfo()));
                              },
                              child: AutoSizeText(
                                "General",
                                minFontSize: 12,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(fontSize: 20),
                              ),
                            ),
                          ),
                          Container(
                              width: MediaQuery.of(context).size.width * .20,
                              child: IconButton(
                                icon: Icon(Icons.keyboard_arrow_right),
                                onPressed: () {
                                  Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                          builder: (context) => GeneralInfo()));
                                },
                              )),
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: <Widget>[
                          Expanded(
                            child: GestureDetector(
                              onTap: () {
                                Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                        builder: (context) => SettingsInfo()));
                              },
                              child: AutoSizeText(
                                "User Settings",
                                minFontSize: 12,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(fontSize: 20),
                              ),
                            ),
                          ),
                          Container(
                              width: MediaQuery.of(context).size.width * .20,
                              child: IconButton(
                                icon: Icon(Icons.keyboard_arrow_right),
                                onPressed: () {
                                  Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                          builder: (context) =>
                                              SettingsInfo()));
                                },
                              )),
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: <Widget>[
                          Expanded(
                            child: GestureDetector(
                              onTap: () {
                                Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                        builder: (context) =>
                                            CategoriesInfo()));
                              },
                              child: AutoSizeText(
                                "Categories",
                                minFontSize: 12,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(fontSize: 20),
                              ),
                            ),
                          ),
                          Container(
                              width: MediaQuery.of(context).size.width * .20,
                              child: IconButton(
                                icon: Icon(Icons.keyboard_arrow_right),
                                onPressed: () {
                                  Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                          builder: (context) =>
                                              CategoriesInfo()));
                                },
                              )),
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: <Widget>[
                          Expanded(
                            child: GestureDetector(
                              onTap: () {
                                Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                        builder: (context) => GroupsInfo()));
                              },
                              child: AutoSizeText(
                                "Groups",
                                minFontSize: 12,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(fontSize: 20),
                              ),
                            ),
                          ),
                          Container(
                              width: MediaQuery.of(context).size.width * .20,
                              child: IconButton(
                                icon: Icon(Icons.keyboard_arrow_right),
                                onPressed: () {
                                  Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                          builder: (context) => GroupsInfo()));
                                },
                              )),
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: <Widget>[
                          Expanded(
                            child: GestureDetector(
                              onTap: () {
                                Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                        builder: (context) => EventsInfo()));
                              },
                              child: AutoSizeText(
                                "Events",
                                minFontSize: 12,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(fontSize: 20),
                              ),
                            ),
                          ),
                          Container(
                              width: MediaQuery.of(context).size.width * .20,
                              child: IconButton(
                                icon: Icon(Icons.keyboard_arrow_right),
                                onPressed: () {
                                  Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                          builder: (context) => EventsInfo()));
                                },
                              )),
                        ],
                      ),
                    ),
                  ],
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
    // make sure that the widget is visible
    if (keyContext != null) {
      // find the height of the list view inside the expansion tile
      final box = keyContext.findRenderObject() as RenderBox;
      print(box.size.height);
      this.scrollController.animateTo(
          isExpanded ? (box.size.height) : previousOffset,
          duration: Duration(milliseconds: 500),
          curve: Curves.linear);
    }
  }
}
