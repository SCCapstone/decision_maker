import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:package_info/package_info.dart';
import 'package:url_launcher/url_launcher.dart';

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
              children: <Widget>[
                Padding(
                  padding: const EdgeInsets.fromLTRB(15.0, 8, 15.0, 8),
                  child: Container(
                    height: MediaQuery.of(context).size.height * .25,
                    child: Column(
                      children: <Widget>[
                        Expanded(
                          child: Image(
                            image: AssetImage("assets/images/pollSplash.png"),
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
                  padding:
                      EdgeInsets.all(MediaQuery.of(context).size.height * .01),
                ),
                InkWell(
                  child: Center(
                    child: AutoSizeText(
                      "Click here to view our privacy policy.",
                      maxLines: 1,
                      minFontSize: 14,
                      style: TextStyle(
                          color: Colors.blue,
                          fontSize: 20,
                          decoration: TextDecoration.underline),
                    ),
                  ),
                  onTap: () async {
                    if (await canLaunch(Globals.privacyPolicyUrl)) {
                      await launch(Globals.privacyPolicyUrl);
                    } else {
                      throw 'Could not launch ${Globals.privacyPolicyUrl}';
                    }
                  },
                ),
                Padding(
                  padding:
                      EdgeInsets.all(MediaQuery.of(context).size.height * .008),
                ),
                InkWell(
                  child: Center(
                    child: AutoSizeText(
                      "Click here to view our terms and conditions.",
                      maxLines: 1,
                      minFontSize: 14,
                      style: TextStyle(
                          color: Colors.blue,
                          fontSize: 20,
                          decoration: TextDecoration.underline),
                    ),
                  ),
                  onTap: () async {
                    if (await canLaunch(Globals.termsUrl)) {
                      await launch(Globals.termsUrl);
                    } else {
                      throw 'Could not launch ${Globals.termsUrl}';
                    }
                  },
                ),
                Padding(
                  padding:
                      EdgeInsets.all(MediaQuery.of(context).size.height * .008),
                ),
                Center(
                  child: AutoSizeText(
                    "Built Using",
                    minFontSize: 12,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontSize: 20),
                  ),
                ),
                InkWell(
                  child: Center(
                    child: AutoSizeText(
                      "Flutter",
                      maxLines: 1,
                      minFontSize: 14,
                      style: TextStyle(
                          color: Colors.blue,
                          fontSize: 20,
                          decoration: TextDecoration.underline),
                    ),
                  ),
                  onTap: () async {
                    if (await canLaunch(Globals.flutterUrl)) {
                      await launch(Globals.flutterUrl);
                    } else {
                      throw 'Could not launch ${Globals.flutterUrl}';
                    }
                  },
                ),
                Padding(
                  padding:
                      EdgeInsets.all(MediaQuery.of(context).size.height * .01),
                ),
                Center(
                  child: AutoSizeText(
                    "Hosted On",
                    minFontSize: 12,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontSize: 20),
                  ),
                ),
                InkWell(
                  child: Center(
                    child: AutoSizeText(
                      "Amazon Web Services (AWS)",
                      maxLines: 1,
                      minFontSize: 14,
                      style: TextStyle(
                          color: Colors.blue,
                          fontSize: 20,
                          decoration: TextDecoration.underline),
                    ),
                  ),
                  onTap: () async {
                    if (await canLaunch(Globals.awsUrl)) {
                      await launch(Globals.awsUrl);
                    } else {
                      throw 'Could not launch ${Globals.awsUrl}';
                    }
                  },
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
}
