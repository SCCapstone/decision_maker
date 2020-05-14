import 'dart:collection';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/categories_widgets/choice_row.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';

class EventUpdateRatings extends StatefulWidget {
  final String categoryId;
  final int versionNumber;

  EventUpdateRatings({Key key, this.categoryId, this.versionNumber})
      : super(key: key);

  @override
  _EventUpdateRatingsState createState() => _EventUpdateRatingsState();
}

class _EventUpdateRatingsState extends State<EventUpdateRatings> {
  final List<ChoiceRow> choiceRows = new List<ChoiceRow>();
  final int defaultRate = 3;

  // preserve the original labels for copying purposes and detecting if changes were made
  Map<String, String> originalRatings;
  Map<String, String> userRatings;
  bool isCategoryOwner;
  bool loading;
  bool errorLoading;
  bool categoryChanged;
  Widget errorWidget;
  Category category;

  @override
  void dispose() {
    for (ChoiceRow choiceRow in this.choiceRows) {
      choiceRow.rateController.dispose();
      choiceRow.labelController.dispose();
    }
    super.dispose();
  }

  @override
  void initState() {
    this.originalRatings = new LinkedHashMap<String, String>();
    this.userRatings = new LinkedHashMap<String, String>();
    this.categoryChanged = false;
    this.loading = true;
    this.errorLoading = false;
    getCategory();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    if (this.loading) {
      return categoriesLoading();
    } else if (this.errorLoading) {
      return this.errorWidget;
    } else {
      return WillPopScope(
        onWillPop: handleBackPress,
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
                visible: this.categoryChanged,
                child: FlatButton(
                  child: Text(
                    "SAVE",
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                  key: Key("categories_edit:save_button"),
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
                  Container(
                    width: MediaQuery.of(context).size.width * .7,
                    child: TextFormField(
                      enabled: false,
                      key: Key("event_update_ratings:category_name_input"),
                      textCapitalization: TextCapitalization.sentences,
                      style: TextStyle(fontSize: 20),
                      decoration: InputDecoration(
                          border: OutlineInputBorder(),
                          labelText: "Category Name",
                          counterText: ""),
                    ),
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
                      style:
                          TextStyle(fontWeight: FontWeight.bold, fontSize: 20),
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
                        itemBuilder: (context, index) => this.choiceRows[index],
                      ),
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
      );
    }
  }

  /*
    If making changes to a category then display a popup to confirm if the user really wishes to
    lose their changes.
    Return true to pop the current page and false to stay.
   */
  Future<bool> handleBackPress() async {
    if (this.categoryChanged) {
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
            title: Text("Unsaved ratings"),
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
                "You have unsaved ratings for this category snapshot. To save them click the \"Save\" button in "
                "the upper right hand corner.\n\nAre you sure you wish to leave this page and lose your changes?"),
          );
        });
  }

  void checkForChanges() {
    bool ratingsChanged = false;
    // check to see if the rating or name for a given choice id changed
    for (ChoiceRow choiceRow in this.choiceRows) {
      if (this.originalRatings[choiceRow.choiceNumber] !=
          choiceRow.rateController.text) {
        ratingsChanged = true;
      }
    }
    setState(() {
      this.categoryChanged = ratingsChanged;
    });
  }

  Widget categoriesLoading() {
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

  Widget categoriesError(String errorMsg) {
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

  // fetches category from DB
  Future<Null> getCategory() async {
//    ResultStatus<List<Category>> resultStatus =
//        await CategoriesManager.getAllCategoriesList(
//            categoryId: widget.category.categoryId);
//    if (resultStatus.success) {
//      this.errorLoading = false;
//      this.category = resultStatus.data.first;
//      if (this.category.owner == Globals.username) {
//        // cache groups that the user owns
//        Globals.activeUserCategories.insert(0, this.category);
//        if (Globals.activeUserCategories.length >
//            Globals.maxCategoryCacheSize) {
//          Globals.activeUserCategories
//              .removeAt(Globals.maxCategoryCacheSize - 1);
//        }
//      }
//      getRatings();
//    } else {
//      this.errorLoading = true;
//      this.errorWidget = categoriesError(resultStatus.errorMessage);
//    }
//    setState(() {
//      this.loading = false;
//    });
  }

  // load ratings from the local user object
  void getRatings() {
//    this.isCategoryOwner = (this.category.owner == Globals.username);
//    this.categoryNameController.text = this.category.categoryName;
//    this.nextChoiceNum = this.category.nextChoiceNum;
//
//    for (String choiceId in this.category.choices.keys) {
//      TextEditingController labelController = new TextEditingController();
//      labelController.text = this.category.choices[choiceId];
//      // we assume the user has no ratings so put all ratings to default value
//      TextEditingController rateController = new TextEditingController();
//      rateController.text = this.defaultRate.toString();
//
//      ChoiceRow choice = new ChoiceRow(
//        choiceId,
//        false,
//        labelController,
//        rateController,
//      );
//      this.choiceRows.add(choice);
//    }
//    // populate the choices with the ratings if they exist in the user object
//    Map<String, String> categoryRatings =
//        Globals.user.categoryRatings[widget.category.categoryId];
//    if (categoryRatings != null) {
//      for (ChoiceRow choiceRow in this.choiceRows) {
//        if (categoryRatings.containsKey(choiceRow.choiceNumber)) {
//          choiceRow.rateController.text =
//              categoryRatings[choiceRow.choiceNumber];
//        }
//      }
//    }
//    // sort by rows choice number
//    this.choiceRows.sort((a, b) => a.choiceNumber.compareTo(b.choiceNumber));
//    setOriginalValues();
  }

  /*
    Attempts to save the category to the DB. If not the owner, then only the new ratings are saved since only the owner
    can change the name/choices.

    First validates all the user input and if success then it launches an API call. If that call
    returns a success then the page is popped. Else an error message is displayed.
   */
  void saveRatings() async {
    setOriginalValues();
  }

  /*
    Preserve original values to determine if save button is to be shown or not.

    This is called when the category is first loaded as well as when a category successfully saves
   */
  void setOriginalValues() {
    this.originalRatings.clear();
    for (ChoiceRow choiceRow in this.choiceRows) {
      this.originalRatings.putIfAbsent(choiceRow.choiceNumber,
          () => choiceRow.rateController.text.toString());
    }
  }
}
