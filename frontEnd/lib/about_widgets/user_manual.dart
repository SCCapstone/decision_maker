import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/about_widgets/categories_info.dart';
import 'package:frontEnd/about_widgets/events_info.dart';
import 'package:frontEnd/about_widgets/general_info.dart';
import 'package:frontEnd/about_widgets/groups_info.dart';
import 'package:frontEnd/about_widgets/settings_info.dart';

class UserManual extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: AutoSizeText("User Manual",
            style: TextStyle(fontSize: 40), maxLines: 1, minFontSize: 20),
        centerTitle: true,
      ),
      body: Padding(
        padding: const EdgeInsets.fromLTRB(8.0, 0, 8.0, 0),
        child: Scrollbar(
          child: ListView(
            children: <Widget>[
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
                                        builder: (context) => SettingsInfo()));
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
                                      builder: (context) => CategoriesInfo()));
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