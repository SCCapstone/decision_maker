import 'dart:io';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/groups_widgets/group_categories.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/group.dart';
import 'package:front_end_pocket_poll/models/group_category.dart';
import 'package:front_end_pocket_poll/models/group_left.dart';
import 'package:front_end_pocket_poll/models/member.dart';
import 'package:front_end_pocket_poll/utilities/validator.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'package:front_end_pocket_poll/widgets/members_page.dart';
import 'package:image_cropper/image_cropper.dart';
import 'package:image_picker/image_picker.dart';

class GroupSettings extends StatefulWidget {
  GroupSettings({Key key}) : super(key: key);

  @override
  _GroupSettingsState createState() => _GroupSettingsState();
}

class _GroupSettingsState extends State<GroupSettings> {
  bool autoValidate;
  bool validGroupIcon;
  bool editing;
  bool newIcon;
  bool isOpen;
  bool canEdit;
  File icon;
  String groupName;
  String currentGroupIcon;
  int votingDuration;
  int considerDuration;
  bool owner;
  List<Member> originalMembers;
  List<Member> displayedMembers;
  List<String> membersLeft; // list of usernames
  Map<String, GroupCategory>
      selectedCategories; // map of categoryIds -> GroupCategory
  Map<String, GroupCategory>
      originalCategories; // map of categoryIds -> GroupCategory

  final GlobalKey<FormState> formKey = new GlobalKey<FormState>();
  final TextEditingController groupNameController = new TextEditingController();
  final TextEditingController votingDurationController =
      new TextEditingController();
  final TextEditingController considerDurationController =
      new TextEditingController();
  final int muteAction = 0;
  final int leaveDeleteAction = 1;

  @override
  void dispose() {
    groupNameController.dispose();
    votingDurationController.dispose();
    considerDurationController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    this.autoValidate = false;
    this.validGroupIcon = true;
    this.editing = false;
    this.newIcon = false;
    this.originalMembers = new List<Member>();
    this.displayedMembers = new List<Member>();
    this.membersLeft = new List<String>();
    this.originalCategories = new Map<String, GroupCategory>();
    this.selectedCategories = new Map<String, GroupCategory>();

    if (Globals.user.username ==
        Globals.currentGroupResponse.group.groupCreator) {
      // to display the delete group button, check if user owns this group
      this.owner = true;
    } else {
      this.owner = false;
    }
    for (String username in Globals.currentGroupResponse.group.members.keys) {
      Member member = new Member(
          username: username,
          displayName:
              Globals.currentGroupResponse.group.members[username].displayName,
          icon: Globals.currentGroupResponse.group.members[username].icon);
      this.originalMembers.add(member); // preserve original members
      this.displayedMembers.add(member); // current selected members
    }
    for (String catId in Globals.currentGroupResponse.group.categories.keys) {
      // preserve original categories selected
      this.originalCategories.putIfAbsent(
          catId, () => Globals.currentGroupResponse.group.categories[catId]);
      this.selectedCategories.putIfAbsent(
          catId, () => Globals.currentGroupResponse.group.categories[catId]);
    }
    for (String username
        in Globals.currentGroupResponse.group.membersLeft.keys) {
      this.membersLeft.add(username);
    }
    this.groupName = Globals.currentGroupResponse.group.groupName;
    this.votingDuration =
        Globals.currentGroupResponse.group.defaultVotingDuration;
    this.considerDuration =
        Globals.currentGroupResponse.group.defaultConsiderDuration;
    this.currentGroupIcon = Globals.currentGroupResponse.group.icon;
    this.isOpen = Globals.currentGroupResponse.group.isOpen;
    this.canEdit = owner || (isOpen && !owner);

    this.groupNameController.text = groupName;
    this.votingDurationController.text = votingDuration.toString();
    this.considerDurationController.text = considerDuration.toString();

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
                visible: this.editing && this.canEdit,
                child: FlatButton(
                  child: Text(
                    "SAVE",
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                  key: Key("group_settings:save_button"),
                  textColor: Colors.white,
                  onPressed: () {
                    if (this.canEdit) {
                      attemptSave();
                    }
                  },
                ),
              ),
              Visibility(
                  visible: !this.canEdit,
                  child: IconButton(
                      disabledColor: Colors.white,
                      icon: Icon(Icons.lock),
                      tooltip: "Group is locked")),
            ],
          ),
          key: Key("group_settings:scaffold"),
          body: Column(children: <Widget>[
            Form(
              key: this.formKey,
              autovalidate: this.autoValidate,
              child: Expanded(
                child: Scrollbar(
                  child: ListView(
                    shrinkWrap: true,
                    padding:
                        EdgeInsets.only(left: 12.0, right: 12.0, bottom: 5.0),
                    children: <Widget>[
                      Column(
                        children: [
                          Row(
                            children: <Widget>[
                              Expanded(
                                child: TextFormField(
                                  enabled: this.canEdit,
                                  maxLength: Globals.maxGroupNameLength,
                                  controller: this.groupNameController,
                                  validator: validGroupName,
                                  onChanged: (String arg) {
                                    this.groupName = arg.trim();
                                    showSaveButton();
                                  },
                                  onSaved: (String arg) {
                                    this.groupName = arg.trim();
                                  },
                                  key: Key("group_settings:group_name_input"),
                                  style: TextStyle(fontSize: 33),
                                  decoration: InputDecoration(
                                      labelText: "Group Name", counterText: ""),
                                ),
                              ),
                              PopupMenuButton<int>(
                                child: Icon(
                                  Icons.more_vert,
                                  size:
                                      MediaQuery.of(context).size.height * .04,
                                ),
                                key: Key("group_settings:more_icon_button"),
                                tooltip: "More Options",
                                onCanceled: () => hideKeyboard(context),
                                onSelected: (int result) {
                                  hideKeyboard(context);
                                  if (result == this.leaveDeleteAction) {
                                    if (this.owner) {
                                      confirmDeleteGroup();
                                    } else {
                                      confirmLeaveGroup();
                                    }
                                  } else if (result == this.muteAction) {
                                    if ((Globals
                                        .user
                                        .groups[Globals
                                            .currentGroupResponse.group.groupId]
                                        .muted)) {
                                      // we're unmuting the group
                                      UsersManager.setUserGroupMute(
                                          Globals.currentGroupResponse.group
                                              .groupId,
                                          false);
                                      Globals
                                          .user
                                          .groups[Globals.currentGroupResponse
                                              .group.groupId]
                                          .muted = false;
                                    } else {
                                      // we muting that fool
                                      UsersManager.setUserGroupMute(
                                          Globals.currentGroupResponse.group
                                              .groupId,
                                          true);
                                      Globals
                                          .user
                                          .groups[Globals.currentGroupResponse
                                              .group.groupId]
                                          .muted = true;
                                    }
                                  }
                                },
                                itemBuilder: (BuildContext context) =>
                                    <PopupMenuEntry<int>>[
                                  PopupMenuItem<int>(
                                    value: this.muteAction,
                                    child: Text(
                                      (Globals
                                              .user
                                              .groups[Globals
                                                  .currentGroupResponse
                                                  .group
                                                  .groupId]
                                              .muted)
                                          ? "Unmute"
                                          : "Mute",
                                    ),
                                  ),
                                  PopupMenuItem<int>(
                                    value: this.leaveDeleteAction,
                                    key: Key(
                                        "group_settings:delete_group_button"),
                                    child: Text(
                                      (this.owner)
                                          ? "Delete Group"
                                          : "Leave Group",
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                          Padding(
                            padding: EdgeInsets.all(
                                MediaQuery.of(context).size.height * .004),
                          ),
                          GestureDetector(
                            onTap: () {
                              showGroupImage(
                                  this.icon == null
                                      ? getGroupIconImage(this.currentGroupIcon)
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
                                          ? getGroupIconImage(
                                              this.currentGroupIcon)
                                          : FileImage(this.icon))),
                              child: Container(
                                  decoration: BoxDecoration(
                                      color: Colors.grey.withOpacity(0.7),
                                      shape: BoxShape.circle),
                                  child: Visibility(
                                    visible: this.canEdit,
                                    child: IconButton(
                                      icon: Icon(Icons.edit),
                                      color: Colors.blueAccent,
                                      onPressed: () {
                                        getImage();
                                      },
                                    ),
                                  )),
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
                                      enabled: this.canEdit,
                                      maxLength: Globals.maxConsiderDigits,
                                      keyboardType: TextInputType.number,
                                      validator: (value) {
                                        return validConsiderDuration(
                                            value, false);
                                      },
                                      key: Key("group_settings:conider_input"),
                                      controller:
                                          this.considerDurationController,
                                      onChanged: (String arg) {
                                        try {
                                          this.considerDuration =
                                              int.parse(arg);
                                          showSaveButton();
                                        } catch (e) {
                                          this.autoValidate = true;
                                        }
                                      },
                                      onSaved: (String arg) {
                                        this.considerDuration = int.parse(arg);
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
                                      enabled: this.canEdit,
                                      maxLength: Globals.maxVotingDigits,
                                      keyboardType: TextInputType.number,
                                      validator: (value) {
                                        return validVotingDuration(
                                            value, false);
                                      },
                                      key: Key("group_settings:vote_input"),
                                      controller: this.votingDurationController,
                                      onChanged: (String arg) {
                                        try {
                                          this.votingDuration = int.parse(arg);
                                          showSaveButton();
                                        } catch (e) {
                                          this.autoValidate = true;
                                        }
                                      },
                                      onSaved: (String arg) {
                                        this.votingDuration = int.parse(arg);
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
                                      (this.canEdit)
                                          ? "Select categories for group"
                                          : "View categories in group",
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
                                        icon: (this.canEdit)
                                            ? Icon(Icons.add)
                                            : Icon(Icons.keyboard_arrow_right),
                                        key: Key(
                                            "group_settings:add_categories_button"),
                                        onPressed: () {
                                          Navigator.push(
                                              context,
                                              MaterialPageRoute(
                                                  builder: (context) =>
                                                      GroupCategories(
                                                        selectedCategories: this
                                                            .selectedCategories,
                                                        canEdit: this.canEdit,
                                                      ))).then((_) {
                                            if (this.canEdit) {
                                              saveCategories();
                                            }
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
                                      (this.canEdit)
                                          ? "Add/Remove members"
                                          : "View members",
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
                                        icon: (this.canEdit)
                                            ? Icon(Icons.add)
                                            : Icon(Icons.keyboard_arrow_right),
                                        key: Key(
                                            "group_settings:add_members_button"),
                                        onPressed: () {
                                          Navigator.push(
                                              context,
                                              MaterialPageRoute(
                                                  builder: (context) =>
                                                      MembersPage(
                                                          this.displayedMembers,
                                                          this.membersLeft,
                                                          false,
                                                          this.canEdit))).then(
                                              (_) {
                                            if (this.canEdit) {
                                              saveMembers();
                                            }
                                          });
                                        },
                                      )),
                                ],
                              ),
                              Visibility(
                                visible: this.owner,
                                child: Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceAround,
                                  children: <Widget>[
                                    Expanded(
                                      child: AutoSizeText(
                                        (this.isOpen)
                                            ? "Make group private"
                                            : "Make group open",
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
                                          icon: (this.isOpen)
                                              ? Icon(Icons.lock_open)
                                              : Icon(Icons.lock),
                                          onPressed: () {
                                            setState(() {
                                              this.isOpen = !this.isOpen;
                                              showSaveButton();
                                            });
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
        ),
      ),
    );
  }

  // if editing, ensure the user really wants to leave and lose their changes
  Future<bool> handleBackPress() async {
    if (this.editing) {
      confirmLeavePage();
      return false;
    } else {
      return true;
    }
  }

  /*
    Save members if new ones were added/removed
    
    This is called automatically when the user closes the members page
   */
  void saveMembers() async {
    Set oldMembers = this.originalMembers.toSet();
    Set newMembers = this.displayedMembers.toSet();
    bool changedMembers = !(oldMembers.containsAll(newMembers) &&
        oldMembers.length == newMembers.length);
    if (changedMembers) {
      Map<String, Member> membersMap = new Map<String, Member>();
      for (Member member in this.displayedMembers) {
        membersMap.putIfAbsent(member.username, () => member);
      }

      Group group = new Group(
          groupId: Globals.currentGroupResponse.group.groupId,
          groupName: Globals.currentGroupResponse.group.groupName,
          groupCreator: Globals.currentGroupResponse.group.groupCreator,
          categories: Globals.currentGroupResponse.group.categories,
          members: membersMap,
          newEvents: Globals.currentGroupResponse.group.newEvents,
          votingEvents: Globals.currentGroupResponse.group.votingEvents,
          considerEvents: Globals.currentGroupResponse.group.considerEvents,
          closedEvents: Globals.currentGroupResponse.group.closedEvents,
          occurringEvents: Globals.currentGroupResponse.group.occurringEvents,
          defaultVotingDuration:
              Globals.currentGroupResponse.group.defaultVotingDuration,
          defaultConsiderDuration:
              Globals.currentGroupResponse.group.defaultConsiderDuration,
          isOpen: Globals.currentGroupResponse.group.isOpen);

      ResultStatus<Group> resultStatus =
          await GroupsManager.editGroup(group, this.icon);

      if (resultStatus.success) {
        Globals.currentGroupResponse.group = resultStatus.data;
        this.originalMembers.clear();
        this.originalMembers.addAll(displayedMembers);
      } else {
        this.displayedMembers.clear();
        this.displayedMembers.addAll(originalMembers);
        showErrorMessage("Error", "Error saving members", this.context);
      }
    }
  }

  /*
    Save categories if new ones were added/removed
    
    This is called automatically when the user closes the group categories page
   */
  void saveCategories() async {
    Set oldCategories = this.originalCategories.keys.toSet();
    Set newCategories = this.selectedCategories.keys.toSet();
    bool changedCategories = !(oldCategories.containsAll(newCategories) &&
        oldCategories.length == newCategories.length);
    if (changedCategories) {
      Group group = new Group(
          groupId: Globals.currentGroupResponse.group.groupId,
          groupName: Globals.currentGroupResponse.group.groupName,
          groupCreator: Globals.currentGroupResponse.group.groupCreator,
          categories: this.selectedCategories,
          members: Globals.currentGroupResponse.group.members,
          newEvents: Globals.currentGroupResponse.group.newEvents,
          votingEvents: Globals.currentGroupResponse.group.votingEvents,
          considerEvents: Globals.currentGroupResponse.group.considerEvents,
          closedEvents: Globals.currentGroupResponse.group.closedEvents,
          occurringEvents: Globals.currentGroupResponse.group.occurringEvents,
          defaultVotingDuration:
              Globals.currentGroupResponse.group.defaultVotingDuration,
          defaultConsiderDuration:
              Globals.currentGroupResponse.group.defaultConsiderDuration,
          isOpen: Globals.currentGroupResponse.group.isOpen);

      ResultStatus<Group> resultStatus =
          await GroupsManager.editGroup(group, this.icon);

      if (resultStatus.success) {
        Globals.currentGroupResponse.group = resultStatus.data;
        originalCategories.clear();
        originalCategories.addAll(this.selectedCategories);
      } else {
        selectedCategories.clear();
        selectedCategories.addAll(this.originalCategories);
        showErrorMessage("Error", "Error saving categories", this.context);
      }
    }
  }

  // uses the OS of the device to pick an image, we compress it before sending it
  Future getImage() async {
    File newIconFile = await ImagePicker.pickImage(
        source: ImageSource.gallery, imageQuality: 75);

    if (newIconFile != null) {
      // user successfully picked an image, so now allow them to crop it
      File croppedImage = await ImageCropper.cropImage(
          sourcePath: newIconFile.path,
          aspectRatio: CropAspectRatio(ratioX: 1, ratioY: 1),
          compressQuality: 100,
          maxHeight: 600,
          maxWidth: 600,
          compressFormat: ImageCompressFormat.jpg,
          androidUiSettings: AndroidUiSettings(
              toolbarColor: Globals.pocketPollPrimary,
              toolbarWidgetColor: Colors.white,
              toolbarTitle: "Crop Image"));
      if (croppedImage != null) {
        this.icon = croppedImage;
        this.newIcon = true;
        showSaveButton();
      }
    }
  }

  // show popup asking if user wants to leave page
  void confirmLeavePage() {
    hideKeyboard(this.context);
    showDialog(
        context: this.context,
        builder: (context) {
          return AlertDialog(
            title: Text("Unsaved changes"),
            actions: <Widget>[
              FlatButton(
                child: Text("YES"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                  Navigator.of(context).pop();
                },
              ),
              FlatButton(
                child: Text("NO"),
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

  // show popup asking if user really wants to leave group
  void confirmLeaveGroup() {
    showDialog(
        context: this.context,
        builder: (context) {
          return AlertDialog(
            title: Text("Leave group?"),
            actions: <Widget>[
              FlatButton(
                child: Text("YES"),
                key: Key("group_settings:leave_confirm"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                  tryLeave();
                },
              ),
              FlatButton(
                child: Text("NO"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                },
              )
            ],
            content: Text(
                "Are you sure you wish to leave the group \"$groupName\"?"),
          );
        });
  }

  // show popup asking if user really wants to delete group
  void confirmDeleteGroup() {
    showDialog(
        context: this.context,
        builder: (context) {
          return AlertDialog(
            title: Text("Delete"),
            actions: <Widget>[
              FlatButton(
                child: Text("YES"),
                key: Key("group_settings:delete_confirm"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                  tryDelete();
                },
              ),
              FlatButton(
                child: Text("NO"),
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
    showLoadingDialog(this.context, "Leaving group...", true);
    ResultStatus resultStatus = await GroupsManager.leaveGroup(
        Globals.currentGroupResponse.group.groupId);
    Navigator.of(this.context, rootNavigator: true).pop('dialog');

    if (resultStatus.success) {
      Globals.user.groups.remove(Globals.currentGroupResponse.group.groupId);
      Globals.user.groupsLeft.putIfAbsent(
          Globals.currentGroupResponse.group.groupId,
          () => new GroupLeft(
              groupId: Globals.currentGroupResponse.group.groupId,
              groupName: Globals.currentGroupResponse.group.groupName,
              icon: Globals.currentGroupResponse.group.icon));
      Navigator.of(this.context).popUntil((route) => route.isFirst);
    } else {
      showErrorMessage("Error", resultStatus.errorMessage, this.context);
    }
  }

  void tryDelete() async {
    showLoadingDialog(this.context, "Deleting group...", true);
    ResultStatus resultStatus = await GroupsManager.deleteGroup(
        Globals.currentGroupResponse.group.groupId);
    Navigator.of(this.context, rootNavigator: true).pop('dialog');

    if (resultStatus.success) {
      Globals.user.groups.remove(Globals.currentGroupResponse.group.groupId);
      Navigator.of(this.context).popUntil((route) => route.isFirst);
    } else {
      showErrorMessage("Error", resultStatus.errorMessage, this.context);
    }
  }

  // the moment the user makes changes to their previously saved settings, display the save button
  void showSaveButton() {
    if (this.votingDuration !=
            Globals.currentGroupResponse.group.defaultVotingDuration ||
        this.considerDuration !=
            Globals.currentGroupResponse.group.defaultConsiderDuration ||
        this.groupName != Globals.currentGroupResponse.group.groupName ||
        this.isOpen != Globals.currentGroupResponse.group.isOpen ||
        this.newIcon) {
      setState(() {
        this.editing = true;
      });
    } else {
      setState(() {
        this.editing = false;
      });
    }
  }

  /*
    Attempts to save the new group if all input is valid.
    
    If success then the local current group is updated with the value returned by the DB.
   */
  void attemptSave() async {
    hideKeyboard(this.context);
    final form = this.formKey.currentState;
    if (form.validate() && this.validGroupIcon) {
      // b/c url is entered in a popup dialog, can't share the same form so must use another flag
      form.save();
      Map<String, Member> membersMap = new Map<String, Member>();
      for (Member member in this.displayedMembers) {
        membersMap.putIfAbsent(member.username, () => member);
      }

      Group group = new Group(
          groupId: Globals.currentGroupResponse.group.groupId,
          groupName: this.groupName,
          groupCreator: Globals.currentGroupResponse.group.groupCreator,
          categories: this.selectedCategories,
          members: membersMap,
          newEvents: Globals.currentGroupResponse.group.newEvents,
          votingEvents: Globals.currentGroupResponse.group.votingEvents,
          considerEvents: Globals.currentGroupResponse.group.considerEvents,
          closedEvents: Globals.currentGroupResponse.group.closedEvents,
          occurringEvents: Globals.currentGroupResponse.group.occurringEvents,
          defaultVotingDuration: this.votingDuration,
          defaultConsiderDuration: this.considerDuration,
          isOpen: this.isOpen);

      int batchNum = Globals.currentGroupResponse.group.currentBatchNum;

      showLoadingDialog(this.context, "Saving...", true);
      ResultStatus<Group> resultStatus =
          await GroupsManager.editGroup(group, icon, batchNumber: batchNum);
      Navigator.of(this.context, rootNavigator: true).pop('dialog');

      if (resultStatus.success) {
        Globals.currentGroupResponse.group = resultStatus.data;
        Globals.currentGroupResponse.group.currentBatchNum = batchNum;
        setState(() {
          // reset everything and reflect changes made
          this.originalMembers.clear();
          this.originalMembers.addAll(displayedMembers);
          this.originalCategories.clear();
          this.originalCategories.addAll(selectedCategories);
          this.groupNameController.text = groupName;
          this.votingDurationController.text = votingDuration.toString();
          this.considerDurationController.text = considerDuration.toString();
          this.editing = false;
          this.autoValidate = false;
          this.newIcon = false;
        });
      } else {
        showErrorMessage("Error", resultStatus.errorMessage, this.context);
      }
    } else {
      setState(() => this.autoValidate = true);
    }
  }
}
