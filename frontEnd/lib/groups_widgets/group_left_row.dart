import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/utilities/utilities.dart';

class GroupLeftRow extends StatefulWidget {
  final Group group;
  final Function refreshGroups;

  GroupLeftRow(this.group, {this.refreshGroups});

  @override
  _GroupLeftRowState createState() => _GroupLeftRowState();
}

class _GroupLeftRowState extends State<GroupLeftRow> {
  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        Container(
          height: MediaQuery.of(context).size.height * .14,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Container(
                height: MediaQuery.of(context).size.width * .20,
                width: MediaQuery.of(context).size.width * .20,
                decoration: BoxDecoration(
                    image: DecorationImage(
                        image: getIconUrl(widget.group.icon),
                        fit: BoxFit.cover)),
              ),
              Padding(
                  padding: EdgeInsets.all(
                      MediaQuery.of(context).size.height * .002)),
              Expanded(
                child: Container(
                  color: Colors.blueGrey.withOpacity(0.25),
                  height: MediaQuery.of(context).size.width * .20,
                  child: Center(
                    child: AutoSizeText(
                      widget.group.groupName,
                      minFontSize: 12,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 25),
                    ),
                  ),
                ),
              ),
              Padding(
                  padding: EdgeInsets.all(
                      MediaQuery.of(context).size.height * .002)),
              RaisedButton(
                child: Text("Rejoin"),
                onPressed: () {
                  // TODO rejoin the group.
                  /*
                    TODO if success add back to users group mapping, delete this group from groups left mapping,
                     and refresh the group home page
                   */
                },
              )
            ],
          ),
        ),
        Padding(
          padding: EdgeInsets.all(MediaQuery.of(context).size.height * .004),
        ),
      ],
    );
  }
}
