import 'dart:collection';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:front_end_pocket_poll/categories_widgets/choice_row.dart';
import 'package:front_end_pocket_poll/imports/categories_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/imports/users_manager.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/models/category_rating_tuple.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'package:front_end_pocket_poll/utilities/validator.dart';

class CategoryEdit extends StatefulWidget {
  final Category category;
  final CategoryRatingTuple categoryRatingTuple;

  CategoryEdit({Key key, this.category, this.categoryRatingTuple})
      : super(key: key);

  @override
  _CategoryEditState createState() => _CategoryEditState();
}

class _CategoryEditState extends State<CategoryEdit> {
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  final TextEditingController categoryNameController =
      new TextEditingController();
  final List<ChoiceRow> choiceRows = new List<ChoiceRow>();
  final ScrollController scrollController = new ScrollController();
  final int defaultRate = 3;

  // preserve the original labels for copying purposes and detecting if changes were made
  Map<String, int> originalLabels;
  Map<String, String> originalRatings;
  bool autoValidate;
  bool isCategoryOwner;
  bool loading;
  bool errorLoading;
  bool categoryChanged;
  int nextChoiceNum;
  Widget errorWidget;
  Category category;

  @override
  void dispose() {
    this.categoryNameController.dispose();
    for (ChoiceRow choiceRow in this.choiceRows) {
      choiceRow.rateController.dispose();
      choiceRow.labelController.dispose();
    }
    this.scrollController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    this.originalLabels = new LinkedHashMap<String, int>();
    this.originalRatings = new LinkedHashMap<String, String>();
    this.autoValidate = false;
    this.categoryChanged = false;
    this.loading = true;
    this.errorLoading = false;

    if (widget.categoryRatingTuple != null) {
      // means we are editing from the group category page
      this.category = widget.categoryRatingTuple.category;
      this.originalRatings = widget.categoryRatingTuple.ratings;
      this.loading = false;
    } else if (widget.category != null) {
      // means the active user owns the category and is not editing from the group category page
      for (CategoryRatingTuple cat in Globals.cachedCategories) {
        // if category is cached get it and don't bother querying DB
        if (cat.category.categoryId == widget.category.categoryId) {
          this.category = cat.category;
          this.originalRatings = cat.ratings;
          this.loading = false;
          // put the recently accessed category back to top of list of cached categories
          Globals.cachedCategories.removeWhere(
              (cat) => cat.category.categoryId == widget.category.categoryId);
          Globals.cachedCategories.insert(
              0,
              new CategoryRatingTuple(
                  category: this.category, ratings: this.originalRatings));
          if (Globals.cachedCategories.length > Globals.maxCategoryCacheSize) {
            Globals.cachedCategories.removeAt(Globals.maxCategoryCacheSize - 1);
          }
        }
      }
    }

    if (this.loading) {
      // category not cached so fetch from DB
      getCategory();
    } else {
      buildChoiceRows();
    }
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
        child: GestureDetector(
          onTap: () {
            // allows for anywhere on the screen to be clicked to lose focus of a textfield
            hideKeyboard(context);
          },
          child: Scaffold(
              appBar: AppBar(
                title: AutoSizeText(
                  (this.isCategoryOwner) ? "Edit Category" : "Update Ratings",
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
                        style: TextStyle(
                            fontSize: 16, fontWeight: FontWeight.bold),
                      ),
                      key: Key("category_edit:save_button"),
                      textColor: Colors.black,
                      onPressed: saveCategory,
                    ),
                  ),
                ],
              ),
              key: Key("category_edit:scaffold"),
              body: Center(
                child: Form(
                  key: this.formKey,
                  autovalidate: this.autoValidate,
                  child: Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .015),
                    child: Column(
                      children: <Widget>[
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
                                  enabled: this.isCategoryOwner,
                                  onChanged: (val) => checkForChanges(),
                                  maxLength: Globals.maxCategoryNameLength,
                                  controller: this.categoryNameController,
                                  validator: (value) {
                                    return validCategoryName(value.trim(),
                                        categoryId: widget.category.categoryId);
                                  },
                                  key: Key("category_edit:category_name_input"),
                                  textCapitalization:
                                      TextCapitalization.sentences,
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
                                icon: Icon(Icons.content_copy),
                                key: Key("category_edit:copy_button"),
                                tooltip: "Copy Category",
                                onPressed: () {
                                  copyPopup();
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
                            "Category Owner: ${widget.category.owner}",
                            style: TextStyle(
                                fontWeight: FontWeight.bold, fontSize: 20),
                            maxLines: 1,
                            minFontSize: 12,
                            overflow: TextOverflow.ellipsis,
                            key: Key("category_edit:category_owner_text"),
                          ),
                        ),
                        Padding(
                          padding: EdgeInsets.all(
                              MediaQuery.of(context).size.height * .0035),
                        ),
                        Expanded(
                          child: Scrollbar(
                            child: CustomScrollView(
                              controller: this.scrollController,
                              slivers: <Widget>[
                                SliverList(
                                    delegate: SliverChildBuilderDelegate(
                                        (context, index) =>
                                            this.choiceRows[index],
                                        childCount: this.choiceRows.length),
                                    key: Key("category_edit:choice_list"))
                              ],
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
              floatingActionButton: Visibility(
                visible: this.isCategoryOwner,
                child: FloatingActionButton(
                  child: Icon(Icons.add),
                  key: Key("category_edit:add_choice_button"),
                  onPressed: () {
                    if (this.isCategoryOwner) {
                      // only owners of the category can currently add new choices
                      FocusNode focusNode = new FocusNode();
                      TextEditingController labelController =
                          new TextEditingController();
                      TextEditingController rateController =
                          new TextEditingController();
                      rateController.text = this.defaultRate.toString();

                      ChoiceRow choice = new ChoiceRow(this.nextChoiceNum,
                          this.isCategoryOwner, labelController, rateController,
                          deleteChoice: (choice) => deleteChoice(choice),
                          focusNode: focusNode,
                          checkForChange: checkForChanges);
                      setState(() {
                        this.choiceRows.add(choice);
                        this.nextChoiceNum++;
                        checkForChanges();
                      });
                      SchedulerBinding.instance
                          .addPostFrameCallback((_) => scrollToBottom(choice));
                    }
                  },
                ),
              )),
        ),
      );
    }
  }

  // scrolls to the bottom of the listview of all the choices
  void scrollToBottom(ChoiceRow choiceRow) async {
    await this
        .scrollController
        .animateTo(
          this.scrollController.position.maxScrollExtent,
          duration: const Duration(microseconds: 100),
          curve: Curves.easeOut,
        )
        .then((_) {
      // at the bottom of the list now, so request the focus of the choice row
      choiceRow.requestFocus(context);
    });
  }

  Widget categoriesLoading() {
    return Scaffold(
        appBar: AppBar(
            title: AutoSizeText(
          "Edit Category",
          maxLines: 1,
          style: TextStyle(fontSize: 25),
          minFontSize: 12,
          overflow: TextOverflow.ellipsis,
        )),
        key: Key("category_edit:scaffold_loading"),
        body: Center(child: CircularProgressIndicator()));
  }

  Widget categoriesError(String errorMsg) {
    return Scaffold(
        appBar: AppBar(
            title: AutoSizeText(
          "Edit Category",
          maxLines: 1,
          style: TextStyle(fontSize: 25),
          minFontSize: 12,
          overflow: TextOverflow.ellipsis,
        )),
        key: Key("category_edit:scaffold_error"),
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
    if (this.categoryChanged) {
      confirmLeavePage();
      return false;
    } else {
      return true;
    }
  }

  /*
    Display a popup to allow the user to copy a category.

    Note that the category is only copied using the original ratings/labels. Unsaved changes will
    not be carried over to the copied category.
   */
  void copyPopup() {
    final TextEditingController copyNameController =
        new TextEditingController();
    final GlobalKey<FormState> copyForm = GlobalKey<FormState>();
    hideKeyboard(this.context);
    showDialog(
        context: this.context,
        builder: (context) {
          return AlertDialog(
            title: Text("Copy \"${this.category.categoryName}\""),
            actions: <Widget>[
              FlatButton(
                child: Text("CANCEL"),
                key: Key("category_edit:copy_popup_cancel"),
                onPressed: () {
                  Navigator.of(this.context, rootNavigator: true).pop('dialog');
                },
              ),
              FlatButton(
                child: Text("SAVE AS NEW"),
                key: Key("category_edit:copy_popup_save"),
                onPressed: () {
                  final form = copyForm.currentState;
                  if (form.validate()) {
                    copyCategory(copyNameController.text.trim());
                  }
                },
              )
            ],
            content: Form(
              key: copyForm,
              child: Container(
                width: double.maxFinite,
                child: ListView(
                  shrinkWrap: true,
                  children: <Widget>[
                    Text(
                        "To copy this category enter the name that you wish your owned copy to have.\n\n"
                        "Note that any current unsaved changes to the category will not be copied."),
                    TextFormField(
                      controller: copyNameController,
                      textCapitalization: TextCapitalization.sentences,
                      validator: validCategoryName,
                      key: Key("category_edit:copy_popup_category_name_input"),
                      maxLength: Globals.maxCategoryNameLength,
                      decoration: InputDecoration(
                          labelText: "Category Name", counterText: ""),
                    )
                  ],
                ),
              ),
            ),
          );
        });
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
                key: Key("category_edit:confirm_leave_page_button"),
                onPressed: () {
                  Navigator.of(this.context, rootNavigator: true).pop('dialog');
                  Navigator.of(this.context).pop();
                },
              ),
              FlatButton(
                child: Text("NO"),
                key: Key("category_edit:denial_leave_page_button"),
                onPressed: () {
                  Navigator.of(this.context, rootNavigator: true).pop('dialog');
                },
              )
            ],
            content: Text(
                "You have unsaved changes to this category. To save them click the \"Save\" button in "
                "the upper right hand corner.\n\nAre you sure you wish to leave this page and lose your changes?"),
          );
        });
  }

  void checkForChanges() {
    // if lengths differ, then automatically something has changed.
    bool changed = (this.originalLabels.length != this.choiceRows.length);
    bool choiceChanged = false;
    // check to see if the rating or name for a given choice id changed
    for (ChoiceRow choiceRow in this.choiceRows) {
      if (this.originalLabels[choiceRow.labelController.text] !=
              choiceRow.choiceNumber ||
          this.originalRatings[choiceRow.labelController.text] !=
              choiceRow.rateController.text) {
        choiceChanged = true;
      }
    }
    // also check for category name change
    if (this.category.categoryName != this.categoryNameController.text.trim()) {
      changed = true;
    }
    setState(() {
      this.categoryChanged = changed || choiceChanged;
    });
  }

  // fetches category from DB
  Future<void> getCategory() async {
    ResultStatus<List<CategoryRatingTuple>> resultStatus =
        await CategoriesManager.getCategoriesList(
            categoryId: widget.category.categoryId);
    if (resultStatus.success) {
      this.errorLoading = false;
      this.category = resultStatus.data.first.category;
      this.originalRatings = resultStatus.data.first.ratings;
      if (this.category.owner == Globals.username) {
        // cache categories that the user owns
        Globals.cachedCategories.insert(
            0,
            new CategoryRatingTuple(
                category: this.category, ratings: this.originalRatings));
        if (Globals.cachedCategories.length > Globals.maxCategoryCacheSize) {
          Globals.cachedCategories.removeAt(Globals.maxCategoryCacheSize - 1);
        }
      }
      buildChoiceRows();
    } else {
      this.errorLoading = true;
      this.errorWidget = categoriesError(resultStatus.errorMessage);
    }
    setState(() {
      this.loading = false;
    });
  }

  // build choice rows and populate them with ratings if they exist for the category
  void buildChoiceRows() {
    this.isCategoryOwner = (this.category.owner == Globals.username);
    this.categoryNameController.text = this.category.categoryName;

    int i = 0;
    for (String choiceLabel in this.category.choices.keys) {
      TextEditingController labelController = new TextEditingController();
      labelController.text = choiceLabel;
      // we assume the user has no ratings so put all ratings to default value
      TextEditingController rateController = new TextEditingController();
      rateController.text = this.defaultRate.toString();

      //check to see if the above assumption of having no ratings was true
      if (this.originalRatings != null &&
          this.originalRatings.containsKey(choiceLabel)) {
        rateController.text = this.originalRatings[choiceLabel];
      }

      ChoiceRow choice = new ChoiceRow(
        this.category.choices[choiceLabel],
        this.isCategoryOwner,
        labelController,
        rateController,
        deleteChoice: (choice) => deleteChoice(choice),
        checkForChange: checkForChanges,
      );
      this.choiceRows.add(choice);
      i++;
    }
    this.nextChoiceNum = i;

    // sort by rows by choice number
    this.choiceRows.sort((a, b) => a.choiceNumber.compareTo(b.choiceNumber));
    setOriginalValues();
  }

  /*
    Preserve original values to determine if save button is to be shown or not.

    This is called when the category is first loaded as well as when a category successfully saves
   */
  void setOriginalValues() {
    this.originalRatings.clear();
    this.originalLabels.clear();
    for (ChoiceRow choiceRow in this.choiceRows) {
      this.originalLabels.putIfAbsent(choiceRow.labelController.text.toString(),
          () => choiceRow.choiceNumber);
      this.originalRatings.putIfAbsent(
          choiceRow.labelController.text.toString(),
          () => choiceRow.rateController.text.toString());
    }
  }

  /*
    Copies the category by using the current original ratings and names for choices
   */
  void copyCategory(String categoryName) async {
    hideKeyboard(this.context);
    Map<String, int> labelsToSave = new LinkedHashMap<String, int>();
    Map<String, String> ratesToSave = new LinkedHashMap<String, String>();
    for (String label in this.originalLabels.keys) {
      labelsToSave.putIfAbsent(label, () => this.originalLabels[label]);
      ratesToSave.putIfAbsent(label, () => this.originalRatings[label]);
    }
    showLoadingDialog(this.context, "Copying category...", true);
    ResultStatus<Category> resultStatus =
        await CategoriesManager.addOrEditCategory(
            categoryName, labelsToSave, ratesToSave, null);
    Navigator.of(this.context, rootNavigator: true).pop('dialog');
    if (resultStatus.success) {
      // update mapping in user object locally
      Globals.user.ownedCategories.add(new Category(
          categoryId: resultStatus.data.categoryId,
          categoryName: categoryName));
      // cache the new category
      Globals.cachedCategories.add(new CategoryRatingTuple(
          category: resultStatus.data, ratings: ratesToSave));
      if (Globals.cachedCategories.length > Globals.maxCategoryCacheSize) {
        // we only let the user cache so many categories
        Globals.cachedCategories.removeAt(Globals.maxCategoryCacheSize - 1);
      }
      // close the original alert dialog
      Navigator.of(this.context, rootNavigator: true).pop('dialog');
      Fluttertoast.showToast(
          msg: "Category copied successfully!",
          toastLength: Toast.LENGTH_LONG,
          gravity: ToastGravity.CENTER);
    } else {
      showErrorMessage("Error", resultStatus.errorMessage, this.context);
    }
  }

  /*
    Attempts to save the category to the DB. If not the owner, then only the new ratings are saved since only the owner
    can change the name/choices.

    First validates all the user input and if success then it launches an API call. If that call
    returns a success then the page is popped. Else an error message is displayed.
   */
  void saveCategory() async {
    hideKeyboard(this.context);

    Map<String, int> labelsToSave = new LinkedHashMap<String, int>();
    Map<String, String> ratesToSave = new LinkedHashMap<String, String>();
    bool duplicates = false;
    Set names = new Set();
    for (ChoiceRow choiceRow in this.choiceRows) {
      labelsToSave.putIfAbsent(
          choiceRow.labelController.text.trim(), () => choiceRow.choiceNumber);
      ratesToSave.putIfAbsent(choiceRow.labelController.text.trim(),
          () => choiceRow.rateController.text.trim());
      if (!names.add(choiceRow.labelController.text.trim())) {
        duplicates = true;
      }
    }

    final form = this.formKey.currentState;
    if (this.choiceRows.isEmpty) {
      showErrorMessage("Error", "Must have at least one choice!", this.context);
    } else if (!this.isCategoryOwner) {
      // not the owner so only save these new ratings
      showLoadingDialog(this.context, "Saving changes...", true);
      ResultStatus resultStatus = await UsersManager.updateUserChoiceRatings(
          this.category.categoryId, ratesToSave);
      Navigator.of(this.context, rootNavigator: true).pop('dialog');

      if (resultStatus.success) {
        setState(() {
          setOriginalValues();
          this.categoryChanged = false;
        });
      } else {
        showErrorMessage("Error", resultStatus.errorMessage, this.context);
      }
    } else if (form.validate()) {
      // the owner is trying to save the category, so now check choice names/category name
      if (duplicates) {
        setState(() {
          showErrorMessage(
              "Input Error", "No duplicate choices allowed!", this.context);
          this.autoValidate = true;
        });
      } else {
        showLoadingDialog(this.context, "Saving changes...", true);
        ResultStatus<Category> resultStatus =
            await CategoriesManager.addOrEditCategory(
                this.categoryNameController.text.trim(),
                labelsToSave,
                ratesToSave,
                this.category);
        Navigator.of(this.context, rootNavigator: true).pop('dialog');
        if (resultStatus.success) {
          // update category with new one returned from DB
          this.category = resultStatus.data;
          // update mapping in user object locally
          Globals.user.ownedCategories.remove(widget.category);
          Globals.user.ownedCategories.add(new Category(
              categoryId: widget.category.categoryId,
              categoryName: this.category.categoryName));
          // re-cache the category
          Globals.cachedCategories.removeWhere(
              (cat) => cat.category.categoryId == this.category.categoryId);
          Globals.cachedCategories.add(new CategoryRatingTuple(
              category: this.category, ratings: ratesToSave));
          setState(() {
            setOriginalValues();
            this.categoryChanged = false;
          });
        } else {
          showErrorMessage("Error", resultStatus.errorMessage, this.context);
        }
      }
    } else {
      // set autoValidate to true to highlight errors
      setState(() {
        this.autoValidate = true;
      });
    }
  }

  // deletes a given choice row from the main list of choice rows
  void deleteChoice(ChoiceRow choiceRow) {
    setState(() {
      this.choiceRows.remove(choiceRow);
      hideKeyboard(this.context);
      checkForChanges();
    });
  }
}
