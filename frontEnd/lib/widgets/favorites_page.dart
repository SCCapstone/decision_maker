import 'package:flutter/material.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/favorite.dart';
import 'package:frontEnd/models/user.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/widgets/user_row.dart';

class FavoritesPage extends StatefulWidget {
  final List<Favorite> displayedFavorites;

  FavoritesPage(this.displayedFavorites);

  @override
  _FavoritesPageState createState() => _FavoritesPageState();
}

class _FavoritesPageState extends State<FavoritesPage> {
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
    return Form(
      key: formKey,
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
                          controller: userController,
                          validator: (value) {
                            return validNewFavorite(
                                value.trim(), widget.displayedFavorites);
                          },
                          decoration: InputDecoration(
                            labelText: "Enter a username to add",
                          )),
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
                ),
                Expanded(
                  child: Scrollbar(
                    child: ListView.builder(
                        shrinkWrap: false,
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
                ),
              ],
            ),
          )),
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
