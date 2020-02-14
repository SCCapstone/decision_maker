import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/favorite.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/widgets/user_row.dart';

class AddUserPopup extends StatefulWidget {
  final List<Favorite> displayedUsers;
  final List<Favorite> originalUsers;
  final Function handlePopupClosed;

  AddUserPopup(this.displayedUsers, this.originalUsers,
      {this.handlePopupClosed});

  @override
  _AddUserPopupState createState() => _AddUserPopupState();
}

class _AddUserPopupState extends State<AddUserPopup> {
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  final TextEditingController userController = new TextEditingController();
  List<UserRow> displayedUsers = new List<UserRow>();
  List<UserRow> searchResults = new List<UserRow>();
  bool searching = false;

  @override
  void initState() {
    for (Favorite user in widget.displayedUsers) {
      bool displayDelete = user.username != Globals.currentGroup.groupCreator &&
          user.username != Globals.username;
      displayedUsers.add(new UserRow(
          user.displayName,
          user.username,
          user.icon,
          displayDelete,
          false,
          (user.username == Globals.currentGroup.groupCreator), deleteUser: () {
        removeUser(user.username, displayedUsers);
        setState(() {});
      }));
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    // displays a popup for editing the group's members
    return Form(
      key: formKey,
      child: AlertDialog(
        title: Text("Group Memebers"),
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
                // TODO launch api request to add user. Then parse info and create appropriate Favorite model
                String username = userController.text.trim();
                widget.displayedUsers.add(new Favorite(
                    username: username, displayName: username, icon: username));
                setState(() {
                  displayedUsers.add(new UserRow(
                      username,
                      username,
                      username,
                      true,
                      false,
                      username == Globals.currentGroup.groupCreator,
                      deleteUser: () {
                    removeUser(username, displayedUsers);
                    setState(() {});
                  }));
                  userController.clear();
                  searching = false;
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
                    controller: userController,
                    validator: (value) {
                      List<String> allUsers = new List<String>();
                      for (Favorite user in widget.displayedUsers) {
                        allUsers.add(user.username);
                      }
                      for (Favorite user in widget.originalUsers) {
                        allUsers.add(user.username);
                      }
                      return validUser(value.trim(), allUsers);
                    },
                    onChanged: (value) {
                      if (value.isNotEmpty) {
                        searching = true;
                        List<UserRow> temp = new List<UserRow>();
                        for (Favorite favorite in Globals.user.favorites) {
                          // show suggestion if match to username or displayname
                          if ((favorite.username
                                      .toLowerCase()
                                      .contains(value.toLowerCase()) &&
                                  !widget.displayedUsers.contains(favorite)) ||
                              (favorite.displayName
                                      .toLowerCase()
                                      .contains(value.toLowerCase()) &&
                                  !widget.displayedUsers.contains(favorite))) {
                            // when searching, only show suggestions if the user hasn't already added the user to the group
                            temp.add(new UserRow(
                                favorite.displayName,
                                favorite.username,
                                favorite.icon,
                                false,
                                true,
                                (favorite.username ==
                                    Globals.currentGroup.groupCreator),
                                addUser: () => addUser(favorite)));
                          }
                        }
                        setState(() {
                          searchResults = temp;
                        });
                      } else {
                        setState(() {
                          searching = false;
                        });
                      }
                    },
                    decoration: InputDecoration(
                      labelText: "Enter a username to add",
                    )),
                Scrollbar(
                  child: Container(
                    height: MediaQuery.of(context).size.height * .25,
                    child: ListView.builder(
                        shrinkWrap: true,
                        itemCount: (searching)
                            ? searchResults.length
                            : displayedUsers.length,
                        itemBuilder: (context, index) {
                          // sorting by alphabetical by displayname for now
                          searchResults.sort((a, b) => b.displayName
                              .toLowerCase()
                              .compareTo(a.displayName.toLowerCase()));
                          displayedUsers.sort((a, b) => a.displayName
                              .toLowerCase()
                              .compareTo(b.displayName.toLowerCase()));
                          if (searching) {
                            return searchResults[index];
                          } else {
                            return displayedUsers[index];
                          }
                        }),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  void addUser(Favorite userToAdd) {
    // add the new user to the list and then display it in the scroll view
    widget.displayedUsers.add(userToAdd);
    for (Favorite user in Globals.user.favorites) {
      if (userToAdd.username == user.username) {
        displayedUsers.add(new UserRow(
            user.displayName,
            user.username,
            user.icon,
            true,
            false,
            (user.username == Globals.currentGroup.groupCreator),
            deleteUser: () {
          removeUser(user.username, displayedUsers);
          setState(() {});
        }));
      }
    }
    // update the alertdialog to show the new user added
    setState(() {
      searching = false;
      searchResults.clear();
      userController.clear();
    });
  }

  void removeUser(String username, List<UserRow> displayedUserRows) {
    Favorite userToRemove;
    for (Favorite user in widget.displayedUsers) {
      if (user.username == username) {
        userToRemove = user;
      }
    }
    widget.displayedUsers.remove(userToRemove);

    UserRow rowToRemove;
    for (UserRow row in displayedUserRows) {
      if (row.username == username) {
        rowToRemove = row;
      }
    }
    displayedUserRows.remove(rowToRemove);
  }
}
