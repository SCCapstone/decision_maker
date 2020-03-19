import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/groups_widgets/group_page.dart';
import 'package:frontEnd/models/user_group.dart';
import 'package:frontEnd/utilities/utilities.dart';

class GroupRow extends StatefulWidget {
  final UserGroup group;
  final Function refreshGroups;

  GroupRow(this.group, {this.refreshGroups});

  @override
  _GroupRowState createState() => _GroupRowState();
}

class _GroupRowState extends State<GroupRow> {

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
              GestureDetector(
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (context) => GroupPage(
                              groupId: widget.group.groupId,
                              groupName: widget.group.groupName,
                            )),
                  ).then((val) {
                    Globals.currentGroup = null;
                    // TODO figure out a better way to refresh without making unnecessary API calls
                    this.widget.refreshGroups();
                  });
                },
                child: Container(
                  height: MediaQuery.of(context).size.width * .20,
                  width: MediaQuery.of(context).size.width * .20,
                  decoration: BoxDecoration(
                      image: DecorationImage(
                          image: getIconUrl(widget.group.icon),
                          fit: BoxFit.cover)),
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
                                groupId: widget.group.groupId,
                                groupName: widget.group.groupName,
                              )),
                    ).then((val) {
                      Globals.currentGroup = null;
                      // TODO figure out a better way to refresh without making unnecessary API calls
                      this.widget.refreshGroups();
                    });
                  },
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
              ),
              GestureDetector(
                onTap: () {
                  // doing this to try and help users who have a hard time clicking the icon
                  setState(() {
                    // TODO mute the group
                    this.widget.group.muted = !this.widget.group.muted;
                  });
                },
                child: Container(
                  // for some reason the color has to be set for the on tap above to work?
                  color: Theme.of(context).scaffoldBackgroundColor,
                  height: MediaQuery.of(context).size.width * .20,
                  width: MediaQuery.of(context).size.width * .15,
                  child: Center(
                    child: Wrap(
                        spacing: (this.widget.group.eventsUnseen.length > 0)
                            ? -25
                            : 0,
                        direction: Axis.vertical,
                        children: <Widget>[
                          IconButton(
                            icon: (this.widget.group.muted)
                                ? Icon(Icons.notifications_off)
                                : Icon(Icons.notifications),
                            color: Colors.blueAccent,
                            tooltip:
                                (this.widget.group.muted) ? "Unmute" : "Mute",
                            onPressed: () {
                              setState(() {
                                // TODO mute the group
                                this.widget.group.muted =
                                    !this.widget.group.muted;
                              });
                            },
                          ),
                          Visibility(
                            visible:
                                (this.widget.group.eventsUnseen.length > 0),
                            child: GestureDetector(
                              onTap: () {
                                setState(() {
                                  // TODO mute the group
                                  this.widget.group.muted =
                                      !this.widget.group.muted;
                                });
                              },
                              child: Row(
                                children: <Widget>[
                                  Padding(
                                      padding: EdgeInsets.all(
                                          MediaQuery.of(context).size.height *
                                              .02)),
                                  Text((this.widget.group.eventsUnseen.length <
                                          100)
                                      ? this
                                          .widget
                                          .group
                                          .eventsUnseen
                                          .length
                                          .toString()
                                      : "99+")
                                ],
                              ),
                            ),
                          )
                        ]),
                  ),
                ),
              ),
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
