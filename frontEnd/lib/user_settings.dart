import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'dart:io';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/favorite.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/widgets/favorites_popup.dart';

class UserSettings extends StatefulWidget {
  UserSettings({Key key}) : super(key: key);

  @override
  _UserSettingsState createState() => _UserSettingsState();
}

class _UserSettingsState extends State<UserSettings> {
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  final TextEditingController displayNameController = TextEditingController();
  final TextEditingController userIconController = TextEditingController();

  bool autoValidate = false;
  bool editing = false;
  bool _darkTheme = false;
  bool _muted = false;
  bool newIcon = false;
  File _icon;
  String _displayName;
  int _groupSort = 0;
  List<Favorite> displayedFavorites = new List<Favorite>();
  List<Favorite> originalFavorites = new List<Favorite>();

  @override
  void dispose() {
    displayNameController.dispose();
    userIconController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    _displayName = Globals.user.displayName;
    _darkTheme = Globals.user.appSettings.darkTheme;
    _groupSort = Globals.user.appSettings.groupSort;
    _muted = Globals.user.appSettings.muted;
    originalFavorites = Globals.user.favorites;
    displayedFavorites.addAll(originalFavorites);
    displayNameController.text = _displayName;
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
                              controller: displayNameController,
                              validator: validName,
                              onChanged: (String arg) {
                                _displayName = arg.trim();
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
                                  fit: BoxFit.cover,
                                  image: _icon == null
                                      ? getUserIconUrl(Globals.user)
                                      : FileImage(_icon))),
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
                        RaisedButton.icon(
                            onPressed: () {
                              showFavoritesPopup();
                            },
                            icon: Icon(Icons.contacts),
                            label: Text("My Favorites")),
                        Container(
                          width: MediaQuery.of(context).size.width * .7,
                          child: Column(
                            children: <Widget>[
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceEvenly,
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
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceEvenly,
                                children: <Widget>[
                                  Expanded(
                                    child: Text(
                                      "Light Theme",
                                      style: TextStyle(
                                          fontSize: DefaultTextStyle.of(context)
                                                  .style
                                                  .fontSize *
                                              0.4),
                                    ),
                                  ),
                                  Switch(
                                    value: !_darkTheme,
                                    onChanged: (bool value) {
                                      setState(() {
                                        _darkTheme = !value;
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
                                height:
                                    MediaQuery.of(context).size.height * .12,
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
      _icon = newIconFile;
      newIcon = true;
      enableAutoValidation();
    }
  }

  void showFavoritesPopup() {
    showDialog(
        context: context,
        child: FavoritesPopup(displayedFavorites, originalFavorites,
            handlePopupClosed: () => popupClosed())).then((val) {
      popupClosed();
    });
  }

  void popupClosed() {
    enableAutoValidation();
    hideKeyboard(context);
  }

  void selectGroupSort(int val) {
    _groupSort = val;
    enableAutoValidation();
  }

  void enableAutoValidation() {
    // the moment the user makes changes to their previously saved settings, display the save button
    Set newContactsSet = displayedFavorites.toSet();
    Set oldContactsSet = originalFavorites.toSet();
    // check if the user added or removed any users from their favorites list
    bool newUsers = !(oldContactsSet.containsAll(newContactsSet) &&
        oldContactsSet.length == newContactsSet.length);
    if (Globals.user.appSettings.darkTheme != _darkTheme ||
        Globals.user.appSettings.muted != _muted ||
        Globals.user.displayName != _displayName ||
        Globals.user.appSettings.groupSort != _groupSort ||
        newUsers ||
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
    if (form.validate()) {
      form.save();
      Globals.user.appSettings.groupSort = _groupSort;
      Globals.user.appSettings.muted = _muted;
      Globals.user.appSettings.darkTheme = _darkTheme;
      Globals.user.displayName = _displayName;
      Globals.user.favorites = displayedFavorites;
      List<String> userNames = new List<String>();
      for (Favorite favorite in displayedFavorites) {
        userNames.add(favorite.username);
      }

      showLoadingDialog(
          context, "Saving settings...", true); // show loading dialog
      await UsersManager.updateUserSettings(
          _displayName,
          boolToInt(_darkTheme),
          boolToInt(_muted),
          _groupSort,
          userNames,
          _icon,
          context); // blind send for now?
      Navigator.of(context, rootNavigator: true)
          .pop('dialog'); // dismiss the loading dialog

      setState(() {
        // reset everything and reflect changes made
        originalFavorites.clear();
        originalFavorites.addAll(displayedFavorites);
        editing = false;
        newIcon = false;
        autoValidate = false;
        changeTheme(context);
      });
    } else {
      setState(() => autoValidate = true);
    }
  }
}
