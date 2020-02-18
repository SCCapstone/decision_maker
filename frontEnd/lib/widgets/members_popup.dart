import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/favorite.dart';
import 'package:frontEnd/models/member.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/widgets/user_row.dart';

class MembersPopup extends StatefulWidget {
  final List<Member> displayedMembers;
  final List<Member> originalMembers;
  final Function handlePopupClosed;
  final bool
      isCreating; // if creating you don't have to worry about group creator

  MembersPopup(this.displayedMembers, this.originalMembers, this.isCreating,
      {this.handlePopupClosed});

  @override
  _MembersPopupState createState() => _MembersPopupState();
}

class _MembersPopupState extends State<MembersPopup> {
  final GlobalKey<FormState> formKey = new GlobalKey<FormState>();
  final TextEditingController userController = new TextEditingController();
  List<UserRow> displayedUserRows = new List<UserRow>();
  List<UserRow> searchResults = new List<UserRow>();
  bool searching = false;

  @override
  void initState() {
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
        removeMember(user.username, displayedUserRows);
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
                widget.displayedMembers.add(new Member(
                    username: username, displayName: username, icon: username));
                setState(() {
                  displayedUserRows.add(new UserRow(
                      username, username, username, true, false, false,
                      deleteUser: () {
                    removeMember(username, displayedUserRows);
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
                    onChanged: (value) {
                      // anytime the user is searching, immediately display results
                      if (value.isNotEmpty) {
                        searching = true;
                        List<UserRow> currentSearch = new List<UserRow>();
                        for (Favorite favorite in Globals.user.favorites) {
                          // show suggestion if match to username or displayname of a favorite
                          if ((favorite.username
                                      .toLowerCase()
                                      .contains(value.toLowerCase()) &&
                                  !widget.displayedMembers.contains(
                                      new Member.fromFavorite(favorite))) ||
                              (favorite.displayName
                                      .toLowerCase()
                                      .contains(value.toLowerCase()) &&
                                  !widget.displayedMembers.contains(
                                      new Member.fromFavorite(favorite)))) {
                            // when searching, only show suggestions if the user hasn't already added the user to the group
                            currentSearch.add(new UserRow(
                                favorite.displayName,
                                favorite.username,
                                favorite.icon,
                                false,
                                true,
                                false,
                                addUser: () =>
                                    addMemberFromFavorites(favorite)));
                          }
                        }
                        setState(() {
                          searchResults = currentSearch;
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
                            : displayedUserRows.length,
                        itemBuilder: (context, index) {
                          // sorting by alphabetical by displayname for now
                          searchResults.sort((a, b) => b.displayName
                              .toLowerCase()
                              .compareTo(a.displayName.toLowerCase()));
                          displayedUserRows.sort((a, b) => a.displayName
                              .toLowerCase()
                              .compareTo(b.displayName.toLowerCase()));
                          if (searching) {
                            return searchResults[index];
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
      removeMember(memberToAdd.username, displayedUserRows);
    }));
    // update the alertdialog to show the new user added
    setState(() {
      searching = false;
      searchResults.clear();
      userController.clear();
    });
  }

  void removeMember(String username, List<UserRow> displayedUserRows) {
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
    displayedUserRows.remove(rowToRemove);
    setState(() {});
  }
}
