import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/main.dart';

class InternetLoss extends StatelessWidget {
  InternetLoss();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: Center(
        child: Column(
          children: <Widget>[
            Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .015),
            ),
            Icon(
              Icons.signal_cellular_connected_no_internet_4_bar,
              size: 40,
            ),
            Column(
              children: <Widget>[
                Text(
                  "No internet detected!\nClick button below to retry.",
                  style: TextStyle(fontSize: 30),
                ),
                IconButton(
                  icon: Icon(Icons.refresh),
                  iconSize: 40,
                  onPressed: () {
                    Navigator.pushAndRemoveUntil(
                        context,
                        MaterialPageRoute(
                          builder: (BuildContext context) => AppStart(),
                        ),
                        (Route<dynamic> route) => false);
                  },
                )
              ],
            )
          ],
        ),
      ),
    );
  }
}
