import 'dart:collection';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/categories_widgets/choice_row.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/models/categoryRatingTuple.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';

class EventUpdateRatings extends StatefulWidget {
  final String groupId;
  final String eventId;

  EventUpdateRatings({Key key, this.groupId, this.eventId}) : super(key: key);

  @override
  _EventUpdateRatingsState createState() => _EventUpdateRatingsState();
}

class _EventUpdateRatingsState extends State<EventUpdateRatings> {
  final List<ChoiceRow> choiceRows = new List<ChoiceRow>();
  final TextEditingController categoryNameController =
      new TextEditingController();
  final int defaultRate = 3;

  Map<String, String> originalRatings;
  bool isCategoryOwner;
  bool loading;
  bool errorLoading;
  bool ratingsChanged;
  Widget errorWidget;
  Category category;

  @override
  void dispose() {
    this.categoryNameController.dispose();
    for (ChoiceRow choiceRow in this.choiceRows) {
      choiceRow.rateController.dispose();
      choiceRow.labelController.dispose();
    }
    super.dispose();
  }

  @override
  void initState() {
    this.originalRatings = new LinkedHashMap<String, String>();
    this.ratingsChanged = false;
    this.loading = true;
    this.errorLoading = false;
    getCategory();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    if (this.loading) {
      return categoryLoading();
    } else if (this.errorLoading) {
      return this.errorWidget;
    } else {
      return WillPopScope(
        onWillPop: handleBackPress,
        child: GestureDetector(
          onTap: () {
            // allows for anywhere on the screen to be clicked to lose focus of a textfield
            hideKeyboard(context);
          },
          child: Scaffold(
            appBar: AppBar(
              title: AutoSizeText(
                "Update Ratings",
                maxLines: 1,
                style: TextStyle(fontSize: 25),
                minFontSize: 12,
                overflow: TextOverflow.ellipsis,
              ),
              actions: <Widget>[
                Visibility(
                  visible: this.ratingsChanged,
                  child: FlatButton(
                    child: Text(
                      "SAVE",
                      style:
                          TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                    ),
                    key: Key("event_update_ratings:save_button"),
                    textColor: Colors.black,
                    onPressed: saveRatings,
                  ),
                ),
              ],
            ),
            key: Key("event_update_ratings:scaffold"),
            body: Center(
              child: Padding(
                padding:
                    EdgeInsets.all(MediaQuery.of(context).size.height * .015),
                child: Column(
                  children: <Widget>[
                    Align(
                      alignment: Alignment.topRight,
                      child: Text(
                        "Version: ${this.category.categoryVersion}",
                        key: Key("event_update_ratings:version_text"),
                      ),
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .004),
                    ),
                    Stack(
                      children: <Widget>[
                        Align(
                          alignment: Alignment.center,
                          child: Container(
                            width: MediaQuery.of(context).size.width * .7,
                            child: TextFormField(
                              enabled: false,
                              controller: this.categoryNameController,
                              style: TextStyle(fontSize: 20),
                              decoration: InputDecoration(
                                  border: OutlineInputBorder(),
                                  labelText: "Category Name",
                                  counterText: ""),
                            ),
                          ),
                        ),
                        Align(
                          alignment: Alignment.centerRight,
                          child: IconButton(
                            icon: Icon(Icons.help_outline),
                            key: Key("event_update_ratings:copy_button"),
                            tooltip: "Help",
                            onPressed: () {
                              showHelpMessage(
                                  "Update Ratings Help",
                                  "This category is a snapshot of a category at a given version number. It "
                                      "is possible that this category has been changed since the event has been created.\n\n"
                                      "Your ratings updates to these choices will only affect the choices in this category version.\n\n"
                                      "Make sure these choices have the ratings that you want to be considered for the given event.",
                                  context);
                            },
                          ),
                        )
                      ],
                    ),
                    Visibility(
                      visible: !this.isCategoryOwner,
                      child: Padding(
                        padding: EdgeInsets.all(
                            MediaQuery.of(context).size.height * .008),
                      ),
                    ),
                    Visibility(
                      visible: !this.isCategoryOwner,
                      child: AutoSizeText(
                        "Category Owner: ${this.category.owner}",
                        style: TextStyle(
                            fontWeight: FontWeight.bold, fontSize: 20),
                        maxLines: 1,
                        minFontSize: 12,
                        overflow: TextOverflow.ellipsis,
                        key: Key("event_update_ratings:category_owner_text"),
                      ),
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .0035),
                    ),
                    Expanded(
                      child: Scrollbar(
                        child: ListView.builder(
                            itemCount: this.choiceRows.length,
                            itemBuilder: (context, index) {
                              return this.choiceRows[index];
                            },
                            key: Key("event_update_ratings:choice_list")),
                      ),
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .035),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      );
    }
  }

  Widget categoryLoading() {
    return Scaffold(
        appBar: AppBar(
            title: AutoSizeText(
          "Update Ratings",
          maxLines: 1,
          style: TextStyle(fontSize: 25),
          minFontSize: 12,
          overflow: TextOverflow.ellipsis,
        )),
        key: Key("event_update_ratings:scaffold_loading"),
        body: Center(child: CircularProgressIndicator()));
  }

  Widget categoryError(String errorMsg) {
    return Scaffold(
        appBar: AppBar(
            title: AutoSizeText(
          "Update Ratings",
          maxLines: 1,
          style: TextStyle(fontSize: 25),
          minFontSize: 12,
          overflow: TextOverflow.ellipsis,
        )),
        key: Key("event_update_ratings:scaffold_error"),
        body: Container(
          height: MediaQuery.of(this.context).size.height * .80,
          child: RefreshIndicator(
            child: ListView(
              children: <Widget>[
                Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(this.context).size.height * .15)),
                Center(child: Text(errorMsg, style: TextStyle(fontSize: 30))),
              ],
            ),
            onRefresh: getCategory,
          ),
        ));
  }

  /*
    If making changes to a category then display a popup to confirm if the user really wishes to
    lose their changes.
    Return true to pop the current page and false to stay.
   */
  Future<bool> handleBackPress() async {
    if (this.ratingsChanged) {
      confirmLeavePage();
      return false;
    } else {
      return true;
    }
  }

  // Display a popup to confirm if the user wishes to leave the page.
  void confirmLeavePage() {
    hideKeyboard(this.context);
    showDialog(
        context: this.context,
        builder: (context) {
          return AlertDialog(
            title: Text("Unsaved changes"),
            actions: <Widget>[
              FlatButton(
                child: Text("YES"),
                key: Key("event_update_ratings:confirm_leave_page_button"),
                onPressed: () {
                  Navigator.of(this.context, rootNavigator: true).pop('dialog');
                  Navigator.of(this.context).pop();
                },
              ),
              FlatButton(
                child: Text("NO"),
                key: Key("event_update_ratings:denial_leave_page_button"),
                onPressed: () {
                  Navigator.of(this.context, rootNavigator: true).pop('dialog');
                },
              )
            ],
            content: Text(
                "You have unsaved changes to this category snapshot. To save them click the \"Save\" button in "
                "the upper right hand corner.\n\nAre you sure you wish to leave this page and lose your changes?"),
          );
        });
  }

  void checkForChanges() {
    // check to see if the ratings have changed
    for (ChoiceRow choiceRow in this.choiceRows) {
      if (this.originalRatings[choiceRow.choiceNumber] !=
          choiceRow.rateController.text) {
        this.ratingsChanged = true;
        break;
      }
    }
    setState(() {});
  }

  // fetches category and ratings from DB for the given category version
  Future<Null> getCategory() async {
    ResultStatus<CategoryRatingTuple> resultStatus =
        await GroupsManager.getEventCategory(widget.groupId, widget.eventId);
    if (resultStatus.success) {
      this.errorLoading = false;
      this.category = resultStatus.data.category;
      // we're not caching the categories on events for now
      this.originalRatings = resultStatus.data.ratings;
      buildChoiceRows();
    } else {
      this.errorLoading = true;
      this.errorWidget = categoryError(resultStatus.errorMessage);
    }
    setState(() {
      this.loading = false;
    });
  }

  // build choice rows and populate them with ratings if they exist for the category
  void buildChoiceRows() {
    this.isCategoryOwner = (this.category.owner == Globals.username);
    this.categoryNameController.text = this.category.categoryName;

    for (String choiceId in this.category.choices.keys) {
      TextEditingController labelController = new TextEditingController();
      labelController.text = this.category.choices[choiceId];
      // we assume the user has no ratings so put all ratings to default value
      TextEditingController rateController = new TextEditingController();
      rateController.text = this.defaultRate.toString();

      ChoiceRow choice = new ChoiceRow(
        choiceId,
        false,
        labelController,
        rateController,
        checkForChange: checkForChanges,
      );
      this.choiceRows.add(choice);
    }
    // populate the choices with the ratings if they exist in the user object
    if (this.originalRatings != null) {
      for (ChoiceRow choiceRow in this.choiceRows) {
        if (originalRatings.containsKey(choiceRow.choiceNumber)) {
          choiceRow.rateController.text =
              originalRatings[choiceRow.choiceNumber];
        }
      }
    }
    // sort by rows choice number
    this.choiceRows.sort((a, b) => a.choiceNumber.compareTo(b.choiceNumber));
    setOriginalValues();
  }

  /*
    Preserve original ratings to determine if save button is to be shown or not.

    This is called when the category is first loaded as well as when a category successfully saves
   */
  void setOriginalValues() {
    this.originalRatings.clear();
    for (ChoiceRow choiceRow in this.choiceRows) {
      this.originalRatings.putIfAbsent(choiceRow.choiceNumber,
          () => choiceRow.rateController.text.toString());
    }
  }

  /*
    Attempts to save the ratings to the DB for the given category version.
   */
  void saveRatings() async {
    hideKeyboard(this.context);

    Map<String, String> ratesToSave = new LinkedHashMap<String, String>();
    for (ChoiceRow choiceRow in this.choiceRows) {
      ratesToSave.putIfAbsent(
          choiceRow.choiceNumber, () => choiceRow.rateController.text.trim());
    }

    showLoadingDialog(this.context, "Saving changes...", true);
    ResultStatus resultStatus = await UsersManager.updateUserChoiceRatings(
        this.category.categoryId, this.category.categoryVersion, ratesToSave);
    Navigator.of(this.context, rootNavigator: true).pop('dialog');

    if (resultStatus.success) {
      setState(() {
        setOriginalValues();
        this.ratingsChanged = false;
      });
    } else {
      showErrorMessage("Error", resultStatus.errorMessage, this.context);
    }
  }
}
