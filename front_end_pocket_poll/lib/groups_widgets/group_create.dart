import 'dart:io';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/models/event.dart';
import 'package:front_end_pocket_poll/models/group.dart';
import 'package:front_end_pocket_poll/models/group_category.dart';
import 'package:front_end_pocket_poll/models/member.dart';
import 'package:front_end_pocket_poll/models/user_group.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'package:front_end_pocket_poll/utilities/validator.dart';

import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'group_create_pick_categories.dart';
import 'package:front_end_pocket_poll/widgets/members_page.dart';
import 'package:image_cropper/image_cropper.dart';
import 'package:image_picker/image_picker.dart';

class GroupCreate extends StatefulWidget {
  @override
  _GroupCreateState createState() => _GroupCreateState();
}

class _GroupCreateState extends State<GroupCreate> {
  final GlobalKey<FormState> formKey = new GlobalKey<FormState>();
  final TextEditingController groupNameController = new TextEditingController();

  bool autoValidate;
  bool isOpen;
  String groupName;
  File groupIcon;
  List<Member> groupMembers;
  FocusNode considerFocus;
  FocusNode votingFocus;

  // map of categoryIds -> GroupCategory
  Map<String, GroupCategory> groupCategories = new Map<String, GroupCategory>();

  @override
  void initState() {
    this.autoValidate = false;
    this.isOpen = true;
    this.groupMembers = new List<Member>();
    this.considerFocus = new FocusNode();
    this.votingFocus = new FocusNode();
    super.initState();
  }

  @override
  void dispose() {
    groupNameController.dispose();
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
          title: Text("Create New Group"),
        ),
        key: Key("group_create:scaffold"),
        body: Form(
          key: this.formKey,
          autovalidate: this.autoValidate,
          child: ListView(
            shrinkWrap: true,
            padding: EdgeInsets.fromLTRB(20, 0, 20, 0),
            children: <Widget>[
              Column(
                children: [
                  TextFormField(
                    maxLength: Globals.maxGroupNameLength,
                    controller: this.groupNameController,
                    textCapitalization: TextCapitalization.sentences,
                    validator: validGroupName,
                    onSaved: (String arg) {
                      this.groupName = arg.trim();
                    },
                    textInputAction: TextInputAction.next,
                    onFieldSubmitted: (form) {
                      // when user hits the done button on keyboard, hide it.
                      FocusScope.of(context).requestFocus(this.considerFocus);
                    },
                    key: Key("group_create:group_name_input"),
                    decoration: InputDecoration(
                        labelText: "Enter group name", counterText: ""),
                  ),
                  Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .004),
                  ),
                  Container(
                    width: MediaQuery.of(context).size.width * .7,
                    height: MediaQuery.of(context).size.height * .4,
                    alignment: Alignment.topRight,
                    decoration: BoxDecoration(
                        image: DecorationImage(
                            fit: BoxFit.cover,
                            image: this.groupIcon == null
                                ? getGroupIconImage(null)
                                : FileImage(this.groupIcon))),
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
                            icon: Icon(Icons.add),
                            key: Key("group_create:add_categories_button"),
                            onPressed: () {
                              hideKeyboard(context);
                              Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                      builder: (context) =>
                                          GroupCreatePickCategories(
                                              this.groupCategories)));
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
                            key: Key("group_create:add_members_button"),
                            onPressed: () {
                              hideKeyboard(context);
                              Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                      builder: (context) => MembersPage(
                                          this.groupMembers,
                                          new List<String>(),
                                          true,
                                          true)));
                            },
                          )),
                    ],
                  ),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
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
                          width: MediaQuery.of(context).size.width * .20,
                          child: IconButton(
                            icon: (this.isOpen)
                                ? Icon(Icons.lock_open)
                                : Icon(Icons.lock),
                            onPressed: () {
                              setState(() {
                                this.isOpen = !this.isOpen;
                              });
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
                  key: Key("group_create:save_button"),
                  label: Text("Create Group"))
            ],
          ),
        ),
      ),
    );
  }

  // uses the OS of the device to pick an image, we compress it before sending it
  Future getImage() async {
    File newIcon = await ImagePicker.pickImage(
        source: ImageSource.gallery, imageQuality: 75);

    if (newIcon != null) {
      // user successfully picked an image, so now allow them to crop it
      File croppedImage = await ImageCropper.cropImage(
          sourcePath: newIcon.path,
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
        setState(() {
          this.groupIcon = croppedImage;
        });
      }
    }
  }

  /*
    Attempt to save the category if no errors exist with user input.

    If success then save the group locally and pop the page. Else highlight the input errors.
   */
  void validateInput() async {
    hideKeyboard(this.context);
    final form = this.formKey.currentState;
    if (form.validate()) {
      form.save();
      Map<String, Member> membersMap = new Map<String, Member>();
      for (Member member in this.groupMembers) {
        membersMap.putIfAbsent(member.username, () => member);
      }
      // creator is always in the group of course
      membersMap.putIfAbsent(
          Globals.user.username, () => new Member.fromUser(Globals.user));
      // it's okay to not have any inputted members, since creator is guaranteed to be there
      Group group = new Group(
          groupName: this.groupName,
          categories: this.groupCategories,
          members: membersMap,
          newEvents: new Map<String, Event>(),
          votingEvents: new Map<String, Event>(),
          considerEvents: new Map<String, Event>(),
          closedEvents: new Map<String, Event>(),
          occurringEvents: new Map<String, Event>(),
          isOpen: this.isOpen);

      showLoadingDialog(
          this.context, "Creating group...", true); // show loading dialog
      ResultStatus<Group> resultStatus =
          await GroupsManager.createNewGroup(group, groupIcon);
      Navigator.of(this.context, rootNavigator: true)
          .pop('dialog'); // dismiss the loading dialog

      if (resultStatus.success) {
        // update the local user object with this new group returned from the DB
        UserGroup newGroup = new UserGroup(
            groupId: resultStatus.data.groupId,
            groupName: resultStatus.data.groupName,
            icon: resultStatus.data.icon,
            lastActivity: resultStatus.data.lastActivity,
            muted: false,
            eventsUnseen: 0);
        Globals.user.groups
            .putIfAbsent(resultStatus.data.groupId, () => newGroup);
        Navigator.of(this.context).pop();
      } else {
        showErrorMessage("Error", resultStatus.errorMessage, this.context);
      }
    } else {
      setState(() => this.autoValidate = true);
    }
  }
}
