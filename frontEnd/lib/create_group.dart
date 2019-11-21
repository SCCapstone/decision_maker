import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/widgets/category_dropdown.dart';
import 'package:frontEnd/widgets/users_dropdown.dart';

class CreateGroup extends StatefulWidget {
  @override
  _CreateGroupState createState() => _CreateGroupState();
}

class _CreateGroupState extends State<CreateGroup> {
  bool _autoValidate = false;
  String _groupName;
  String _groupIcon;
  int _pollPassPercent;
  int _pollDuration;
  List<Category> categoriesToAdd = new List<Category>();
  List<Category> categoriesTotal = new List<Category>();
  List<String> users;

  final _formKey = GlobalKey<FormState>();
  final _groupNameController = TextEditingController();
  final _groupIconController = TextEditingController();
  final _pollPassController = TextEditingController();
  final _pollDurationController = TextEditingController();

  @override
  void dispose() {
    _groupNameController.dispose();
    _groupIconController.dispose();
    _pollPassController.dispose();
    _pollDurationController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    categoriesToAdd = new List<Category>();
    // TODO actually get the categories from DB
    for (int i = 0; i < 5; i++) {
      Category category = new Category.debug("123", "$i",
          new Map<String, dynamic>(), new Map<String, dynamic>(), 1, "testing");
      categoriesTotal.add(category);
    }
    users = new List<String>();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Add New Group"),
      ),
      body: Form(
        key: _formKey,
        autovalidate: _autoValidate,
        child: ListView(
          shrinkWrap: true,
          padding: EdgeInsets.all(20),
          children: <Widget>[
            Column(
              children: [
                TextFormField(
                  controller: _groupNameController,
                  validator: validGroupName,
                  onSaved: (String arg) {
                    _groupName = arg;
                  },
                  decoration: InputDecoration(
                    labelText: "Enter a group name",
                  ),
                ),
                TextFormField(
                  controller: _groupIconController,
                  keyboardType: TextInputType.url,
                  validator: validGroupIcon,
                  onSaved: (String arg) {
                    _groupIcon = arg;
                  },
                  decoration: InputDecoration(
                    labelText: "Enter a icon link",
                  ),
                ),
                TextFormField(
                  controller: _pollDurationController,
                  keyboardType: TextInputType.number,
                  validator: validPollDuration,
                  onSaved: (String arg) {
                    _pollDuration = int.parse(arg);
                  },
                  decoration: InputDecoration(
                    labelText: "Enter a default poll duration (in minutes)",
                  ),
                ),
                TextFormField(
                  controller: _pollPassController,
                  keyboardType: TextInputType.number,
                  validator: validPassPercentage,
                  onSaved: (String arg) {
                    _pollPassPercent = int.parse(arg);
                  },
                  decoration: InputDecoration(
                    labelText: "Enter a default poll pass percentage (0-100)",
                  ),
                ),
                CategoryDropdown(
                    "Add categories", categoriesTotal, categoriesToAdd,
                    callback: (category) => selectCategory(category)),
                UsersDropdown("Users", users,
                    deleteCallback: (user) => removeUser(user),
                    addCallback: (user) => addUser(user)),
                RaisedButton.icon(
                    onPressed: validateInput,
                    icon: Icon(Icons.add),
                    label: Text("Create Group"))
              ],
            ),
          ],
        ),
      ),
    );
  }

  void selectCategory(Category category) {
    setState(() {
      if (categoriesToAdd.contains(category)) {
        categoriesToAdd.remove(category);
      } else {
        categoriesToAdd.add(category);
      }
    });
  }

  void addUser(String user) {
    setState(() {
      users.add(user);
    });
  }

  void removeUser(String user) {
    setState(() {
      users.remove(user);
    });
  }

  void validateInput() {
    final form = _formKey.currentState;
    if (form.validate()) {
      if (categoriesToAdd.isEmpty) {
        showErrorMessage("Invalid input",
            "Must have at least one category in the group!", context);
      } else {
        form.save();
        users.add(Globals.username); // creator is obviously always in the group
        // convert the lists to maps for the json object that is sent to db
        Map<String, String> categoriesMap = new Map<String, String>();
        for (int i = 0; i < categoriesToAdd.length; i++) {
          categoriesMap.putIfAbsent(categoriesToAdd[i].categoryId,
              () => categoriesToAdd[i].categoryName);
        }
        Map<String, String> usersMap = new Map<String, String>();
        for (int i = 0; i < users.length; i++) {
          usersMap.putIfAbsent(users[i], () => users[i]);
        }
        print(categoriesMap);
        Group group = new Group(
            groupName: _groupName,
            groupCreator: Globals.username,
            groupIcon: _groupIcon,
            groupId: "Generate on backend",
            categories: categoriesMap,
            members: usersMap,
            defaultPollDuration: _pollDuration,
            defaultPollPassPercent: _pollPassPercent);
        print(group);
        // it's okay to not have any members, since creator is guaranteed to be there
        // TODO create group and upload to DB
      }
    } else {
      setState(() => _autoValidate = true);
    }
  }
}
