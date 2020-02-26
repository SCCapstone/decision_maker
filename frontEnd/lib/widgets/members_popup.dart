import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/favorite.dart';
import 'package:frontEnd/models/member.dart';
import 'package:frontEnd/models/user.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/widgets/user_row.dart';

class MembersPopup extends StatefulWidget {
  final List<Member> displayedMembers;
  final List<Member> originalMembers;
  final Function handlePopupClosed;
  final bool isCreating; // if creating don't have to bother with group creator

  MembersPopup(this.displayedMembers, this.originalMembers, this.isCreating,
      {this.handlePopupClosed});

  @override
  _MembersPopupState createState() => _MembersPopupState();
}

class _MembersPopupState extends State<MembersPopup> {
  final GlobalKey<FormState> formKey = new GlobalKey<FormState>();
  final TextEditingController userController = new TextEditingController();
  List<UserRow> displayedUserRows = new List<UserRow>();
  List<UserRow> displayedFavoritesRows = new List<UserRow>();
  bool showFavorites = false;

  @override
  void initState() {
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
      displayedUserRows.add(new UserRow(user.displayName, user.username,
          user.icon, displayDelete, false, displayOwner, deleteUser: () {
        removeMember(user.username);
      }));
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
      child: AlertDialog(
        title: Text("Group Members"),
        actions: <Widget>[
          FlatButton(
            child: Text("Back"),
            onPressed: () {
              userController.clear();
              Navigator.of(context, rootNavigator: true).pop('dialog');
              widget.handlePopupClosed();
            },
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
        ],
        content: SingleChildScrollView(
          scrollDirection: Axis.vertical,
          child: Container(
            width: double.maxFinite,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                Row(
                  children: <Widget>[
                    Visibility(
                      visible: !showFavorites,
                      child: Expanded(
                        child: Container(
                          height: MediaQuery.of(context).size.height * .08,
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
                    ),
                    Visibility(
                      visible: showFavorites,
                      child: Expanded(
                        child: Container(
                            height: MediaQuery.of(context).size.height * .08,
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: <Widget>[
                                Text(
                                  "Add from Favorites List",
                                )
                              ],
                            )),
                      ),
                    ),
                    Container(
                      decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: (showFavorites)
                              ? Theme.of(context).accentColor
                              : Theme.of(context).dialogBackgroundColor),
                      child: IconButton(
                        icon: Icon(Icons.contacts),
                        onPressed: () {
                          setState(() {
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
                Scrollbar(
                  child: Container(
                    height: MediaQuery.of(context).size.height * .25,
                    child: ListView.builder(
                        shrinkWrap: true,
                        itemCount: (showFavorites)
                            ? displayedFavoritesRows.length
                            : displayedUserRows.length,
                        itemBuilder: (context, index) {
                          // sorting by alphabetical by displayname for now
                          displayedUserRows.sort((a, b) => b.displayName
                              .toLowerCase()
                              .compareTo(a.displayName.toLowerCase()));
                          displayedUserRows.sort((a, b) => a.displayName
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
        ),
      ),
    );
  }

  void addNewMember() async {
    User newMember = await UsersManager.getUserData(context, true,
        username: userController.text.trim());
    if (newMember != null) {
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
      showErrorMessage(
          "Error", "User: ${userController.text} does not exist.", context);
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
