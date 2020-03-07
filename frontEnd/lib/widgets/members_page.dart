import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/favorite.dart';
import 'package:frontEnd/models/member.dart';
import 'package:frontEnd/models/user.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/widgets/user_row.dart';

class MembersPage extends StatefulWidget {
  final List<Member> displayedMembers;
  final List<Member> originalMembers;
  final bool isCreating; // if creating don't have to bother with group creator

  MembersPage(this.displayedMembers, this.originalMembers, this.isCreating);

  @override
  _MembersPageState createState() => _MembersPageState();
}

class _MembersPageState extends State<MembersPage> {
  final GlobalKey<FormState> formKey = new GlobalKey<FormState>();
  final TextEditingController userController = new TextEditingController();
  final TextEditingController favoriteController = new TextEditingController();
  List<UserRow> displayedUserRows = new List<UserRow>();
  List<UserRow> displayedFavoritesRows = new List<UserRow>();
  bool showFavorites = false;
  UserRow creator;

  @override
  void initState() {
    getDisplayedFavorites();
    for (Member user in widget.displayedMembers) {
      // if you're creating a group, you can always remove members from the group
      bool displayDelete = true;
      bool displayOwner = false;
      if (!widget.isCreating) {
        // can't delete yourself or the group creator
        displayDelete = user.username != Globals.currentGroup.groupCreator &&
            user.username != Globals.username;
        displayOwner = user.username == Globals.currentGroup.groupCreator;
      }
      UserRow userRow = new UserRow(user.displayName, user.username, user.icon,
          displayDelete, false, displayOwner, deleteUser: () {
        removeMember(user.username);
      });
      if (displayOwner) {
        creator = userRow;
      }
      displayedUserRows.add(userRow);
      // if favorite is already in group, don't show in the favorites section
      displayedFavoritesRows
          .removeWhere((row) => row.username == user.username);
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: formKey,
      child: WillPopScope(
          onWillPop: handleBackPress,
          child: Scaffold(
            appBar: AppBar(
              title: Text("Add/Remove Members"),
              leading: IconButton(
                  icon: Icon(Icons.arrow_back),
                  onPressed: () {
                    Navigator.pop(context, true);
                  }),
            ),
            body: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      Visibility(
                        visible: !showFavorites,
                        child: Expanded(
                          child: TextFormField(
                              maxLength: 100,
                              controller: userController,
                              validator: (value) {
                                List<String> allUsers = new List<String>();
                                for (Member user in widget.displayedMembers) {
                                  allUsers.add(user.username);
                                }
                                for (Member user in widget.originalMembers) {
                                  if (!allUsers.contains(user.username)) {
                                    allUsers.add(user.username);
                                  }
                                }
                                return validUser(value.trim(), allUsers);
                              },
                              decoration: InputDecoration(
                                  labelText: "Enter a username to add",
                                  counterText: "")),
                        ),
                      ),
                      Visibility(
                        visible: !showFavorites,
                        child: FlatButton(
                          child: Text("Add User"),
                          onPressed: () {
                            if (formKey.currentState.validate()) {
                              addNewMember();
                            }
                          },
                        ),
                      ),
                      Visibility(
                        visible: showFavorites,
                        child: Expanded(
                          child: TextFormField(
                            maxLength: 100,
                            controller: favoriteController,
                            decoration: InputDecoration(
                                labelText: "Add user from favorites",
                                counterText: ""),
                            onChanged: (value) {
                              if (value.isEmpty) {
                                setState(() {
                                  displayedFavoritesRows.clear();
                                  getDisplayedFavorites();
                                });
                              } else {
                                List<UserRow> searchRows = new List<UserRow>();
                                for (Favorite fav in Globals.user.favorites) {
                                  // show suggestion if match to username or displayname of a favorite
                                  if ((fav.username
                                              .toLowerCase()
                                              .contains(value.toLowerCase()) &&
                                          !widget.displayedMembers.contains(
                                              new Member.fromFavorite(fav))) ||
                                      (fav.displayName
                                              .toLowerCase()
                                              .contains(value.toLowerCase()) &&
                                          !widget.displayedMembers.contains(
                                              new Member.fromFavorite(fav)))) {
                                    // when searching, only show suggestions if the user hasn't already added the user to the group
                                    searchRows.add(new UserRow(
                                        fav.displayName,
                                        fav.username,
                                        fav.icon,
                                        false,
                                        true,
                                        false,
                                        addUser: () =>
                                            addMemberFromFavorites(fav)));
                                  }
                                }
                                setState(() {
                                  displayedFavoritesRows = searchRows;
                                });
                              }
                            },
                          ),
                        ),
                      ),
                      Container(
                        decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: (showFavorites)
                                ? Theme.of(context).accentColor
                                : Theme.of(context).scaffoldBackgroundColor),
                        child: IconButton(
                          icon: Icon(Icons.contacts),
                          onPressed: () {
                            setState(() {
                              favoriteController.clear();
                              userController.clear();

                              // prevents bug of if user types name that doesn't exist and then hits back, list will be empty
                              displayedFavoritesRows.clear();
                              getDisplayedFavorites();
                              showFavorites = !showFavorites;
                            });
                          },
                        ),
                      )
                    ],
                  ),
                  Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .005)),
                  Expanded(
                    child: Scrollbar(
                      child: ListView.builder(
                          shrinkWrap: true,
                          itemCount: (showFavorites)
                              ? displayedFavoritesRows.length
                              : displayedUserRows.length,
                          itemBuilder: (context, index) {
                            // sorting by alphabetical by displayname for now
                            displayedUserRows.sort((a, b) => a.displayName
                                .toLowerCase()
                                .compareTo(b.displayName.toLowerCase()));
                            if (!widget.isCreating) {
                              // place the owner at the top
                              displayedUserRows.remove(creator);
                              displayedUserRows.insert(0, creator);
                            }
                            displayedFavoritesRows.sort((a, b) => a.displayName
                                .toLowerCase()
                                .compareTo(b.displayName.toLowerCase()));
                            if (showFavorites) {
                              return displayedFavoritesRows[index];
                            } else {
                              return displayedUserRows[index];
                            }
                          }),
                    ),
                  ),
                ],
              ),
            ),
          )),
    );
  }

  Future<bool> handleBackPress() async {
    // allows for user to exit the favorite section with the back button
    if (showFavorites) {
      setState(() {
        showFavorites = false;
      });
      return false;
    } else {
      return true;
    }
  }

  void getDisplayedFavorites() {
    for (Favorite favorite in Globals.user.favorites) {
      // can't ever add the owner of a group, so it's always false below
      displayedFavoritesRows.add(new UserRow(
        favorite.displayName,
        favorite.username,
        favorite.icon,
        false,
        true,
        false,
        addUser: () {
          addMemberFromFavorites(favorite);
        },
      ));
    }
    for (UserRow user in displayedUserRows) {
      // if favorite is already in group, don't show in the favorites section
      displayedFavoritesRows
          .removeWhere((row) => row.username == user.username);
    }
  }

  void addNewMember() async {
    ResultStatus<User> resultStatus =
        await UsersManager.getUserData(username: userController.text.trim());
    if (resultStatus.success) {
      User newMember = resultStatus.data;
      widget.displayedMembers.add(new Member(
          username: newMember.username,
          displayName: newMember.displayName,
          icon: newMember.icon));
      displayedUserRows.add(new UserRow(
          newMember.displayName,
          newMember.username,
          newMember.icon,
          true,
          false,
          false, deleteUser: () {
        removeMember(newMember.username);
      }));
    } else {
      showErrorMessage("Error", "Cannot add: ${userController.text}.", context);
      hideKeyboard(context);
    }
    setState(() {
      userController.clear();
    });
  }

  void addMemberFromFavorites(Favorite memberToAdd) {
    // add the new user to the list and then display it in the scroll view
    widget.displayedMembers.add(new Member.fromFavorite(memberToAdd));
    displayedUserRows.add(new UserRow(
        memberToAdd.displayName,
        memberToAdd.username,
        memberToAdd.icon,
        true,
        false,
        false, deleteUser: () {
      removeMember(memberToAdd.username);
    }));
    displayedFavoritesRows
        .removeWhere((row) => row.username == memberToAdd.username);
    setState(() {});
  }

  void removeMember(String username) {
    Member userToRemove;
    for (Member user in widget.displayedMembers) {
      if (user.username == username) {
        userToRemove = user;
      }
    }
    widget.displayedMembers.remove(userToRemove);

    UserRow rowToRemove;
    for (UserRow row in displayedUserRows) {
      if (row.username == username) {
        rowToRemove = row;
      }
    }
    // if removed user is a favorite of the user, add it to the favorites rows
    for (Favorite favorite in Globals.user.favorites) {
      if (favorite.username == username) {
        displayedFavoritesRows.add(new UserRow(
          favorite.displayName,
          favorite.username,
          favorite.icon,
          false,
          true,
          false,
          addUser: () {
            addMemberFromFavorites(favorite);
          },
        ));
      }
    }
    displayedUserRows.remove(rowToRemove);
    setState(() {});
  }
}