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
          GestureDetector(
            onTap: () {
              showUserImage(getIconUrl(icon), context);
            },
            child: Container(
              height: MediaQuery.of(context).size.height * .2,
              width: MediaQuery.of(context).size.width * .15,
              decoration: BoxDecoration(
                  image: DecorationImage(
                      image: getIconUrl(icon), fit: BoxFit.cover)),
            ),
          ),
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
