import 'dart:io';

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
import 'package:image_picker/image_picker.dart';

import 'groups_home.dart';

class CreateGroup extends StatefulWidget {
  @override
  _CreateGroupState createState() => _CreateGroupState();
}

class _CreateGroupState extends State<CreateGroup> {
  bool autoValidate = false;
  String groupName;
  int votingDuration;
  File icon;
  int rsvpDuration;

  List<Member> displayedMembers = new List<Member>();
  Map<String, String> selectedCategories =
      new Map<String, String>(); // map of categoryIds -> categoryName
  Map<String, String> originalCategories =
      new Map<String, String>(); // map of categoryIds -> categoryName

  final GlobalKey<FormState> formKey = new GlobalKey<FormState>();
  final TextEditingController groupNameController = new TextEditingController();
  final TextEditingController votingDurationController =
      new TextEditingController();
  final TextEditingController rsvpDurationController =
      new TextEditingController();

  @override
  void dispose() {
    groupNameController.dispose();
    votingDurationController.dispose();
    rsvpDurationController.dispose();
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
                    controller: rsvpDurationController,
                    keyboardType: TextInputType.number,
                    validator: validRsvpDuration,
                    onSaved: (String arg) {
                      rsvpDuration = int.parse(arg.trim());
                    },
                    decoration: InputDecoration(
                      labelText: "Enter a default RSVP duration (in minutes)",
                    ),
                  ),
                  TextFormField(
                    controller: votingDurationController,
                    keyboardType: TextInputType.number,
                    validator: validVotingDuration,
                    onSaved: (String arg) {
                      votingDuration = int.parse(arg.trim());
                    },
                    decoration: InputDecoration(
                      labelText: "Enter a default voting duration (in minutes)",
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

  void showMembersPopup() {
    showDialog(
            context: context,
            child: MembersPopup(displayedMembers, displayedMembers, true,
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
          categories: selectedCategories,
          members: membersMap,
          defaultVotingDuration: votingDuration,
          defaultRsvpDuration: rsvpDuration);

      showLoadingDialog(
          context, "Creating group...", true); // show loading dialog
      bool success = await GroupsManager.createNewGroup(group, icon, context);

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
