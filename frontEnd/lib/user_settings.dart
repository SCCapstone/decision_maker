import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:image_picker/image_picker.dart';
import 'dart:io';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/favorite.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/widgets/favorites_page.dart';

class UserSettings extends StatefulWidget {
  UserSettings({Key key}) : super(key: key);

  @override
  _UserSettingsState createState() => _UserSettingsState();
}

class _UserSettingsState extends State<UserSettings> {
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  final TextEditingController displayNameController = TextEditingController();

  bool autoValidate = false;
  bool editing = false;
  bool _darkTheme = false;
  bool _muted = false;
  bool newIcon = false;
  File _icon;
  String _displayName;
  List<Favorite> displayedFavorites = new List<Favorite>();
  List<Favorite> originalFavorites = new List<Favorite>();

  @override
  void dispose() {
    this.displayNameController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    this._displayName = Globals.user.displayName;
    this._darkTheme = Globals.user.appSettings.darkTheme;
    this._muted = Globals.user.appSettings.muted;
    this.originalFavorites = Globals.user.favorites;
    this.displayedFavorites.addAll(this.originalFavorites);
    this.displayNameController.text = this._displayName;
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
                visible: this.editing,
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
              key: this.formKey,
              autovalidate: this.autoValidate,
              child: Expanded(
                child: ListView(
                  shrinkWrap: true,
                  padding: EdgeInsets.all(10.0),
                  children: <Widget>[
                    Column(
                      children: [
                        Container(
                            width: MediaQuery.of(context).size.width * .8,
                            child: TextFormField(
                              maxLength: 50,
                              controller: this.displayNameController,
                              validator: validName,
                              onChanged: (String arg) {
                                this._displayName = arg.trim();
                                enableAutoValidation();
                              },
                              onSaved: (String arg) {},
                              style: TextStyle(fontSize: 25),
                              decoration: InputDecoration(
                                  labelText: "Nickname (@${Globals.username})",
                                  counterText: ""),
                            )),
                        Padding(
                          padding: EdgeInsets.all(
                              MediaQuery.of(context).size.height * .01),
                        ),
                        GestureDetector(
                          onTap: () {
                            showUserImage(
                                this._icon == null
                                    ? getUserIconUrl(Globals.user)
                                    : FileImage(this._icon),
                                context);
                          },
                          child: Container(
                            width: MediaQuery.of(context).size.width * .65,
                            height: MediaQuery.of(context).size.height * .35,
                            alignment: Alignment.topRight,
                            decoration: BoxDecoration(
                                image: DecorationImage(
                                    fit: BoxFit.cover,
                                    image: this._icon == null
                                        ? getUserIconUrl(Globals.user)
                                        : FileImage(this._icon))),
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
                        RaisedButton.icon(
                            onPressed: () {
                              Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                      builder: (context) => FavoritesPage(
                                          this.displayedFavorites))).then((_) {
                                saveFavorites();
                              });
                            },
                            icon: Icon(Icons.contacts),
                            label: Text("My Favorites")),
                        Container(
                          width: MediaQuery.of(context).size.width * .8,
                          child: Column(
                            children: <Widget>[
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceEvenly,
                                children: <Widget>[
                                  Expanded(
                                    child: AutoSizeText(
                                      "Mute Notifcations",
                                      minFontSize: 14,
                                      maxLines: 1,
                                      style: TextStyle(fontSize: 20),
                                    ),
                                  ),
                                  Switch(
                                    value: this._muted,
                                    onChanged: (bool value) {
                                      setState(() {
                                        this._muted = value;
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
                                    child: AutoSizeText(
                                      "Light Theme",
                                      minFontSize: 14,
                                      maxLines: 1,
                                      style: TextStyle(fontSize: 20),
                                    ),
                                  ),
                                  Switch(
                                    value: !this._darkTheme,
                                    onChanged: (bool value) {
                                      setState(() {
                                        this._darkTheme = !value;
                                        enableAutoValidation();
                                      });
                                    },
                                  )
                                ],
                              ),
                            ],
                          ),
                        ),
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
      this._icon = newIconFile;
      this.newIcon = true;
      enableAutoValidation();
    }
  }

  void saveFavorites() async {
    List<String> userNames = new List<String>();
    for (Favorite favorite in this.displayedFavorites) {
      userNames.add(favorite.username);
    }
    ResultStatus resultStatus = await UsersManager.updateUserSettings(
        Globals.user.displayName,
        boolToInt(Globals.user.appSettings.darkTheme),
        boolToInt(Globals.user.appSettings.muted),
        Globals.user.appSettings.groupSort,
        userNames,
        null);
    if (resultStatus.success) {
      this.originalFavorites.clear();
      this.originalFavorites.addAll(this.displayedFavorites);
    } else {
      // if it failed then revert back to old favorites
      this.displayedFavorites.clear();
      this.displayedFavorites.addAll(this.originalFavorites);
      showErrorMessage("Error", "Error saving favorites.", context);
    }
  }

  void enableAutoValidation() {
    // the moment the user makes changes to their previously saved settings, display the save button
    if (Globals.user.appSettings.darkTheme != this._darkTheme ||
        Globals.user.appSettings.muted != this._muted ||
        Globals.user.displayName != this._displayName ||
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

  void validateInput() async {
    final form = this.formKey.currentState;
    if (form.validate()) {
      form.save();
      List<String> userNames = new List<String>();
      for (Favorite favorite in this.displayedFavorites) {
        userNames.add(favorite.username);
      }

      showLoadingDialog(context, "Saving settings...", true);
      ResultStatus resultStatus = await UsersManager.updateUserSettings(
          this._displayName,
          boolToInt(this._darkTheme),
          boolToInt(this._muted),
          Globals.user.appSettings.groupSort,
          userNames,
          this._icon);
      Navigator.of(context, rootNavigator: true).pop('dialog');

      if (resultStatus.success) {
        setState(() {
          hideKeyboard(context);
          // reset everything and reflect changes made
          this.originalFavorites.clear();
          this.originalFavorites.addAll(this.displayedFavorites);
          this.editing = false;
          this.newIcon = false;
          this.autoValidate = false;
          changeTheme(context);
        });
      } else {
        hideKeyboard(context);
        showErrorMessage("Error", resultStatus.errorMessage, context);
      }
    } else {
      setState(() => this.autoValidate = true);
    }
  }
}
