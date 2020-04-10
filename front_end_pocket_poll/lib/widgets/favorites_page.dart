import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/favorite.dart';
import 'package:front_end_pocket_poll/models/user.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'package:front_end_pocket_poll/utilities/validator.dart';
import 'package:front_end_pocket_poll/widgets/user_row.dart';

class FavoritesPage extends StatefulWidget {
  final List<Favorite> displayedFavorites;

  FavoritesPage(this.displayedFavorites);

  @override
  _FavoritesPageState createState() => _FavoritesPageState();
}

class _FavoritesPageState extends State<FavoritesPage> {
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  final TextEditingController userController = new TextEditingController();

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        // allows for anywhere on the screen to be tapped to hide keyboard
        hideKeyboard(context);
      },
      child: Form(
        key: this.formKey,
        child: Scaffold(
            appBar: AppBar(
              title: Text("My Favorites"),
            ),
            body: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Column(
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      Expanded(
                        child: TextFormField(
                            maxLength: Globals.maxUsernameLength,
                            controller: this.userController,
                            validator: (value) {
                              return validNewFavorite(
                                  value.trim(), widget.displayedFavorites);
                            },
                            decoration: InputDecoration(
                              labelText: "Enter a username to add",
                              counterText: "",
                            )),
                      ),
                      FlatButton(
                        child: Text("Add User"),
                        onPressed: () {
                          if (this.formKey.currentState.validate()) {
                            addNewFavorite();
                          }
                        },
                      ),
                    ],
                  ),
                  Expanded(
                    child: Scrollbar(
                      child: ListView.builder(
                          shrinkWrap: false,
                          itemCount: widget.displayedFavorites.length,
                          itemBuilder: (context, index) {
                            // sorting by alphabetical by displayname for now
                            widget.displayedFavorites.sort((a, b) => a
                                .displayName
                                .toLowerCase()
                                .compareTo(b.displayName.toLowerCase()));
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
                  ),
                ],
              ),
            )),
      ),
    );
  }

  // attempts to add a new favorite if that user exists in the DB
  void addNewFavorite() async {
    ResultStatus<User> resultStatus = await UsersManager.getUserData(
        username: this.userController.text.trim());
    if (resultStatus.success) {
      User newFavorite = resultStatus.data;
      widget.displayedFavorites.add(new Favorite(
          username: newFavorite.username,
          displayName: newFavorite.displayName,
          icon: newFavorite.icon));
    } else {
      showErrorMessage(
          "Error", "Cannot add: ${this.userController.text}", this.context);
      hideKeyboard(this.context);
    }
    setState(() {
      this.userController.clear();
    });
  }
}
