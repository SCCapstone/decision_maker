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

class UserSettings extends StatefulWidget {
  UserSettings({Key key}) : super(key: key);

  @override
  _UserSettingsState createState() => _UserSettingsState();
}

class _UserSettingsState extends State<UserSettings> {
  bool autoValidate = false;
  bool validGroupIcon = true;
  bool editing = false;
  bool darkModeEnabled = false;
  bool muted = false;
  String userIcon;

  final List<Category> categoriesSelected = new List<Category>();
  final formKey = GlobalKey<FormState>();
  final nickNameController = TextEditingController();
  final userIconController = TextEditingController();

  @override
  void dispose() {
    nickNameController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    nickNameController.text = Globals.username;
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        resizeToAvoidBottomInset: true,
        appBar: AppBar(
          title: Text("User Settings"),
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
                        controller: nickNameController,
                        validator: validGroupName,
                        onChanged: (String arg) {
                          // the moment the user starts making changes, display the save button
                          enableAutoValidation(
                              !(arg == Globals.username));
                        },
                        onSaved: (String arg) {
                        },
                        style: TextStyle(
                            fontSize:
                            DefaultTextStyle.of(context).style.fontSize *
                                0.8),
                        decoration: InputDecoration(labelText: "Name"),
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
                                image: AssetImage('assets/images/placeholder.jpg'))),
                        child: Container(
                          decoration: BoxDecoration(
                              color: Colors.grey.withOpacity(0.7),
                              shape: BoxShape.circle),
                          child: IconButton(
                            icon: Icon(Icons.edit),
                            color: Colors.blueAccent,
                            onPressed: () {
                              groupIconPopup(context, autoValidate,
                                  userIconController, updateIcon);
                            },
                          ),
                        ),
                      ),
                      Padding(
                        padding: EdgeInsets.all(
                            MediaQuery.of(context).size.height * .004),
                      ),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: <Widget>[
                          Text(
                            "Sort",
                            style: TextStyle(
                                fontSize: DefaultTextStyle.of(context)
                                    .style
                                    .fontSize *
                                    0.4),
                          ),
                          Switch(
                            value: true,
                          ),
                        ],
                      ),
                      Padding(
                        padding: EdgeInsets.all(
                            MediaQuery.of(context).size.height * .004),
                      ),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: <Widget>[
                          Text(
                            "Mute Notifcations",
                            style: TextStyle(
                                fontSize: DefaultTextStyle.of(context)
                                    .style
                                    .fontSize *
                                    0.4),
                          ),
                          Switch(
                            value: true,
                          )
                        ],
                      ),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: <Widget>[
                          Text(
                            "Dark Theme",
                            style: TextStyle(
                                fontSize: DefaultTextStyle.of(context)
                                    .style
                                    .fontSize *
                                    0.4),
                          ),
                          Switch(
                            value: darkModeEnabled,
                            onChanged: (bool value){
                              setState(() {
                                darkModeEnabled = value;
                              });
                            },
                          )
                        ],
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ]));
  }

  void enableAutoValidation(bool val) {
    setState(() {
      editing = val;
    });
  }


  void updateIcon(String iconUrl) {
    setState(() {
      userIcon = iconUrl;
      userIconController.clear();
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
      setState(() {
        // reset everything and reflect changes made
        editing = false;
        autoValidate = false;
      });
    } else {
      setState(() => autoValidate = true);
    }
  }
}
