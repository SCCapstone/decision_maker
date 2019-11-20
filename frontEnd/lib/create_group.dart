import 'package:flutter/material.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';

import 'imports/dev_testing_manager.dart';

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
  List<String> usersToAdd = new List<String>();
  ScrollController _scrollController;

  final _formKey = GlobalKey<FormState>();
  final _groupNameController = TextEditingController();
  final _imageLinkController = TextEditingController();
  final _pollPassController = TextEditingController();
  final _pollDurationController = TextEditingController();

  @override
  void dispose() {
    _scrollController = ScrollController();
    _groupNameController.dispose();
    _imageLinkController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    List<Category> categories = new List<Category>();
    for (int i = 0; i < 20; i++) {
      Category category = new Category.debug("123", "$i",
          new Map<String, dynamic>(), new Map<String, dynamic>(), 1, "testing");
      categories.add(category);
    }
    return Scaffold(
      appBar: AppBar(
        title: Text("Add New Group"),
      ),
      body: Form(
        key: _formKey,
        autovalidate: _autoValidate,
        child: ListView(
          controller: _scrollController,
          shrinkWrap: true,
          padding: EdgeInsets.all(20),
          children: <Widget>[
            Column(
              children: [
                TextFormField(
                  controller: _groupNameController,
                  validator: validGroupName,
                  decoration: InputDecoration(
                    labelText: "Enter a group name",
                  ),
                ),
                TextFormField(
                  controller: _imageLinkController,
                  keyboardType: TextInputType.url,
                  validator: validGroupIcon,
                  decoration: InputDecoration(
                    labelText: "Enter a icon link",
                  ),
                ),
                TextFormField(
                  controller: _pollDurationController,
                  keyboardType: TextInputType.number,
                  validator: validPollDuration,
                  decoration: InputDecoration(
                    labelText: "Enter a default poll duration (in minutes)",
                  ),
                ),
                TextFormField(
                  controller: _pollPassController,
                  keyboardType: TextInputType.number,
                  validator: validPassPercentage,
                  decoration: InputDecoration(
                    labelText: "Enter a default poll pass percentage (0-100)",
                  ),
                ),
                CategoryDropdown("Add categories", categories,
                    callback: (category) => selectCategory(category)),
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
      print(category);
    });
  }

  void validateInput() {
    final form = _formKey.currentState;
    if (form.validate()) {
      // b/c url is entered in a popup dialog, can't share the same form so must use another flag
      form.save();
      // TODO create Group and upload to DB
      print(
          "Group name: $_groupName Duration: $_pollDuration  Percentage: $_pollPassPercent GroupIcon: $_groupIcon");
    } else {
      setState(() => _autoValidate = true);
    }
  }
}
