import 'package:flutter/material.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/groups_widgets/group_page.dart';
import 'package:frontEnd/groups_widgets/groups_home.dart';
import 'package:frontEnd/utilities/utilities.dart';

class GroupRow extends StatelessWidget {
  final Group group;

  GroupRow(this.group);

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        Container(
          height: MediaQuery.of(context).size.height * .14,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              GestureDetector(
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (context) => GroupPage(
                              groupId: group.groupId,
                              groupName: group.groupName,
                            )),
                  ).then((_) => GroupsHome());
                },
                child: Container(
                  height: MediaQuery.of(context).size.width * .20,
                  width: MediaQuery.of(context).size.width * .20,
                  decoration: BoxDecoration(
                      image: DecorationImage(
                          image: getIconUrl(group.icon), fit: BoxFit.cover)),
                ),
              ),
              Padding(
                  padding: EdgeInsets.all(
                      MediaQuery.of(context).size.height * .002)),
              Expanded(
                child: GestureDetector(
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                          builder: (context) => GroupPage(
                                groupId: group.groupId,
                                groupName: group.groupName,
                              )),
                    ).then((_) => GroupsHome());
                  },
                  child: Container(
                    color: Colors.blueGrey.withOpacity(0.25),
                    height: MediaQuery.of(context).size.width * .20,
                    child: Center(
                      child: Text(
                        group.groupName,
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 25),
                      ),
                    ),
                  ),
                ),
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
