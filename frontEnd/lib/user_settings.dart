import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/widgets/user_row.dart';

class UserSettings extends StatefulWidget {
  UserSettings({Key key}) : super(key: key);

  @override
  _UserSettingsState createState() => _UserSettingsState();
}

class _UserSettingsState extends State<UserSettings> {
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  final TextEditingController nickNameController = TextEditingController();
  final TextEditingController userIconController = TextEditingController();
  final TextEditingController contactsController = TextEditingController();

  bool autoValidate = false;
  bool editing = false;
  bool _darkTheme = false;
  bool _muted = false;
  String userIcon;
  String name;
  List<String> newContacts = new List<String>();
  List<String> originalContacts = new List<String>();
  int _groupSort = 0;

  @override
  void dispose() {
    nickNameController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    name = Globals.user.displayName;
    _darkTheme = Globals.user.appSettings.darkTheme;
    _groupSort = Globals.user.appSettings.groupSort;
    _muted = Globals.user.appSettings.muted;
    nickNameController.text = name;
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        resizeToAvoidBottomInset: true,
        appBar: AppBar(
          title: Text("My Settings"),
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
                      Container(
                          width: MediaQuery.of(context).size.width * .6,
                          child: TextFormField(
                            maxLength: 50,
                            controller: nickNameController,
                            validator: validName,
                            onChanged: (String arg) {
                              name = arg;
                              enableAutoValidation();
                            },
                            onSaved: (String arg) {},
                            style: TextStyle(
                                fontSize: DefaultTextStyle.of(context)
                                        .style
                                        .fontSize *
                                    0.6),
                            decoration: InputDecoration(
                                labelText: "Name", counterText: ""),
                          )),
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
                                fit: BoxFit.fitHeight,
                                image: AssetImage(
                                    'assets/images/placeholder.jpg'))),
                        child: Container(
                          decoration: BoxDecoration(
                              color: Colors.grey.withOpacity(0.7),
                              shape: BoxShape.circle),
                          child: IconButton(
                            icon: Icon(Icons.edit),
                            color: Colors.blueAccent,
                            onPressed: () {
                              userIconPopup();
                            },
                          ),
                        ),
                      ),
                      Padding(
                        padding: EdgeInsets.all(
                            MediaQuery.of(context).size.height * .004),
                      ),
                      RaisedButton.icon(
                          onPressed: () {
                            contactsPopup();
                          },
                          icon: Icon(Icons.contacts),
                          label: Text("My Contacts")),
                      Container(
                        width: MediaQuery.of(context).size.width * .7,
                        child: Column(
                          children: <Widget>[
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                              children: <Widget>[
                                Expanded(
                                  child: Text(
                                    "Mute Notifcations",
                                    style: TextStyle(
                                        fontSize: DefaultTextStyle.of(context)
                                                .style
                                                .fontSize *
                                            0.4),
                                  ),
                                ),
                                Switch(
                                  value: _muted,
                                  onChanged: (bool value) {
                                    setState(() {
                                      _muted = value;
                                      enableAutoValidation();
                                    });
                                  },
                                )
                              ],
                            ),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                              children: <Widget>[
                                Expanded(
                                  child: Text(
                                    "Dark Theme",
                                    style: TextStyle(
                                        fontSize: DefaultTextStyle.of(context)
                                                .style
                                                .fontSize *
                                            0.4),
                                  ),
                                ),
                                Switch(
                                  value: _darkTheme,
                                  onChanged: (bool value) {
                                    setState(() {
                                      _darkTheme = value;
                                      enableAutoValidation();
                                    });
                                  },
                                )
                              ],
                            ),
                          ],
                        ),
                      ),
                      Container(
                        // have to do this hack because the expansiontile title wouldn't line up
                        width: MediaQuery.of(context).size.width * .78,
                        child: ExpansionTile(
                          title: Text(
                            "Group Sort Method",
                            style: TextStyle(
                                fontSize: DefaultTextStyle.of(context)
                                        .style
                                        .fontSize *
                                    0.4),
                            textAlign: TextAlign.left,
                          ),
                          children: <Widget>[
                            SizedBox(
                              height: MediaQuery.of(context).size.height * .12,
                              child: ListView(
                                shrinkWrap: true,
                                children: <Widget>[
                                  Row(
                                    children: <Widget>[
                                      Radio(
                                        value: Globals.alphabeticalSort,
                                        groupValue: _groupSort,
                                        onChanged: selectGroupSort,
                                      ),
                                      Text("Alphabetical")
                                    ],
                                  ),
                                  Row(
                                    children: <Widget>[
                                      Radio(
                                        value: Globals.dateSort,
                                        groupValue: _groupSort,
                                        onChanged: selectGroupSort,
                                      ),
                                      Text("Date")
                                    ],
                                  ),
                                ],
                              ),
                            ),
                          ],
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

  void selectGroupSort(int val) {
    _groupSort = val;
    enableAutoValidation();
  }

  void enableAutoValidation() {
    // the moment the user makes changes to their previously saved settings, display the save button
    Set newContactsSet = newContacts.toSet();
    Set oldContactsSet = originalContacts.toSet();
    bool newUsers = !(oldContactsSet.containsAll(newContactsSet));
    if (Globals.user.appSettings.darkTheme != _darkTheme ||
        Globals.user.appSettings.muted != _muted ||
        Globals.user.displayName != name ||
        Globals.user.appSettings.groupSort != _groupSort ||
        newUsers) {
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
    if (form.validate()) {
      form.save();
      setState(() {
        Globals.user.appSettings.groupSort = _groupSort;
        Globals.user.appSettings.muted = _muted;
        Globals.user.appSettings.darkTheme = _darkTheme;
        Globals.user.displayName = name;
        UsersManager.updateUserAppSettings(name, boolToInt(_darkTheme),
            boolToInt(_muted), _groupSort, context);
        // reset everything and reflect changes made
        editing = false;
        autoValidate = false;
      });
    } else {
      setState(() => autoValidate = true);
    }
  }

  void userIconPopup() {
    // displays a popup for editing the user's icon's
    showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: Text("Edit User Icon url"),
            actions: <Widget>[
              FlatButton(
                child: Text("Cancel"),
                onPressed: () {
                  userIconController.clear();
                  Navigator.of(context).pop();
                },
              ),
              FlatButton(
                child: Text("Submit"),
                onPressed: () {
                  userIcon = userIconController.text;
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                },
              ),
            ],
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                TextFormField(
                    controller: userIconController,
                    validator: validGroupIcon,
                    keyboardType: TextInputType.url,
                    decoration: InputDecoration(
                      labelText: "Enter a icon link",
                    )),
              ],
            ),
          );
        });
  }

  void contactsPopup() {
    // displays a popup for editing the user's contacts
    final GlobalKey<FormState> formKey = GlobalKey<FormState>();
    showDialog(
        context: context,
        builder: (context) {
          return StatefulBuilder(
            builder: (context, setState) {
              return Form(
                key: formKey,
                child: AlertDialog(
                  title: Text("My Contacts"),
                  actions: <Widget>[
                    FlatButton(
                      child: Text("Back"),
                      onPressed: () {
                        contactsController.clear();
                        Navigator.of(context, rootNavigator: true)
                            .pop('dialog');
                        enableAutoValidation();
                      },
                    ),
                    FlatButton(
                      child: Text("Add User"),
                      onPressed: () {
                        if (formKey.currentState.validate()) {
                          newContacts.add(contactsController.text.trim());
                          contactsController.clear();
                          setState(() {
                            editing = true;
                          });
                        }
                      },
                    ),
                  ],
                  content: SingleChildScrollView(
                    scrollDirection: Axis.vertical,
                    child: Container(
                      width: double.maxFinite,
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: <Widget>[
                          TextFormField(
                              controller: contactsController,
                              validator: (value) {
                                return validNewContact(
                                    value.trim(), newContacts);
                              },
                              decoration: InputDecoration(
                                labelText: "Enter a username to add",
                              )),
                          Container(
                            height: MediaQuery.of(context).size.height * .25,
                            child: ListView.builder(
                                shrinkWrap: true,
                                itemCount: newContacts.length,
                                itemBuilder: (context, index) {
                                  return UserRow(newContacts[index],
                                      newContacts[index], true, deleteUser: () {
                                    newContacts.remove(newContacts[index]);
                                    setState(() {});
                                  });
                                }),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              );
            },
          );
        }).then((val) {
          // this is called whenever the user clicks outside the alert dialog or hits the back button
      contactsController.clear();
      enableAutoValidation();
    });
  }
}