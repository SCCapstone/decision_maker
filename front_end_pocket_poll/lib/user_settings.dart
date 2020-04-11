import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:image_picker/image_picker.dart';
import 'dart:io';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/favorite.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'package:front_end_pocket_poll/utilities/validator.dart';
import 'package:front_end_pocket_poll/widgets/favorites_page.dart';

class UserSettings extends StatefulWidget {
  UserSettings({Key key}) : super(key: key);

  @override
  _UserSettingsState createState() => _UserSettingsState();
}

class _UserSettingsState extends State<UserSettings> {
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  final TextEditingController displayNameController = TextEditingController();

  bool autoValidate;
  bool editing;
  bool _darkTheme;
  bool _muted;
  bool newIcon;
  File _icon;
  String _displayName;
  List<Favorite> displayedFavorites;
  List<Favorite> originalFavorites;

  @override
  void dispose() {
    this.displayNameController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    this.autoValidate = false;
    this.editing = false;
    this.newIcon = false;
    this.displayedFavorites = new List<Favorite>();
    this.originalFavorites = new List<Favorite>();

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
                    onPressed: saveSettings,
                    key: Key("user_settings:save_button"),
                    icon: Icon(Icons.save),
                    label: Text("Save")),
              )
            ],
          ),
          key: Key("user_settings:scaffold"),
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
                              maxLength: Globals.maxDisplayNameLength,
                              controller: this.displayNameController,
                              validator: validDisplayName,
                              onChanged: (String arg) {
                                this._displayName = arg.trim();
                                showSaveButton();
                              },
                              key: Key("user_settings:displayName_input"),
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
                                    ? getUserIconImage(Globals.user.icon)
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
                                        ? getUserIconImage(Globals.user.icon)
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
                            key: Key("user_settings:favorites_button"),
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
                                      overflow: TextOverflow.ellipsis,
                                      style: TextStyle(fontSize: 20),
                                    ),
                                  ),
                                  Switch(
                                    value: this._muted,
                                    onChanged: (bool value) {
                                      setState(() {
                                        this._muted = value;
                                        showSaveButton();
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
                                      overflow: TextOverflow.ellipsis,
                                      style: TextStyle(fontSize: 20),
                                    ),
                                  ),
                                  Switch(
                                    value: !this._darkTheme,
                                    key: Key("user_settings:dark_theme_switch"),
                                    onChanged: (bool value) {
                                      setState(() {
                                        this._darkTheme = !value;
                                        showSaveButton();
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

  // uses the OS of the device to pick an image, we compress it before sending it
  Future getImage() async {
    File newIconFile = await ImagePicker.pickImage(
        source: ImageSource.gallery,
        imageQuality: 75,
        maxWidth: 600,
        maxHeight: 600);

    if (newIconFile != null) {
      this._icon = newIconFile;
      this.newIcon = true;
      showSaveButton();
    }
  }

  // if the favorites have changed, attempt to update them in the DB
  void saveFavorites() async {
    Set oldFavorites = this.originalFavorites.toSet();
    Set newFavorites = this.displayedFavorites.toSet();
    bool changedFavorites = !(oldFavorites.containsAll(newFavorites) &&
        oldFavorites.length == newFavorites.length);
    if (changedFavorites) {
      List<String> userNames = new List<String>();
      for (Favorite favorite in this.displayedFavorites) {
        userNames.add(favorite.username);
      }
      ResultStatus resultStatus = await UsersManager.updateUserSettings(
          Globals.user.displayName,
          Globals.user.appSettings.darkTheme,
          Globals.user.appSettings.muted,
          userNames,
          null);
      if (resultStatus.success) {
        this.originalFavorites.clear();
        this.originalFavorites.addAll(this.displayedFavorites);
      } else {
        // if it failed then revert back to old favorites
        this.displayedFavorites.clear();
        this.displayedFavorites.addAll(this.originalFavorites);
        showErrorMessage("Error", "Error saving favorites.", this.context);
      }
    }
  }

  // the moment the user makes changes to their previously saved settings, display the save button
  void showSaveButton() {
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

  // attempts to save the user settings if the input is valid
  void saveSettings() async {
    final form = this.formKey.currentState;
    if (form.validate()) {
      form.save();
      List<String> userNames = new List<String>();
      for (Favorite favorite in this.displayedFavorites) {
        userNames.add(favorite.username);
      }

      showLoadingDialog(this.context, "Saving settings...", true);
      ResultStatus resultStatus = await UsersManager.updateUserSettings(
          this._displayName,
          this._darkTheme,
          this._muted,
          userNames,
          this._icon);
      Navigator.of(this.context, rootNavigator: true).pop('dialog');

      if (resultStatus.success) {
        setState(() {
          hideKeyboard(this.context);
          // reset everything and reflect changes made
          this.originalFavorites.clear();
          this.originalFavorites.addAll(this.displayedFavorites);
          this.editing = false;
          this.newIcon = false;
          this.autoValidate = false;
          changeTheme(this.context);
        });
      } else {
        hideKeyboard(this.context);
        showErrorMessage("Error", resultStatus.errorMessage, this.context);
      }
    } else {
      setState(() => this.autoValidate = true);
    }
  }
}
