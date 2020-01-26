import 'package:flutter/material.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/widgets/category_dropdown.dart';
import 'package:frontEnd/widgets/users_dropdown.dart';

import 'package:frontEnd/models/category.dart';

class GroupSettings extends StatefulWidget {
  GroupSettings({Key key}) : super(key: key);

  @override
  _GroupSettingsState createState() => _GroupSettingsState();
}

class _GroupSettingsState extends State<GroupSettings> {
  bool autoValidate = false;
  bool validGroupIcon = true;
  bool editing = false;
  String groupName;
  String groupIcon;
  int pollPassPercent;
  int pollDuration;
  Map<String, dynamic> users;
  Future<List<Category>> categoriesTotalFuture;
  bool owner;

  final List<Category> categoriesSelected = new List<Category>();
  final formKey = GlobalKey<FormState>();
  final groupNameController = TextEditingController();
  final groupIconController = TextEditingController();
  final pollPassController = TextEditingController();
  final pollDurationController = TextEditingController();

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
    if (Globals.username == Globals.currentGroup.groupCreator) {
      // to display the delete button, check if user owns this group
      owner = true;
    } else {
      owner = false;
    }

    users = Globals.currentGroup.members;
    groupName = Globals.currentGroup.groupName;
    groupIcon = Globals.currentGroup.icon; // icon only changes via popup
    groupNameController.text = Globals.currentGroup.groupName;
    pollPassController.text =
        Globals.currentGroup.defaultPollPassPercent.toString();
    pollDurationController.text =
        Globals.currentGroup.defaultPollDuration.toString();
    categoriesTotalFuture = CategoriesManager.getAllCategoriesList();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        resizeToAvoidBottomInset: true,
        appBar: AppBar(
          title: Text("$groupName Settings"),
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
                          // the moment the user starts making changes, display the save button
                          enableAutoValidation(
                              !(arg == Globals.currentGroup.groupName));
                        },
                        onSaved: (String arg) {
                          groupName = arg;
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
                        width: MediaQuery.of(context).size.width * .5,
                        height: MediaQuery.of(context).size.height * .3,
                        alignment: Alignment.topRight,
                        decoration: BoxDecoration(
                            image: DecorationImage(
                                fit: BoxFit.fitHeight,
                                image: NetworkImage(groupIcon))),
                        child: Container(
                          decoration: BoxDecoration(
                              color: Colors.grey.withOpacity(0.7),
                              shape: BoxShape.circle),
                          child: IconButton(
                            icon: Icon(Icons.edit),
                            color: Colors.blueAccent,
                            onPressed: () {
                              groupIconPopup(context, autoValidate,
                                  groupIconController, updateIcon);
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
                                  int num = int.parse(arg);
                                  // the moment the user starts making changes, display the save button
                                  enableAutoValidation(num !=
                                      Globals.currentGroup.defaultPollDuration);
                                } catch (e) {
                                  enableAutoValidation(true);
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
                                  int num = int.parse(arg);
                                  enableAutoValidation(num !=
                                      Globals
                                          .currentGroup.defaultPollPassPercent);
                                } catch (e) {
                                  enableAutoValidation(true);
                                }
                                // the moment the user starts making changes, display the save button
                              },
                              onSaved: (String arg) {
                                pollPassPercent = int.parse(arg);
                              },
                              decoration: InputDecoration(
                                  border: OutlineInputBorder(),
                                  counterText: ""),
                            ),
                          )
                        ],
                      ),
                      FutureBuilder(
                          future: categoriesTotalFuture,
                          builder:
                              (BuildContext context, AsyncSnapshot snapshot) {
                            if (snapshot.hasData) {
                              List<Category> categories = snapshot.data;
                              categories = snapshot.data;
                              if (!editing) {
                                for (Category category in categories) {
                                  if (Globals.currentGroup.categories.keys
                                      .contains(category.categoryId)) {
                                    categoriesSelected.add(category);
                                  }
                                }
                              }
                              return CategoryDropdown("Add categories",
                                  categories, categoriesSelected,
                                  callback: (category) =>
                                      selectCategory(category));
                            } else if (snapshot.hasError) {
                              return Text("Error: ${snapshot.error}");
                            } else {
                              return Center(child: CircularProgressIndicator());
                            }
                          }),
                      UsersDropdown("Users", users,
                          Globals.currentGroup.groupCreator == Globals.username,
                          deleteCallback: (user) => removeUser(user),
                          addCallback: (user) => addUser(user)),
                      Visibility(
                        visible: owner,
                        child: RaisedButton(
                          child: Text("Delete Group"),
                          color: Colors.red,
                          onPressed: () {
                            tryDelete(context);
                          },
                        ),
                      )
                    ],
                  ),
                ],
              ),
            ),
          ),
        ]));
  }

  void tryDelete(BuildContext context) async {
    // TODO delete entire group, then go back to home page (https://github.com/SCCapstone/decision_maker/issues/114)
    bool success =
        await GroupsManager.deleteGroup(Globals.currentGroup.groupId, context);
  }

  void enableAutoValidation(bool val) {
    setState(() {
      editing = val;
    });
  }

  void addUser(String username) {
    setState(() {
      editing = true;
      users.putIfAbsent(username, () => username);
    });
  }

  void removeUser(String user) {
    setState(() {
      editing = true;
      users.remove(user);
    });
  }

  void selectCategory(Category category) {
    setState(() {
      editing = true;
      if (categoriesSelected.contains(category)) {
        categoriesSelected.remove(category);
      } else {
        categoriesSelected.add(category);
      }
    });
  }

  void updateIcon(String iconUrl) {
    setState(() {
      groupIcon = iconUrl;
      groupIconController.clear();
      editing = true;
      validGroupIcon = true;
      autoValidate = true;
      Navigator.of(context).pop();
    });
  }

  void validateInput() {
    final form = formKey.currentState;
    if (form.validate() && validGroupIcon) {
      // b/c url is entered in a popup dialog, can't share the same form so must use another flag
      form.save();
      Map<String, String> categoriesMap = new Map<String, String>();
      for (int i = 0; i < categoriesSelected.length; i++) {
        categoriesMap.putIfAbsent(categoriesSelected[i].categoryId,
            () => categoriesSelected[i].categoryName);
      }

      Group group = new Group(
          groupId: Globals.currentGroup.groupId,
          groupName: groupName,
          groupCreator: Globals.currentGroup.groupCreator,
          icon: groupIcon,
          categories: categoriesMap,
          members: users,
          events: Globals.currentGroup.events,
          defaultPollDuration: pollDuration,
          defaultPollPassPercent: pollPassPercent,
          nextEventId: Globals.currentGroup.nextEventId);

      Globals.currentGroup = group;
      GroupsManager.editGroup(group, context);

      setState(() {
        // reset everything and reflect changes made
        groupNameController.text = groupName;
        groupIconController.clear();
        pollDurationController.text = pollDuration.toString();
        pollPassController.text = pollPassPercent.toString();
        editing = false;
        autoValidate = false;
      });
    } else {
      setState(() => autoValidate = true);
    }
  }
}
