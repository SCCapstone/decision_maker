import 'package:flutter/material.dart';
import 'package:frontEnd/utilities/utilities.dart';

class UserRowEvents extends StatelessWidget {
  final String displayName;
  final String username;
  final String icon;

  UserRowEvents(this.displayName, this.username, this.icon);

  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * .07,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Padding(
            padding: EdgeInsets.all(MediaQuery.of(context).size.height * .015),
          ),
          Image(image: getIconUrl(icon), fit: BoxFit.fitHeight),
          Expanded(
            child: Center(
              child: Text(
                this.displayName,
                style: TextStyle(fontSize: 20),
              ),
            ),
          ),
        ],
      ),
      decoration: BoxDecoration(
          border: Border(bottom: BorderSide(color: getBorderColor()))),
    );
  }
}
