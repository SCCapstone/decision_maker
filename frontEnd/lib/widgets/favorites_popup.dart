import 'package:flutter/material.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/favorite.dart';
import 'package:frontEnd/models/user.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/widgets/user_row.dart';

class FavoritesPopup extends StatefulWidget {
  final List<Favorite> displayedFavorites;
  final List<Favorite> originalFavorites;
  final Function handlePopupClosed;

  FavoritesPopup(this.displayedFavorites, this.originalFavorites,
      {this.handlePopupClosed});

  @override
  _FavoritesPopupState createState() => _FavoritesPopupState();
}

class _FavoritesPopupState extends State<FavoritesPopup> {
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  final TextEditingController userController = new TextEditingController();
  List<UserRow> displayedUserRows = new List<UserRow>();
  List<UserRow> searchResults = new List<UserRow>();
  bool searching = false;

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    // displays a popup for editing the group's members
    return Form(
      key: formKey,
      child: AlertDialog(
        title: Text("My Favorites"),
        actions: <Widget>[
          FlatButton(
            child: Text("Back"),
            onPressed: () {
              userController.clear();
              Navigator.of(context, rootNavigator: true).pop('dialog');
              widget.handlePopupClosed();
            },
          ),
          FlatButton(
            child: Text("Add User"),
            onPressed: () {
              if (formKey.currentState.validate()) {
                addNewFavorite();
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
                    controller: userController,
                    validator: (value) {
                      return validNewFavorite(
                          value.trim(), widget.displayedFavorites);
                    },
                    decoration: InputDecoration(
                      labelText: "Enter a username to add",
                    )),
                Container(
                  height: MediaQuery.of(context).size.height * .25,
                  child: ListView.builder(
                      shrinkWrap: true,
                      itemCount: widget.displayedFavorites.length,
                      itemBuilder: (context, index) {
                        return UserRow(
                            widget.displayedFavorites[index].displayName,
                            widget.displayedFavorites[index].username,
                            widget.displayedFavorites[index].icon,
                            true,
                            false,
                            false, deleteUser: () {
                          widget.displayedFavorites
                              .remove(widget.displayedFavorites[index]);
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
  }

  void addNewFavorite() async {
    ResultStatus<User> resultStatus =
        await UsersManager.getUserData(username: userController.text.trim());
    if (resultStatus.success) {
      User newFavorite = resultStatus.data;
      widget.displayedFavorites.add(new Favorite(
          username: newFavorite.username,
          displayName: newFavorite.displayName,
          icon: newFavorite.icon));
    } else {
      showErrorMessage("Error", "Cannot add: ${userController.text}", context);
      hideKeyboard(context);
    }
    setState(() {
      userController.clear();
    });
  }
}
