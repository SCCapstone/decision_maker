import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';

import 'about_descriptions.dart';

class GroupsInfo extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: AutoSizeText("Groups",
            style: TextStyle(fontSize: 40), maxLines: 1, minFontSize: 20),
        centerTitle: true,
      ),
      key: Key("groups_info:scaffold"),
      body: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Scrollbar(
          child: ListView(
              shrinkWrap: true,
              physics: ClampingScrollPhysics(),
              children: <Widget>[
                AboutDescriptions.groups,
              ]),
        ),
      ),
    );
  }
}
