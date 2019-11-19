import 'package:flutter/material.dart';
import 'package:frontEnd/models/group.dart';

import 'imports/dev_testing_manager.dart';

class GroupSettings extends StatefulWidget {
  final Group group;

  GroupSettings({Key key, this.group}) : super(key: key);

  @override
  _GroupSettingsState createState() => _GroupSettingsState();
}

class _GroupSettingsState extends State<GroupSettings> {
  final groupNameController = TextEditingController();
  final imageLinkController = TextEditingController();
  final pollPassController = TextEditingController();
  final pollDurationController = TextEditingController();

  @override
  void dispose() {
    groupNameController.dispose();
    imageLinkController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Group Settings"),
      ),
      body: Center(
        child: Padding(
          padding: EdgeInsets.all(20.0),
          child: Column(
            children: [
              TextFormField(
                controller: groupNameController,
                decoration: InputDecoration(
                  labelText: "Enter a group name",
                ),
              ),
              TextFormField(
                controller: imageLinkController,
                decoration: InputDecoration(
                  labelText: "Enter a icon link",
                ),
              ),
              TextFormField(
                controller: pollDurationController,
                keyboardType: TextInputType.number,
                decoration: InputDecoration(
                  labelText: "Enter a default poll duration (in minutes)",
                ),
              ),
              TextFormField(
                controller: pollPassController,
                keyboardType: TextInputType.number,
                decoration: InputDecoration(
                  labelText: "Enter a default poll pass percentage (0-100)",
                ),
              ),
              RaisedButton.icon(
                  onPressed: () {},
                  icon: Icon(Icons.add),
                  label: Text("Create Group"))
            ],
          ),
        ),
      ),
    );
  }
}
