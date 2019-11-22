import 'package:flutter/material.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/widgets/category_dropdown.dart';
import 'package:frontEnd/widgets/users_dropdown.dart';

class CreateGroup extends StatefulWidget {
  Future<List<Category>> categoriesTotal;

  @override
  _CreateGroupState createState() => _CreateGroupState();
}

class _CreateGroupState extends State<CreateGroup> {
  bool autoValidate = false;
  String groupName;
  String groupIcon;
  int pollPassPercent;
  int pollDuration;
  List<Category> categoriesToAdd = new List<Category>();
  List<String> users;

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
    categoriesToAdd = new List<Category>();
    widget.categoriesTotal =
        CategoriesManager.getAllCategoriesList(Globals.username);
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
                    groupName = arg;
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
                    groupIcon = arg;
                  },
                  decoration: InputDecoration(
                    labelText: "Enter a icon link",
                  ),
                ),
                TextFormField(
                  controller: pollDurationController,
                  keyboardType: TextInputType.number,
                  validator: validPollDuration,
                  onSaved: (String arg) {
                    pollDuration = int.parse(arg);
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
                    pollPassPercent = int.parse(arg);
                  },
                  decoration: InputDecoration(
                    labelText: "Enter a default poll pass percentage (0-100)",
                  ),
                ),
                FutureBuilder(
                    future: widget.categoriesTotal,
                    builder: (BuildContext context, AsyncSnapshot snapshot) {
                      if (snapshot.hasData) {
                        List<Category> categories = snapshot.data;
                        return CategoryDropdown(
                            "Add categories", categories, categoriesToAdd,
                            callback: (category) => selectCategory(category));
                      } else if (snapshot.hasError) {
                        print(snapshot.error);
                        return Text("Error: ${snapshot.error}");
                      } else {
                        return Center(child: CircularProgressIndicator());
                      }
                    }),
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
    final form = formKey.currentState;
    if (form.validate()) {
      if (categoriesToAdd.isNotEmpty) {
        form.save();
        users.add(Globals.username); // creator is obviously always in the group
        // convert the lists to maps for the json object that is sent to db
        Map<String, String> categoriesMap = new Map<String, String>();
        for (int i = 0; i < categoriesToAdd.length; i++) {
          categoriesMap.putIfAbsent(categoriesToAdd[i].categoryId,
              () => categoriesToAdd[i].categoryName);
        }
        // it's okay to not have any inputted members, since creator is guaranteed to be there
        Map<String, String> usersMap = new Map<String, String>();
        for (int i = 0; i < users.length; i++) {
          usersMap.putIfAbsent(users[i], () => users[i]);
        }
        Group group = new Group(
            groupName: groupName,
            groupCreator: Globals.username,
            groupIcon: groupIcon,
            groupId: "Generate on backend",
            categories: categoriesMap,
            members: usersMap,
            defaultPollDuration: pollDuration,
            defaultPollPassPercent: pollPassPercent);
        print(group);
        // TODO upload to DB (https://github.com/SCCapstone/decision_maker/issues/116)
        setState(() {
          // reset everything
          groupNameController.clear();
          groupIconController.clear();
          categoriesToAdd.clear();
          users.clear();
          pollDurationController.clear();
          pollPassController.clear();
        });
      } else {
        showErrorMessage("Invalid input",
            "Must have at least one category in the group!", context);
      }
    } else {
      setState(() => autoValidate = true);
    }
  }
}
