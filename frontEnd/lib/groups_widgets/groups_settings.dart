import 'dart:io';

import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/models/member.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/widgets/category_popup.dart';
import 'package:frontEnd/widgets/members_popup.dart';
import 'package:image_picker/image_picker.dart';

class GroupSettings extends StatefulWidget {
  GroupSettings({Key key}) : super(key: key);

  @override
  _GroupSettingsState createState() => _GroupSettingsState();
}

class _GroupSettingsState extends State<GroupSettings> {
  bool autoValidate = false;
  bool validGroupIcon = true;
  bool editing = false;
  bool newIcon = false;
  File icon;
  String groupName;
  String currentGroupIcon;
  int pollPassPercent;
  int pollDuration;
  bool owner;
  List<Member> originalMembers = new List<Member>();
  List<Member> displayedMembers = new List<Member>();
  Map<String, String> selectedCategories =
      new Map<String, String>(); // map of categoryIds -> categoryName
  Map<String, String> originalCategories =
      new Map<String, String>(); // map of categoryIds -> categoryName

  final GlobalKey<FormState> formKey = new GlobalKey<FormState>();
  final TextEditingController groupNameController = new TextEditingController();
  final TextEditingController pollPassController = new TextEditingController();
  final TextEditingController pollDurationController =
      new TextEditingController();

  @override
  void dispose() {
    groupNameController.dispose();
    pollPassController.dispose();
    pollDurationController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    if (Globals.username == Globals.currentGroup.groupCreator) {
      // to display the delete button, check if user owns this group
      owner = true;
    } else {
      owner = false;
    }
    for (String username in Globals.currentGroup.members.keys) {
      Member member = new Member(
          username: username,
          displayName: Globals.currentGroup.members[username]
              [UsersManager.DISPLAY_NAME],
          icon: Globals.currentGroup.members[username][UsersManager.ICON]);
      originalMembers.add(member); // preserve original members
      displayedMembers.add(member); // used to show group members in the popup
    }
    for (String catId in Globals.currentGroup.categories.keys) {
      // preserve original categories selected
      originalCategories.putIfAbsent(
          catId, () => Globals.currentGroup.categories[catId]);
      selectedCategories.putIfAbsent(
          catId, () => Globals.currentGroup.categories[catId]);
    }
    groupName = Globals.currentGroup.groupName;
    pollDuration = Globals.currentGroup.defaultPollDuration;
    pollPassPercent = Globals.currentGroup.defaultPollPassPercent;
    currentGroupIcon = Globals.currentGroup.icon;

    groupNameController.text = groupName;
    pollDurationController.text = pollDuration.toString();
    pollPassController.text = pollPassPercent.toString();

    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        // allows for anywhere on the screen to be clicked to lose focus of a textfield
        hideKeyboard(context);
      },
      child: Scaffold(
          resizeToAvoidBottomInset: true,
          appBar: AppBar(
            title: Text("${Globals.currentGroup.groupName} Settings"),
            actions: <Widget>[
              Visibility(
                visible: editing,
                child: RaisedButton.icon(
                    color: Colors.blue,
                    onPressed: validateInput,
                    icon: Icon(Icons.save),
                    label: Text("Save")),
              )
            ],
          ),
          body: Column(children: <Widget>[
            Form(
              key: formKey,
              autovalidate: autoValidate,
              child: Expanded(
                child: ListView(
                  shrinkWrap: true,
                  padding: EdgeInsets.all(10.0),
                  children: <Widget>[
                    Column(
                      children: [
                        TextFormField(
                          controller: groupNameController,
                          validator: validGroupName,
                          onChanged: (String arg) {
                            groupName = arg.trim();
                            enableAutoValidation();
                          },
                          onSaved: (String arg) {
                            groupName = arg.trim();
                          },
                          style: TextStyle(
                              fontSize:
                                  DefaultTextStyle.of(context).style.fontSize *
                                      0.8),
                          decoration: InputDecoration(labelText: "Group Name"),
                        ),
                        Padding(
                          padding: EdgeInsets.all(
                              MediaQuery.of(context).size.height * .01),
                        ),
                        Container(
                          width: MediaQuery.of(context).size.width * .6,
                          height: MediaQuery.of(context).size.height * .3,
                          alignment: Alignment.topRight,
                          decoration: BoxDecoration(
                              image: DecorationImage(
                                  fit: BoxFit.cover,
                                  image: this.icon == null
                                      ? getIconUrl(currentGroupIcon)
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
                        Padding(
                          padding: EdgeInsets.all(
                              MediaQuery.of(context).size.height * .004),
                        ),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceAround,
                          children: <Widget>[
                            Text(
                              "Default poll duration (mins)",
                              style: TextStyle(
                                  fontSize: DefaultTextStyle.of(context)
                                          .style
                                          .fontSize *
                                      0.4),
                            ),
                            Container(
                              width: MediaQuery.of(context).size.width * .25,
                              child: TextFormField(
                                maxLength: 6,
                                keyboardType: TextInputType.number,
                                validator: validPollDuration,
                                controller: pollDurationController,
                                onChanged: (String arg) {
                                  try {
                                    pollDuration = int.parse(arg);
                                    enableAutoValidation();
                                  } catch (e) {
                                    autoValidate = true;
                                  }
                                },
                                onSaved: (String arg) {
                                  pollDuration = int.parse(arg);
                                },
                                decoration: InputDecoration(
                                    border: OutlineInputBorder(),
                                    counterText: ""),
                              ),
                            ),
                          ],
                        ),
                        Padding(
                          padding: EdgeInsets.all(
                              MediaQuery.of(context).size.height * .004),
                        ),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceAround,
                          children: <Widget>[
                            Text(
                              "Default pass percentage     ",
                              style: TextStyle(
                                  fontSize: DefaultTextStyle.of(context)
                                          .style
                                          .fontSize *
                                      0.4),
                            ),
                            Container(
                              width: MediaQuery.of(context).size.width * .25,
                              child: TextFormField(
                                maxLength: 3,
                                controller: pollPassController,
                                keyboardType: TextInputType.number,
                                validator: validPassPercentage,
                                onChanged: (String arg) {
                                  try {
                                    pollPassPercent = int.parse(arg.trim());
                                    enableAutoValidation();
                                  } catch (e) {
                                    autoValidate = true;
                                  }
                                },
                                onSaved: (String arg) {
                                  pollPassPercent = int.parse(arg.trim());
                                },
                                decoration: InputDecoration(
                                    border: OutlineInputBorder(),
                                    counterText: ""),
                              ),
                            )
                          ],
                        ),
                        Padding(
                          padding: EdgeInsets.all(
                              MediaQuery.of(context).size.height * .004),
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
                        Padding(
                          padding: EdgeInsets.all(
                              MediaQuery.of(context).size.height * .004),
                        ),
                        Visibility(
                          visible: owner,
                          child: RaisedButton(
                            child: Text("Delete Group"),
                            color: Colors.red,
                            onPressed: () {
                              confirmDeleteGroup(context);
                            },
                          ),
                        ),
                        Visibility(
                          visible: !owner,
                          child: RaisedButton(
                            child: Text("Leave Group"),
                            color: Colors.red,
                            onPressed: () {
                              // TODO leave group
                            },
                          ),
                        )
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ])),
    );
  }

  Future getImage() async {
    File newIconFile = await ImagePicker.pickImage(
        source: ImageSource.gallery,
        imageQuality: 75,
        maxWidth: 600,
        maxHeight: 600);

    if (newIconFile != null) {
      this.newIcon = true;
      this.icon = newIconFile;
      enableAutoValidation();
    }
  }

  void showMembersPopup() {
    showDialog(
            context: context,
            child: MembersPopup(displayedMembers, originalMembers, false,
                handlePopupClosed: popupClosed))
        .then((val) {
      // this is called whenever the user clicks outside the alert dialog or hits the back button
      popupClosed();
    });
  }

  void showCategoriesPopup() {
    showDialog(
            context: context,
            child: CategoryPopup(selectedCategories,
                handlePopupClosed: popupClosed))
        .then((val) {
      // this is called whenever the user clicks outside the alert dialog or hits the back button
      popupClosed();
    });
  }

  void popupClosed() {
    enableAutoValidation();
    hideKeyboard(context);
  }

  void confirmDeleteGroup(BuildContext context) {
    showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: Text("Delete"),
            actions: <Widget>[
              FlatButton(
                child: Text("Yes"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                  tryDelete(context);
                },
              ),
              FlatButton(
                child: Text("No"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                },
              )
            ],
            content: Text("Are you sure you wish to delete this group?"),
          );
        });
  }

  void tryDelete(BuildContext context) async {
    // TODO delete entire group, then go back to home page (https://github.com/SCCapstone/decision_maker/issues/114)
    bool success =
        await GroupsManager.deleteGroup(Globals.currentGroup.groupId, context);
  }

  void enableAutoValidation() {
    // the moment the user makes changes to their previously saved settings, display the save button
    Set oldUsers = originalMembers.toSet();
    Set newUsers = displayedMembers.toSet();
    bool newUsersAdded =
        !(oldUsers.containsAll(newUsers) && oldUsers.length == newUsers.length);

    Set oldCategories = originalCategories.keys.toSet();
    Set newCategories = selectedCategories.keys.toSet();
    bool newCategoriesAdded = !(oldCategories.containsAll(newCategories) &&
        oldCategories.length == newCategories.length);

    if (pollPassPercent != Globals.currentGroup.defaultPollPassPercent ||
        pollDuration != Globals.currentGroup.defaultPollDuration ||
        groupName != Globals.currentGroup.groupName ||
        newUsersAdded ||
        newCategoriesAdded ||
        newIcon) {
      setState(() {
        editing = true;
      });
    } else {
      setState(() {
        editing = false;
      });
    }
  }

  void validateInput() {
    final form = formKey.currentState;
    if (form.validate() && validGroupIcon) {
      // b/c url is entered in a popup dialog, can't share the same form so must use another flag
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

      Group group = new Group(
          groupId: Globals.currentGroup.groupId,
          groupName: groupName,
          groupCreator: Globals.currentGroup.groupCreator,
          categories: selectedCategories,
          members: membersMap,
          events: Globals.currentGroup.events,
          defaultPollDuration: pollDuration,
          defaultPollPassPercent: pollPassPercent,
          nextEventId: Globals.currentGroup.nextEventId);

      Globals.currentGroup = group;
      GroupsManager.editGroup(group, icon, context);

      setState(() {
        // reset everything and reflect changes made
        originalMembers.clear();
        originalMembers.addAll(displayedMembers);
        originalCategories.clear();
        originalCategories.addAll(selectedCategories);
        groupNameController.text = groupName;
        pollDurationController.text = pollDuration.toString();
        pollPassController.text = pollPassPercent.toString();
        editing = false;
        autoValidate = false;
        newIcon = false;
        hideKeyboard(context);
      });
    } else {
      setState(() => autoValidate = true);
    }
  }
}
