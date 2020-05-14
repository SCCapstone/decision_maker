import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/favorite.dart';
import 'package:front_end_pocket_poll/models/member.dart';
import 'package:front_end_pocket_poll/models/user.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'package:front_end_pocket_poll/utilities/validator.dart';
import 'package:front_end_pocket_poll/widgets/user_row.dart';

class MembersPage extends StatefulWidget {
  final List<Member> displayedMembers;
  final List<String> membersLeft;
  final bool isCreating; // if creating don't have to bother with group creator
  final bool canEdit;

  MembersPage(
      this.displayedMembers, this.membersLeft, this.isCreating, this.canEdit);

  @override
  _MembersPageState createState() => _MembersPageState();
}

class _MembersPageState extends State<MembersPage> {
  final GlobalKey<FormState> formKey = new GlobalKey<FormState>();
  final TextEditingController userController = new TextEditingController();
  final TextEditingController favoriteController = new TextEditingController();

  List<UserRow> displayedUserRows;
  List<UserRow> displayedFavoritesRows;
  bool showFavorites;
  UserRow creatorRow; // separate variable for putting it at the top of the list

  @override
  void initState() {
    this.displayedUserRows = new List<UserRow>();
    this.displayedFavoritesRows = new List<UserRow>();
    this.showFavorites = false;

    getDisplayedFavorites();
    for (Member user in widget.displayedMembers) {
      // if you're creating a group, you can always remove members from the group
      bool displayDelete = true;
      bool displayOwner = false;
      if (!widget.isCreating) {
        // can't delete yourself or the group creator
        displayDelete = user.username != Globals.currentGroup.groupCreator &&
            user.username != Globals.username &&
            widget.canEdit;
        displayOwner = user.username == Globals.currentGroup.groupCreator;
      }
      UserRow userRow = new UserRow(user.displayName, user.username, user.icon,
          displayDelete, false, displayOwner, deleteUser: () {
        removeMember(user.username);
      });
      if (displayOwner) {
        this.creatorRow = userRow;
      }
      this.displayedUserRows.add(userRow);
      // if favorite is already in group, don't show in the favorites section
      this
          .displayedFavoritesRows
          .removeWhere((row) => row.username == user.username);
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: this.formKey,
      child: GestureDetector(
        onTap: () {
          // allows for anywhere on the page to be clicked to hide the keyboard
          hideKeyboard(context);
        },
        child: WillPopScope(
            onWillPop: handleBackPress,
            child: Scaffold(
              appBar: AppBar(
                title: (widget.canEdit)
                    ? Text("Add/Remove Members")
                    : Text("View Members"),
              ),
              key: Key("members_page:scaffold"),
              body: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    Visibility(
                      visible: widget.canEdit,
                      child: Row(
                        children: <Widget>[
                          Visibility(
                            visible: !this.showFavorites,
                            child: Expanded(
                              child: TextFormField(
                                  maxLength: Globals.maxUsernameLength,
                                  controller: this.userController,
                                  validator: (value) {
                                    List<String> allUsers = new List<String>();
                                    for (Member user
                                        in widget.displayedMembers) {
                                      allUsers.add(user.username);
                                    }
                                    return validNewUser(value.trim(), allUsers);
                                  },
                                  key: Key("members_page:member_input"),
                                  decoration: InputDecoration(
                                      labelText: "Enter username to add",
                                      counterText: "")),
                            ),
                          ),
                          Visibility(
                            visible: !this.showFavorites,
                            child: FlatButton(
                              child: Text("Add User"),
                              key: Key("members_page:add_member_button"),
                              onPressed: () {
                                if (this.formKey.currentState.validate()) {
                                  addNewMember();
                                }
                              },
                            ),
                          ),
                          Visibility(
                            visible: this.showFavorites,
                            child: Expanded(
                              child: TextFormField(
                                maxLength: Globals.maxUsernameLength,
                                controller: this.favoriteController,
                                decoration: InputDecoration(
                                    labelText: "Add user from favorites",
                                    counterText: ""),
                                onChanged: (value) {
                                  // allows users to type to get suggestions
                                  if (value.isEmpty) {
                                    setState(() {
                                      this.displayedFavoritesRows.clear();
                                      getDisplayedFavorites();
                                    });
                                  } else {
                                    List<UserRow> searchRows =
                                        new List<UserRow>();
                                    for (Favorite fav
                                        in Globals.user.favorites) {
                                      // show suggestion if match to username or displayname of a favorite
                                      if ((fav.username.toLowerCase().contains(
                                                  value.toLowerCase()) &&
                                              !widget.displayedMembers.contains(
                                                  new Member.fromFavorite(
                                                      fav))) ||
                                          (fav.displayName
                                                  .toLowerCase()
                                                  .contains(
                                                      value.toLowerCase()) &&
                                              !widget.displayedMembers.contains(
                                                  new Member.fromFavorite(
                                                      fav)))) {
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
                                      this.displayedFavoritesRows = searchRows;
                                    });
                                  }
                                },
                              ),
                            ),
                          ),
                          Container(
                            decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                color: (this.showFavorites)
                                    ? Theme.of(context).accentColor
                                    : Theme.of(context)
                                        .scaffoldBackgroundColor),
                            child: IconButton(
                              icon: Icon(Icons.contacts),
                              tooltip: (this.showFavorites)
                                  ? "Hide Favorites"
                                  : "Show favorites",
                              key: Key("members_page:show_favorites_button"),
                              onPressed: () {
                                setState(() {
                                  this.favoriteController.clear();
                                  this.userController.clear();

                                  // prevents bug if user types name that doesn't exist and then hits back, list will be empty
                                  this.displayedFavoritesRows.clear();
                                  getDisplayedFavorites();
                                  this.showFavorites = !this.showFavorites;
                                });
                              },
                            ),
                          )
                        ],
                      ),
                    ),
                    Padding(
                        padding: EdgeInsets.all(
                            MediaQuery.of(context).size.height * .005)),
                    Visibility(
                        visible: (this.showFavorites &&
                            this.displayedFavoritesRows.isEmpty),
                        child: Text((Globals.user.favorites.isEmpty)
                            ? "No favorites found! Go to your user settings page to add some."
                            : "No more favorites left to add.")),
                    Expanded(
                      child: Scrollbar(
                        child: ListView.builder(
                            shrinkWrap: true,
                            itemCount: (this.showFavorites)
                                ? this.displayedFavoritesRows.length
                                : this.displayedUserRows.length,
                            itemBuilder: (context, index) {
                              // sorting by alphabetical by displayname for now
                              this.displayedUserRows.sort((a, b) => a
                                  .displayName
                                  .toLowerCase()
                                  .compareTo(b.displayName.toLowerCase()));
                              if (!widget.isCreating) {
                                // place the owner at the top
                                this.displayedUserRows.remove(creatorRow);
                                this.displayedUserRows.insert(0, creatorRow);
                              }
                              this.displayedFavoritesRows.sort((a, b) => a
                                  .displayName
                                  .toLowerCase()
                                  .compareTo(b.displayName.toLowerCase()));
                              if (this.showFavorites) {
                                return this.displayedFavoritesRows[index];
                              } else {
                                return this.displayedUserRows[index];
                              }
                            }),
                      ),
                    ),
                  ],
                ),
              ),
            )),
      ),
    );
  }

  // allows for user to exit the favorite section with the back button
  Future<bool> handleBackPress() async {
    if (this.showFavorites) {
      setState(() {
        this.showFavorites = false;
      });
      return false;
    } else {
      return true;
    }
  }

  // populate the total favorites using the favorites on the local user object
  void getDisplayedFavorites() {
    for (Favorite favorite in Globals.user.favorites) {
      // can't ever add the owner of a group, so it's always false below
      this.displayedFavoritesRows.add(new UserRow(
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
      this
          .displayedFavoritesRows
          .removeWhere((row) => row.username == user.username);
    }
    for (String username in widget.membersLeft) {
      // if favorite has previously left the group, don't show in the favorites section
      this
          .displayedFavoritesRows
          .removeWhere((row) => row.username == username);
    }
  }

  // attempt to add a new member to the group if said user is in the DB
  void addNewMember() async {
    String username = this.userController.text.trim();
    if (widget.membersLeft.contains(username)) {
      showErrorMessage(
          "Error",
          "Cannot add $username because they have previously left this group.",
          this.context);
      hideKeyboard(this.context);
    } else {
      ResultStatus<User> resultStatus =
          await UsersManager.getUserData(username: username);
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
        showErrorMessage("Error", resultStatus.errorMessage, this.context);
        hideKeyboard(this.context);
      }
      setState(() {
        this.userController.clear();
      });
    }
  }

  // add the new user to the list and then display it in the scroll view
  void addMemberFromFavorites(Favorite memberToAdd) {
    widget.displayedMembers.add(new Member.fromFavorite(memberToAdd));
    this.displayedUserRows.add(new UserRow(
            memberToAdd.displayName,
            memberToAdd.username,
            memberToAdd.icon,
            true,
            false,
            false, deleteUser: () {
          removeMember(memberToAdd.username);
        }));
    this
        .displayedFavoritesRows
        .removeWhere((row) => row.username == memberToAdd.username);
    setState(() {});
  }

  // removes a member from the list
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
        this.displayedFavoritesRows.add(new UserRow(
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
    this.displayedUserRows.remove(rowToRemove);
    setState(() {});
  }
}
