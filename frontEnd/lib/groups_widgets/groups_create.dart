import 'dart:io';

import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/models/member.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';

import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/widgets/category_pick.dart';
import 'package:frontEnd/widgets/members_page.dart';
import 'package:image_picker/image_picker.dart';

class CreateGroup extends StatefulWidget {
  @override
  _CreateGroupState createState() => _CreateGroupState();
}

class _CreateGroupState extends State<CreateGroup> {
  bool autoValidate = false;
  String groupName;
  int votingDuration;
  File icon;
  int considerDuration;

  List<Member> displayedMembers = new List<Member>();
  Map<String, String> selectedCategories =
      new Map<String, String>(); // map of categoryIds -> categoryName

  final GlobalKey<FormState> formKey = new GlobalKey<FormState>();
  final TextEditingController groupNameController = new TextEditingController();
  final TextEditingController votingDurationController =
      new TextEditingController();
  final TextEditingController considerDurationController =
      new TextEditingController();

  @override
  void dispose() {
    groupNameController.dispose();
    votingDurationController.dispose();
    considerDurationController.dispose();
    super.dispose();
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
                  Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .004),
                  ),
                  Container(
                    width: MediaQuery.of(context).size.width * .6,
                    height: MediaQuery.of(context).size.height * .3,
                    alignment: Alignment.topRight,
                    decoration: BoxDecoration(
                        image: DecorationImage(
                            fit: BoxFit.cover,
                            image: this.icon == null
                                ? getIconUrl(null)
                                : FileImage(this.icon))),
                    child: Container(
                      decoration: BoxDecoration(
                          color: Colors.grey.withOpacity(0.7),
                          shape: BoxShape.circle),
                      child: IconButton(
                        icon: Icon(Icons.edit),
                        color: Colors.blueAccent,
                        onPressed: () {
                          getImage();
                        },
                      ),
                    ),
                  ),
                  TextFormField(
                    controller: considerDurationController,
                    maxLength: 4,
                    keyboardType: TextInputType.number,
                    validator: (value) {
                      return validConsiderDuration(value, true);
                    },
                    onSaved: (String arg) {
                      considerDuration = int.parse(arg.trim());
                    },
                    decoration: InputDecoration(
                      labelText:
                          "Enter a default consider duration (in minutes)",
                      counterText: ""
                    ),
                  ),
                  TextFormField(
                    controller: votingDurationController,
                    maxLength: 4,
                    keyboardType: TextInputType.number,
                    validator: (value) {
                      return validVotingDuration(value, true);
                    },
                    onSaved: (String arg) {
                      votingDuration = int.parse(arg.trim());
                    },
                    decoration: InputDecoration(
                      labelText: "Enter a default voting duration (in minutes)",
                      counterText:  ""
                    ),
                  ),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    children: <Widget>[
                      Expanded(
                        child: Text(
                          "Select categories for group",
                          style: TextStyle(
                              fontSize:
                                  DefaultTextStyle.of(context).style.fontSize *
                                      0.4),
                        ),
                      ),
                      Container(
                          width: MediaQuery.of(context).size.width * .20,
                          child: IconButton(
                            icon: Icon(Icons.keyboard_arrow_right),
                            onPressed: () {
                              Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                      builder: (context) =>
                                          CategoryPick(selectedCategories)));
                            },
                          )),
                    ],
                  ),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    children: <Widget>[
                      Expanded(
                        child: Text(
                          "Add/Remove members",
                          style: TextStyle(
                              fontSize:
                                  DefaultTextStyle.of(context).style.fontSize *
                                      0.4),
                        ),
                      ),
                      Container(
                          width: MediaQuery.of(context).size.width * .20,
                          child: IconButton(
                            icon: Icon(Icons.add),
                            onPressed: () {
                              Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                      builder: (context) => MembersPage(
                                            displayedMembers,
                                            displayedMembers,
                                            true,
                                          )));
                            },
                          )),
                    ],
                  ),
                ],
              ),
            ],
          ),
        ),
        bottomNavigationBar: BottomAppBar(
          color: Theme.of(context).scaffoldBackgroundColor,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              RaisedButton.icon(
                  onPressed: validateInput,
                  icon: Icon(Icons.add),
                  label: Text("Create Group"))
            ],
          ),
        ),
      ),
    );
  }

  Future getImage() async {
    File newIcon = await ImagePicker.pickImage(
        source: ImageSource.gallery,
        imageQuality: 75,
        maxWidth: 600,
        maxHeight: 600);

    setState(() {
      this.icon = newIcon;
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
          categories: selectedCategories,
          members: membersMap,
          defaultVotingDuration: votingDuration,
          defaultConsiderDuration: considerDuration);

      showLoadingDialog(
          context, "Creating group...", true); // show loading dialog
      ResultStatus resultStatus =
          await GroupsManager.createNewGroup(group, icon);
      Navigator.of(context, rootNavigator: true)
          .pop('dialog'); // dismiss the loading dialog

      if (resultStatus.success) {
        Navigator.of(context).pop();
      } else {
        showErrorMessage("Error", resultStatus.errorMessage, context);
      }
    } else {
      setState(() => autoValidate = true);
    }
  }
}
