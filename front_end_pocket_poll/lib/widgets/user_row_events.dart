import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/models/favorite.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';

class UserRowEvents extends StatelessWidget {
  final String displayName;
  final String username;
  final String icon;

  UserRowEvents(this.displayName, this.username, this.icon);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        showUserImage(
            new Favorite.debug(this.username, this.displayName, this.icon),
            context);
      },
      child: Container(
        height: MediaQuery.of(context).size.height * .07,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: <Widget>[
            Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .015),
            ),
            Container(
              height: MediaQuery.of(context).size.height * .2,
              width: MediaQuery.of(context).size.width * .15,
              decoration: BoxDecoration(
                  image: DecorationImage(
                      image: getUserIconImage(this.icon), fit: BoxFit.cover)),
            ),
            Expanded(
              child: Center(
                child: AutoSizeText(
                  this.displayName,
                  maxLines: 1,
                  minFontSize: 11,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(fontSize: 20),
                ),
              ),
            ),
          ],
        ),
        decoration: BoxDecoration(
            border: Border(bottom: BorderSide(color: getBorderColor()))),
      ),
    );
  }
}
