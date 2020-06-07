import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/groups_widgets/groups_home.dart';
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

class FirstLogin extends StatefulWidget {
  FirstLogin({Key key}) : super(key: key);

  @override
  _FirstLoginState createState() => _FirstLoginState();
}

class _FirstLoginState extends State<FirstLogin> {
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  final TextEditingController displayNameController = TextEditingController();

  bool autoValidate;
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
    this.newIcon = false;
    this.displayedFavorites = new List<Favorite>();
    this.originalFavorites = new List<Favorite>();

    this._displayName = Globals.user.displayName;
    this._darkTheme = Globals.user.appSettings.darkTheme;
    this._muted = Globals.user.appSettings.muted;
    this.originalFavorites = Globals.user.favorites;
    this.displayedFavorites.addAll(this.originalFavorites);
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
          title: Text("Account Setup"),
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
                          width: MediaQuery.of(context).size.width * .90,
                          child: TextFormField(
                            maxLength: Globals.maxDisplayNameLength,
                            controller: this.displayNameController,
                            validator: validDisplayName,
                            onChanged: (String arg) {
                              this._displayName = arg.trim();
                            },
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
                      Container(
                        width: MediaQuery.of(context).size.width * .8,
                        child: Column(
                          children: <Widget>[
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
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
                                    });
                                  },
                                )
                              ],
                            ),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
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
                                  onChanged: (bool value) {
                                    setState(() {
                                      this._darkTheme = !value;
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
        ]),
        bottomNavigationBar: BottomAppBar(
          color: Theme.of(context).scaffoldBackgroundColor,
          child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                RaisedButton.icon(
                    onPressed: saveSettings,
                    icon: Icon(Icons.save),
                    label: Text("Save")),
              ]),
        ),
      ),
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
      }
      setState(() {});
    }
  }

  // attempts to save the user settings if the input is valid
  void saveSettings() async {
    hideKeyboard(context);
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
          10,
          10,
          userNames,
          this._icon);
      Navigator.of(this.context, rootNavigator: true).pop('dialog');

      if (resultStatus.success) {
        changeTheme(this.context);
        Navigator.pushReplacement(
          this.context,
          MaterialPageRoute(builder: (context) => GroupsHome()),
        );
      } else {
        showErrorMessage("Error", resultStatus.errorMessage, this.context);
      }
    } else {
      setState(() => this.autoValidate = true);
    }
  }
}
