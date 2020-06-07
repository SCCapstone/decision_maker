import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:image_cropper/image_cropper.dart';
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
  final TextEditingController votingDurationController =
      new TextEditingController();
  final TextEditingController considerDurationController =
      new TextEditingController();
  final ScrollController listViewController = new ScrollController();

  bool autoValidate;
  bool editing;
  bool _darkTheme;
  bool _muted;
  bool newIcon;
  int votingDuration;
  int considerDuration;
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
    this.votingDuration = Globals.user.appSettings.defaultVotingDuration;
    this.considerDuration = Globals.user.appSettings.defaultConsiderDuration;
    this.votingDurationController.text = this.votingDuration.toString();
    this.considerDurationController.text = this.considerDuration.toString();

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
            title: Text("Settings"),
            actions: <Widget>[
              Visibility(
                visible: this.editing,
                child: FlatButton(
                  child: Text(
                    "SAVE",
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                  key: Key("user_settings:save_button"),
                  textColor: Colors.white,
                  onPressed: saveSettings,
                ),
              )
            ],
          ),
          key: Key("user_settings:scaffold"),
          body: Column(children: <Widget>[
            Form(
              key: this.formKey,
              autovalidate: this.autoValidate,
              child: Expanded(
                child: Scrollbar(
                  controller: this.listViewController,
                  child: ListView(
                    controller: this.listViewController,
                    shrinkWrap: true,
                    padding: EdgeInsets.all(10.0),
                    children: <Widget>[
                      Column(
                        children: [
                          Container(
                              width: MediaQuery.of(context).size.width * .90,
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
                                style: TextStyle(fontSize: 20),
                                decoration: InputDecoration(
                                    labelText:
                                        "Nickname (@${Globals.user.username})",
                                    counterText: ""),
                              )),
                          Padding(
                            padding: EdgeInsets.all(
                                MediaQuery.of(context).size.height * .01),
                          ),
                          GestureDetector(
                            onTap: () {
                              showActiveUserImage(
                                  this._icon == null
                                      ? getUserIconImage(Globals.user.icon)
                                      : FileImage(this._icon),
                                  context);
                            },
                            child: Container(
                              width: MediaQuery.of(context).size.width * .6,
                              height: MediaQuery.of(context).size.height * .3,
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
                          Container(
                            width: MediaQuery.of(context).size.width * .9,
                            child: Column(
                              children: <Widget>[
                                ListTileTheme(
                                  contentPadding:
                                      EdgeInsets.fromLTRB(0, 0, 10.0, 0),
                                  child: ExpansionTile(
                                    title: AutoSizeText(
                                      "Default Event Durations",
                                      minFontSize: 14,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: TextStyle(fontSize: 20),
                                    ),
                                    key: Key(
                                        "user_settings:default_durations_button"),
                                    children: <Widget>[
                                      ConstrainedBox(
                                          constraints: BoxConstraints(
                                            maxHeight: MediaQuery.of(context)
                                                    .size
                                                    .height *
                                                .2,
                                          ),
                                          child: ListView(
                                            children: <Widget>[
                                              TextFormField(
                                                maxLength:
                                                    Globals.maxConsiderDigits,
                                                keyboardType:
                                                    TextInputType.number,
                                                validator: (value) {
                                                  return validConsiderDuration(
                                                      value, false);
                                                },
                                                key: Key(
                                                    "user_settings:conider_input"),
                                                controller: this
                                                    .considerDurationController,
                                                onChanged: (String arg) {
                                                  try {
                                                    this.considerDuration =
                                                        int.parse(arg);
                                                    showSaveButton();
                                                  } catch (e) {
                                                    this.autoValidate = true;
                                                  }
                                                },
                                                onSaved: (String arg) {
                                                  this.considerDuration =
                                                      int.parse(arg);
                                                },
                                                decoration: InputDecoration(
                                                    labelText:
                                                        "Consider Duration (mins)",
                                                    helperText: " ",
                                                    counterText: ""),
                                              ),
                                              TextFormField(
                                                maxLength:
                                                    Globals.maxVotingDigits,
                                                keyboardType:
                                                    TextInputType.number,
                                                validator: (value) {
                                                  return validVotingDuration(
                                                      value, false);
                                                },
                                                key: Key(
                                                    "user_settings:vote_input"),
                                                controller: this
                                                    .votingDurationController,
                                                onChanged: (String arg) {
                                                  try {
                                                    this.votingDuration =
                                                        int.parse(arg);
                                                    showSaveButton();
                                                  } catch (e) {
                                                    this.autoValidate = true;
                                                  }
                                                },
                                                onSaved: (String arg) {
                                                  this.votingDuration =
                                                      int.parse(arg);
                                                },
                                                decoration: InputDecoration(
                                                    labelText:
                                                        "Voting Duration (mins)",
                                                    helperText: " ",
                                                    counterText: ""),
                                              ),
                                            ],
                                          )),
                                    ],
                                  ),
                                ),
                                Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceAround,
                                  children: <Widget>[
                                    Expanded(
                                      child: AutoSizeText(
                                        "My Favorites",
                                        minFontSize: 14,
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                        style: TextStyle(fontSize: 20),
                                      ),
                                    ),
                                    IconButton(
                                      onPressed: () {
                                        hideKeyboard(this.context);
                                        Navigator.push(
                                                context,
                                                MaterialPageRoute(
                                                    builder: (context) =>
                                                        FavoritesPage(this
                                                            .displayedFavorites)))
                                            .then((_) {
                                          saveFavorites();
                                        });
                                      },
                                      icon: Icon(Icons.contacts),
                                      key:
                                          Key("user_settings:favorites_button"),
                                    ),
                                  ],
                                ),
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
                                      key: Key(
                                          "user_settings:dark_theme_switch"),
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
            ),
          ])),
    );
  }

  // uses the OS of the device to pick an image, we compress it before sending it
  Future getImage() async {
    File newIconFile = await ImagePicker.pickImage(
        source: ImageSource.gallery, imageQuality: 75);

    if (newIconFile != null) {
      // user successfully picked an image, so now allow them to crop it
      File croppedImage = await ImageCropper.cropImage(
          sourcePath: newIconFile.path,
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
        this._icon = croppedImage;
        this.newIcon = true;
        showSaveButton();
      }
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
          Globals.user.appSettings.defaultConsiderDuration,
          Globals.user.appSettings.defaultVotingDuration,
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
    if (Globals.user.appSettings.defaultConsiderDuration !=
            this.considerDuration ||
        Globals.user.appSettings.defaultVotingDuration != this.votingDuration ||
        Globals.user.appSettings.darkTheme != this._darkTheme ||
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
    hideKeyboard(this.context);
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
          this.considerDuration,
          this.votingDuration,
          userNames,
          this._icon);
      Navigator.of(this.context, rootNavigator: true).pop('dialog');

      if (resultStatus.success) {
        setState(() {
          // reset everything and reflect changes made
          this.originalFavorites.clear();
          this.originalFavorites.addAll(this.displayedFavorites);
          this.editing = false;
          this.newIcon = false;
          this.autoValidate = false;
          changeTheme(this.context);
        });
      } else {
        showErrorMessage("Error", resultStatus.errorMessage, this.context);
      }
    } else {
      setState(() => this.autoValidate = true);
    }
  }
}
