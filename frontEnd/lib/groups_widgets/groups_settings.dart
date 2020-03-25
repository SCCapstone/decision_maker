import 'dart:io';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/groups_widgets/group_categories.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/models/group_left.dart';
import 'package:frontEnd/models/member.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/widgets/members_page.dart';
import 'package:image_picker/image_picker.dart';

import 'groups_home.dart';

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
  int votingDuration;
  int considerDuration;
  bool owner;
  List<Member> originalMembers = new List<Member>();
  List<Member> displayedMembers = new List<Member>();
  List<String> membersLeft = new List<String>();
  Map<String, String> selectedCategories =
      new Map<String, String>(); // map of categoryIds -> categoryName
  Map<String, String> originalCategories =
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
          displayName: Globals.currentGroup.members[username].displayName,
          icon: Globals.currentGroup.members[username].icon);
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
    for (String username in Globals.currentGroup.membersLeft.keys) {
      membersLeft.add(username);
    }
    groupName = Globals.currentGroup.groupName;
    votingDuration = Globals.currentGroup.defaultVotingDuration;
    considerDuration = Globals.currentGroup.defaultConsiderDuration;
    currentGroupIcon = Globals.currentGroup.icon;

    groupNameController.text = groupName;
    votingDurationController.text = votingDuration.toString();
    considerDurationController.text = considerDuration.toString();

    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
      onWillPop: handleBackPress,
      child: GestureDetector(
        onTap: () {
          // allows for anywhere on the screen to be clicked to lose focus of a textfield
          hideKeyboard(context);
        },
        child: Scaffold(
          resizeToAvoidBottomInset: true,
          appBar: AppBar(
            title: AutoSizeText(
              "Group Settings",
              minFontSize: 15,
              maxLines: 1,
              style: TextStyle(fontSize: 25),
            ),
            actions: <Widget>[
              Visibility(
                visible: editing,
                child: RaisedButton.icon(
                    // TODO don't show if private and user is not owner
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
                child: Scrollbar(
                  child: ListView(
                    shrinkWrap: true,
                    padding:
                        EdgeInsets.only(left: 12.0, right: 12.0, bottom: 5.0),
                    children: <Widget>[
                      Column(
                        children: [
                          TextFormField(
                            maxLength: Globals.maxGroupNameLength,
                            controller: groupNameController,
                            validator: validGroupName,
                            onChanged: (String arg) {
                              groupName = arg.trim();
                              enableAutoValidation();
                            },
                            onSaved: (String arg) {
                              groupName = arg.trim();
                            },
                            style: TextStyle(fontSize: 33),
                            decoration: InputDecoration(
                                labelText: "Group Name", counterText: ""),
                          ),
                          Padding(
                            padding: EdgeInsets.all(
                                MediaQuery.of(context).size.height * .004),
                          ),
                          GestureDetector(
                            onTap: () {
                              showGroupImage(
                                  this.icon == null
                                      ? getGroupIconUrlStr(currentGroupIcon)
                                      : FileImage(this.icon),
                                  context);
                            },
                            child: Container(
                              width: MediaQuery.of(context).size.width * .57,
                              height: MediaQuery.of(context).size.height * .27,
                              alignment: Alignment.topRight,
                              decoration: BoxDecoration(
                                  image: DecorationImage(
                                      fit: BoxFit.cover,
                                      image: this.icon == null
                                          ? getGroupIconUrlStr(currentGroupIcon)
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
                          ),
                          Padding(
                            padding: EdgeInsets.all(
                                MediaQuery.of(context).size.height * .004),
                          ),
                          Wrap(
                            runSpacing: 7,
                            children: <Widget>[
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceAround,
                                children: <Widget>[
                                  Expanded(
                                    child: AutoSizeText(
                                      "Default consider duration (mins)",
                                      minFontSize: 14,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: TextStyle(fontSize: 20),
                                    ),
                                  ),
                                  Container(
                                    width:
                                        MediaQuery.of(context).size.width * .20,
                                    child: TextFormField(
                                      maxLength: Globals.maxConsiderDigits,
                                      keyboardType: TextInputType.number,
                                      validator: (value) {
                                        return validConsiderDuration(
                                            value, false);
                                      },
                                      controller: considerDurationController,
                                      onChanged: (String arg) {
                                        try {
                                          considerDuration = int.parse(arg);
                                          enableAutoValidation();
                                        } catch (e) {
                                          autoValidate = true;
                                        }
                                      },
                                      onSaved: (String arg) {
                                        considerDuration = int.parse(arg);
                                      },
                                      decoration: InputDecoration(
                                          border: OutlineInputBorder(),
                                          counterText: ""),
                                    ),
                                  ),
                                ],
                              ),
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceAround,
                                children: <Widget>[
                                  Expanded(
                                    child: AutoSizeText(
                                      "Default voting duration (mins)",
                                      minFontSize: 14,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: TextStyle(fontSize: 20),
                                    ),
                                  ),
                                  Container(
                                    width:
                                        MediaQuery.of(context).size.width * .20,
                                    child: TextFormField(
                                      maxLength: Globals.maxVotingDigits,
                                      keyboardType: TextInputType.number,
                                      validator: (value) {
                                        return validVotingDuration(
                                            value, false);
                                      },
                                      controller: votingDurationController,
                                      onChanged: (String arg) {
                                        try {
                                          votingDuration = int.parse(arg);
                                          enableAutoValidation();
                                        } catch (e) {
                                          autoValidate = true;
                                        }
                                      },
                                      onSaved: (String arg) {
                                        votingDuration = int.parse(arg);
                                      },
                                      decoration: InputDecoration(
                                          border: OutlineInputBorder(),
                                          counterText: ""),
                                    ),
                                  ),
                                ],
                              ),
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceAround,
                                children: <Widget>[
                                  Expanded(
                                    child: AutoSizeText(
                                      "Select categories for group",
                                      minFontSize: 14,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: TextStyle(fontSize: 20),
                                    ),
                                  ),
                                  Container(
                                      width: MediaQuery.of(context).size.width *
                                          .20,
                                      child: IconButton(
                                        icon: Icon(Icons.keyboard_arrow_right),
                                        onPressed: () {
                                          Navigator.push(
                                              context,
                                              MaterialPageRoute(
                                                  builder: (context) =>
                                                      GroupCategories(
                                                        selectedCategories:
                                                            selectedCategories,
                                                      ))).then((_) {
                                            saveCategories();
                                          });
                                        },
                                      )),
                                ],
                              ),
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceAround,
                                children: <Widget>[
                                  Expanded(
                                    child: AutoSizeText(
                                      "Add/Remove members",
                                      minFontSize: 14,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: TextStyle(fontSize: 20),
                                    ),
                                  ),
                                  Container(
                                      width: MediaQuery.of(context).size.width *
                                          .20,
                                      child: IconButton(
                                        icon: Icon(Icons.add),
                                        onPressed: () {
                                          Navigator.push(
                                              context,
                                              MaterialPageRoute(
                                                  builder: (context) =>
                                                      MembersPage(
                                                        displayedMembers,
                                                        membersLeft,
                                                        false,
                                                      ))).then((_) {
                                            saveMembers();
                                          });
                                        },
                                      )),
                                ],
                              ),
                              Visibility(
                                visible: owner,
                                child: Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceAround,
                                  children: <Widget>[
                                    Expanded(
                                      child: AutoSizeText(
                                        "Make group open/private",
                                        minFontSize: 14,
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                        style: TextStyle(fontSize: 20),
                                      ),
                                    ),
                                    Container(
                                        width:
                                            MediaQuery.of(context).size.width *
                                                .20,
                                        child: IconButton(
                                          icon: Icon(Icons.lock),
                                          onPressed: () {
                                            // TODO lock group if owner
                                          },
                                        )),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ]),
          bottomNavigationBar: BottomAppBar(
            color: Theme.of(context).scaffoldBackgroundColor,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                RaisedButton(
                  child: Text((this.owner) ? "Delete Group" : "Leave Group"),
                  color: Colors.red,
                  onPressed: () {
                    if (this.owner) {
                      confirmDeleteGroup();
                    } else {
                      confirmLeaveGroup();
                    }
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Future<bool> handleBackPress() async {
    // if editing, ensure the user really wants to leave to lose their changes
    if (editing) {
      confirmLeavePage();
      return false;
    } else {
      return true;
    }
  }

  void saveMembers() async {
    Set oldMembers = originalMembers.toSet();
    Set newMembers = displayedMembers.toSet();
    bool changedMembers = !(oldMembers.containsAll(newMembers) &&
        oldMembers.length == newMembers.length);
    if (changedMembers) {
      Map<String, Member> membersMap = new Map<String, Member>();
      for (Member member in displayedMembers) {
        membersMap.putIfAbsent(member.username, () => member);
      }

      Group group = new Group(
          groupId: Globals.currentGroup.groupId,
          groupName: Globals.currentGroup.groupName,
          groupCreator: Globals.currentGroup.groupCreator,
          categories: Globals.currentGroup.categories,
          members: membersMap,
          events: Globals.currentGroup.events,
          defaultVotingDuration: Globals.currentGroup.defaultVotingDuration,
          defaultConsiderDuration:
              Globals.currentGroup.defaultConsiderDuration);

      ResultStatus<Group> resultStatus =
          await GroupsManager.editGroup(group, icon);

      if (resultStatus.success) {
        Globals.currentGroup = resultStatus.data;
        originalMembers.clear();
        originalMembers.addAll(displayedMembers);
      } else {
        displayedMembers.clear();
        displayedMembers.addAll(originalMembers);
        showErrorMessage("Error", "Error saving members", context);
      }
    }
  }

  void saveCategories() async {
    Set oldCategories = originalCategories.keys.toSet();
    Set newCategories = selectedCategories.keys.toSet();
    bool changedCategories = !(oldCategories.containsAll(newCategories) &&
        oldCategories.length == newCategories.length);
    if (changedCategories) {
      Group group = new Group(
          groupId: Globals.currentGroup.groupId,
          groupName: Globals.currentGroup.groupName,
          groupCreator: Globals.currentGroup.groupCreator,
          categories: selectedCategories,
          members: Globals.currentGroup.members,
          events: Globals.currentGroup.events,
          defaultVotingDuration: Globals.currentGroup.defaultVotingDuration,
          defaultConsiderDuration:
              Globals.currentGroup.defaultConsiderDuration);

      ResultStatus<Group> resultStatus =
          await GroupsManager.editGroup(group, icon);

      if (resultStatus.success) {
        Globals.currentGroup = resultStatus.data;
        originalCategories.clear();
        originalCategories.addAll(selectedCategories);
      } else {
        selectedCategories.clear();
        selectedCategories.addAll(originalCategories);
        showErrorMessage("Error", "Error saving categories", context);
      }
    }
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

  void confirmLeavePage() {
    hideKeyboard(context);
    showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: Text("Unsaved changes"),
            actions: <Widget>[
              FlatButton(
                child: Text("Yes"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                  Navigator.of(context).pop();
                },
              ),
              FlatButton(
                child: Text("No"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                },
              )
            ],
            content: Text(
                "You have unsaved changes to this group. To save them click the \"Save\" button in "
                "the upper right hand corner.\n\nAre you sure you wish to leave this page and lose your changes?"),
          );
        });
  }

  void confirmLeaveGroup() {
    showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: Text("Leave group?"),
            actions: <Widget>[
              FlatButton(
                child: Text("Yes"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                  tryLeave();
                },
              ),
              FlatButton(
                child: Text("No"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                },
              )
            ],
            content:
                Text("Are you sure you wish to leave the group: $groupName?"),
          );
        });
  }

  void confirmDeleteGroup() {
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
                  tryDelete();
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

  void tryLeave() async {
    showLoadingDialog(context, "Leaving group...", true);
    ResultStatus resultStatus =
        await GroupsManager.leaveGroup(Globals.currentGroup.groupId);
    Navigator.of(context, rootNavigator: true).pop('dialog');

    if (resultStatus.success) {
      Globals.user.groups.remove(Globals.currentGroup.groupId);
      Globals.user.groupsLeft.putIfAbsent(
          Globals.currentGroup.groupId,
          () => new GroupLeft(
              groupId: Globals.currentGroup.groupId,
              groupName: Globals.currentGroup.groupName,
              icon: Globals.currentGroup.icon));
      Navigator.pushAndRemoveUntil(
          context,
          new MaterialPageRoute(
              builder: (BuildContext context) => GroupsHome()),
          (Route<dynamic> route) => false);
    } else {
      showErrorMessage("Error", resultStatus.errorMessage, context);
    }
  }

  void tryDelete() async {
    showLoadingDialog(context, "Deleting group...", true);
    ResultStatus resultStatus =
        await GroupsManager.deleteGroup(Globals.currentGroup.groupId);
    Navigator.of(context, rootNavigator: true).pop('dialog');

    if (resultStatus.success) {
      Globals.user.groups.remove(Globals.currentGroup.groupId);
      Navigator.pushAndRemoveUntil(
          context,
          new MaterialPageRoute(
              builder: (BuildContext context) => GroupsHome()),
          (Route<dynamic> route) => false);
    } else {
      showErrorMessage("Error", resultStatus.errorMessage, context);
    }
  }

  void enableAutoValidation() {
    // the moment the user makes changes to their previously saved settings, display the save button
    if (votingDuration != Globals.currentGroup.defaultVotingDuration ||
        considerDuration != Globals.currentGroup.defaultConsiderDuration ||
        groupName != Globals.currentGroup.groupName ||
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

  void validateInput() async {
    final form = formKey.currentState;
    if (form.validate() && validGroupIcon) {
      // b/c url is entered in a popup dialog, can't share the same form so must use another flag
      form.save();
      Map<String, Member> membersMap = new Map<String, Member>();
      for (Member member in displayedMembers) {
        membersMap.putIfAbsent(member.username, () => member);
      }

      Group group = new Group(
          groupId: Globals.currentGroup.groupId,
          groupName: groupName,
          groupCreator: Globals.currentGroup.groupCreator,
          categories: selectedCategories,
          members: membersMap,
          events: Globals.currentGroup.events,
          defaultVotingDuration: votingDuration,
          defaultConsiderDuration: considerDuration);

      showLoadingDialog(context, "Saving...", true);
      ResultStatus<Group> resultStatus =
          await GroupsManager.editGroup(group, icon);
      Navigator.of(context, rootNavigator: true).pop('dialog');

      if (resultStatus.success) {
        Globals.currentGroup = resultStatus.data;
        setState(() {
          // reset everything and reflect changes made
          originalMembers.clear();
          originalMembers.addAll(displayedMembers);
          originalCategories.clear();
          originalCategories.addAll(selectedCategories);
          groupNameController.text = groupName;
          votingDurationController.text = votingDuration.toString();
          considerDurationController.text = considerDuration.toString();
          editing = false;
          autoValidate = false;
          newIcon = false;
          hideKeyboard(context);
        });
      } else {
        showErrorMessage("Error", resultStatus.errorMessage, context);
      }
    } else {
      setState(() => autoValidate = true);
    }
  }
}
