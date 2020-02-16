import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/models/member.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';

import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/widgets/category_popup.dart';
import 'package:frontEnd/widgets/members_popup.dart';

import 'groups_home.dart';

class CreateGroup extends StatefulWidget {
  @override
  _CreateGroupState createState() => _CreateGroupState();
}

class _CreateGroupState extends State<CreateGroup> {
  bool autoValidate = false;
  String groupName;
  String groupIcon;
  int pollPassPercent;
  int pollDuration;

  List<Member> displayedMembers = new List<Member>();
  Map<String, String> selectedCategories =
      new Map<String, String>(); // map of categoryIds -> categoryName
  Map<String, String> originalCategories =
      new Map<String, String>(); // map of categoryIds -> categoryName

  final GlobalKey<FormState> formKey = new GlobalKey<FormState>();
  final TextEditingController groupNameController = new TextEditingController();
  final TextEditingController groupIconController = new TextEditingController();
  final TextEditingController pollPassController = new TextEditingController();
  final TextEditingController pollDurationController =
      new TextEditingController();

  @override
  void dispose() {
    groupNameController.dispose();
    groupIconController.dispose();
    pollPassController.dispose();
    pollDurationController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      // allows for anywhere on the screen to be clicked to lose focus of a textfield
      onTap: () {
        hideKeyboard(context);
      },
      child: Scaffold(
        appBar: AppBar(
          title: Text("Add New Group"),
        ),
        body: Form(
          key: formKey,
          autovalidate: autoValidate,
          child: ListView(
            shrinkWrap: true,
            padding: EdgeInsets.all(20),
            children: <Widget>[
              Column(
                children: [
                  TextFormField(
                    controller: groupNameController,
                    validator: validGroupName,
                    onSaved: (String arg) {
                      groupName = arg.trim();
                    },
                    decoration: InputDecoration(
                      labelText: "Enter a group name",
                    ),
                  ),
                  TextFormField(
                    controller: groupIconController,
                    keyboardType: TextInputType.url,
                    validator: validGroupIcon,
                    onSaved: (String arg) {
                      groupIcon = arg.trim();
                    },
                    decoration: InputDecoration(
                      labelText: "Enter an icon link",
                    ),
                  ),
                  TextFormField(
                    controller: pollDurationController,
                    keyboardType: TextInputType.number,
                    validator: validPollDuration,
                    onSaved: (String arg) {
                      pollDuration = int.parse(arg.trim());
                    },
                    decoration: InputDecoration(
                      labelText: "Enter a default poll duration (in minutes)",
                    ),
                  ),
                  TextFormField(
                    controller: pollPassController,
                    keyboardType: TextInputType.number,
                    validator: validPassPercentage,
                    onSaved: (String arg) {
                      pollPassPercent = int.parse(arg.trim());
                    },
                    decoration: InputDecoration(
                      labelText: "Enter a default poll pass percentage (0-100)",
                    ),
                  ),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: <Widget>[
                      RaisedButton(
                        child: Text("Members"),
                        onPressed: () {
                          showMembersPopup();
                        },
                      ),
                      RaisedButton(
                        child: Text("Categories"),
                        onPressed: () {
                          showCategoriesPopup();
                        },
                      )
                    ],
                  ),
                  RaisedButton.icon(
                      onPressed: validateInput,
                      icon: Icon(Icons.add),
                      label: Text("Create Group"))
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  void showMembersPopup() {
    showDialog(
            context: context,
            child: MembersPopup(displayedMembers, displayedMembers,
                handlePopupClosed: () {}))
        .then((value) {
      hideKeyboard(context);
    });
  }

  void showCategoriesPopup() {
    showDialog(
            context: context,
            child: CategoryPopup(selectedCategories, handlePopupClosed: () {}))
        .then((value) {
      hideKeyboard(context);
    });
  }

  void validateInput() async {
    final form = formKey.currentState;
    if (form.validate()) {
      form.save();
      Map<String, Map<String, String>> membersMap =
          new Map<String, Map<String, String>>();
      for (Member member in displayedMembers) {
        Map<String, String> memberInfo = new Map<String, String>();
        memberInfo.putIfAbsent(
            UsersManager.DISPLAY_NAME, () => member.displayName);
        memberInfo.putIfAbsent(UsersManager.ICON, () => member.icon);
        membersMap.putIfAbsent(member.username, () => memberInfo);
      }
      // creator is always in the group of course
      Map<String, String> memberInfo = new Map<String, String>();
      memberInfo.putIfAbsent(
          UsersManager.DISPLAY_NAME, () => Globals.user.displayName);
      memberInfo.putIfAbsent(UsersManager.ICON, () => Globals.user.icon);
      membersMap.putIfAbsent(Globals.username, () => memberInfo);
      // it's okay to not have any inputted members, since creator is guaranteed to be there
      Group group = new Group(
          groupName: groupName,
          groupCreator: Globals.username,
          icon: groupIcon,
          categories: selectedCategories,
          members: membersMap,
          defaultPollDuration: pollDuration,
          defaultPollPassPercent: pollPassPercent);

      showLoadingDialog(context, "Creating group..."); // show loading dialog
      bool success = await GroupsManager.createNewGroup(group, context);

      Navigator.of(context, rootNavigator: true)
          .pop('dialog'); // dismiss the loading dialog

      if (success) {
        Navigator.pushAndRemoveUntil(
            context,
            new MaterialPageRoute(
                builder: (BuildContext context) => GroupsHome()),
            (Route<dynamic> route) => false);
      }
    } else {
      setState(() => autoValidate = true);
    }
  }
}
