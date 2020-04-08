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
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'package:front_end_pocket_poll/utilities/validator.dart';

class EditCategory extends StatefulWidget {
  final Category category;

  EditCategory({Key key, this.category}) : super(key: key);

  @override
  _EditCategoryState createState() => _EditCategoryState();
}

class _EditCategoryState extends State<EditCategory> {
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  final TextEditingController categoryNameController =
      new TextEditingController();
  final List<ChoiceRow> choiceRows = new List<ChoiceRow>();
  final ScrollController scrollController = new ScrollController();
  final int defaultRate = 3;

  // map of choice number to controller that contains the name of the proposed choice
  final Map<String, TextEditingController> labelControllers =
      new LinkedHashMap<String, TextEditingController>();

  // map of choice number to controller that contains the rating of the proposed choice
  final Map<String, TextEditingController> ratesControllers =
      new LinkedHashMap<String, TextEditingController>();

  // preserve the original labels for copying purposes and detecting if changes were made
  Map<String, String> originalLabels = new LinkedHashMap<String, String>();
  Map<String, String> originalRatings = new LinkedHashMap<String, String>();

  FocusNode focusNode;
  bool autoValidate;
  bool isCategoryOwner;
  bool loading;
  bool errorLoading;
  bool categoryChanged;
  int nextChoiceNum;
  ChoiceRow currentChoice;
  Widget errorWidget;
  Category category;

  @override
  void dispose() {
    this.categoryNameController.dispose();
    for (TextEditingController tec in this.labelControllers.values) {
      tec.dispose();
    }
    for (TextEditingController tec in this.ratesControllers.values) {
      tec.dispose();
    }
    this.scrollController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    this.autoValidate = false;
    this.categoryChanged = false;
    this.loading = true;
    this.errorLoading = false;

    for (Category cat in Globals.activeUserCategories) {
      // if category is cached get it and don't bother querying DB
      if (cat.categoryId == widget.category.categoryId) {
        this.category = cat;
        this.loading = false;
      }
    }
    if (this.loading) {
      // category not cached so fetch from DB
      getCategory();
    } else {
      // put the recently accessed category back to top of list of cached categories
      Globals.activeUserCategories.remove(this.category);
      Globals.activeUserCategories.insert(0, this.category);
      if (Globals.activeUserCategories.length > Globals.maxCategoryCacheSize) {
        Globals.activeUserCategories.removeAt(Globals.maxCategoryCacheSize - 1);
      }
      getRatings();
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    if (this.currentChoice != null) {
      // this allows for the keyboard to be displayed when clicking new choice button
      this.currentChoice.requestFocus(context);
      this.currentChoice = null;
    }
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
                  "Edit Category",
                  maxLines: 1,
                  style: TextStyle(fontSize: 25),
                  minFontSize: 12,
                  overflow: TextOverflow.ellipsis,
                ),
                actions: <Widget>[
                  Visibility(
                    visible: this.categoryChanged,
                    child: RaisedButton.icon(
                      icon: Icon(Icons.save),
                      label: Text("Save"),
                      key: Key("categories_edit:save_button"),
                      color: Colors.blue,
                      onPressed: saveCategory,
                    ),
                  ),
                ],
              ),
              key: Key("categories_edit:scaffold"),
              body: Center(
                child: Form(
                  key: this.formKey,
                  autovalidate: this.autoValidate,
                  child: Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .015),
                    child: Column(
                      children: <Widget>[
                        Align(
                          alignment: Alignment.topRight,
                          child: Text(
                            "Version: ${this.category.categoryVersion}",
                            key: Key("categories_edit:version_text"),
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
                                  enabled: this.isCategoryOwner,
                                  onChanged: (val) => checkForChanges(),
                                  maxLength: Globals.maxCategoryNameLength,
                                  controller: this.categoryNameController,
                                  validator: (value) {
                                    return validCategoryName(value.trim(),
                                        categoryId: widget.category.categoryId);
                                  },
                                  key: Key(
                                      "categories_edit:category_name_input"),
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
                                key: Key("categories_edit:copy_button"),
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
                            key: Key("categories_edit:category_owner_text"),
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
                                    key: Key("categories_edit:choice_list"))
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
                  key: Key("categories_edit:add_choice_button"),
                  onPressed: () {
                    if (this.isCategoryOwner) {
                      // only owners of the category can currently add new choices
                      setState(() {
                        this.focusNode = new FocusNode();
                        TextEditingController labelController =
                            new TextEditingController();
                        this.labelControllers.putIfAbsent(
                            this.nextChoiceNum.toString(),
                            () => labelController);
                        TextEditingController rateController =
                            new TextEditingController();
                        rateController.text = this.defaultRate.toString();
                        this.ratesControllers.putIfAbsent(
                            this.nextChoiceNum.toString(),
                            () => rateController);

                        ChoiceRow choice = new ChoiceRow(
                            this.nextChoiceNum.toString(),
                            this.isCategoryOwner,
                            labelController,
                            rateController,
                            deleteChoice: (choice) => deleteChoice(choice),
                            focusNode: this.focusNode,
                            checkForChange: checkForChanges);
                        this.currentChoice = choice;
                        this.choiceRows.add(choice);
                        this.nextChoiceNum++;
                        // allow the list to automatically scroll down as it grows
                        SchedulerBinding.instance.addPostFrameCallback((_) {
                          this.scrollController.animateTo(
                                this.scrollController.position.maxScrollExtent,
                                duration: const Duration(microseconds: 100),
                                curve: Curves.easeOut,
                              );
                        });
                        checkForChanges();
                      });
                    }
                  },
                ),
              )),
        ),
      );
    }
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
        key: Key("categories_edit:scaffold_loading"),
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
        key: Key("categories_edit:scaffold_error"),
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
            title: Text("Copy \"${widget.category.categoryName}\""),
            actions: <Widget>[
              FlatButton(
                child: Text("Cancel"),
                key: Key("categories_edit:copy_popup_cancel"),
                onPressed: () {
                  Navigator.of(this.context, rootNavigator: true).pop('dialog');
                },
              ),
              FlatButton(
                child: Text("Save as New"),
                key: Key("categories_edit:copy_popup_save"),
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
                      validator: (value) {
                        return validCategoryName(value.trim(),
                            categoryId: widget.category.categoryId);
                      },
                      key:
                          Key("categories_edit:copy_popup_category_name_input"),
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
                child: Text("Yes"),
                key: Key("categories_edit:confirm_leave_page_button"),
                onPressed: () {
                  Navigator.of(this.context, rootNavigator: true).pop('dialog');
                  Navigator.of(this.context).pop();
                },
              ),
              FlatButton(
                child: Text("No"),
                key: Key("categories_edit:denial_leave_page_button"),
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
    bool changed = (this.originalLabels.length != this.labelControllers.length);
    bool labelChanged = false;
    for (String choiceId in this.labelControllers.keys) {
      if (this.originalLabels[choiceId] !=
          this.labelControllers[choiceId].text) {
        labelChanged = labelChanged || true;
      }
    }
    bool ratingsChanged = false;
    for (String choiceId in this.ratesControllers.keys) {
      if (this.originalRatings[choiceId] !=
          this.ratesControllers[choiceId].text) {
        ratingsChanged = ratingsChanged || true;
      }
    }
    // also check for category name change
    if (this.category.categoryName != this.categoryNameController.text.trim()) {
      changed = true;
    }
    setState(() {
      this.categoryChanged = changed || ratingsChanged || labelChanged;
    });
  }

  // fetches category from DB
  Future<Null> getCategory() async {
    ResultStatus<List<Category>> resultStatus =
        await CategoriesManager.getAllCategoriesList(
            categoryId: widget.category.categoryId);
    if (resultStatus.success) {
      this.errorLoading = false;
      this.category = resultStatus.data.first;
      if (this.category.owner == Globals.username) {
        // cache groups that the user owns
        Globals.activeUserCategories.insert(0, this.category);
        if (Globals.activeUserCategories.length >
            Globals.maxCategoryCacheSize) {
          Globals.activeUserCategories
              .removeAt(Globals.maxCategoryCacheSize - 1);
        }
      }
      getRatings();
    } else {
      this.errorLoading = true;
      this.errorWidget = categoriesError(resultStatus.errorMessage);
    }
    setState(() {
      this.loading = false;
    });
  }

  // load ratings from the local user object
  void getRatings() {
    this.isCategoryOwner = (this.category.owner == Globals.username);
    this.categoryNameController.text = this.category.categoryName;
    this.nextChoiceNum = this.category.nextChoiceNum;

    for (String choiceId in this.category.choices.keys) {
      TextEditingController labelController = new TextEditingController();
      labelController.text = this.category.choices[choiceId];
      this.labelControllers.putIfAbsent(choiceId, () => labelController);
      // we assume the user has no ratings so put all ratings to default value
      TextEditingController rateController = new TextEditingController();
      rateController.text = this.defaultRate.toString();
      this.ratesControllers.putIfAbsent(choiceId, () => rateController);

      ChoiceRow choice = new ChoiceRow(
        choiceId,
        this.isCategoryOwner,
        labelController,
        rateController,
        deleteChoice: (choice) => deleteChoice(choice),
        checkForChange: checkForChanges,
      );
      this.choiceRows.add(choice);
    }
    // populate the choices with the ratings if they exist in the user object
    Map<String, String> categoryRatings =
        Globals.user.categoryRatings[widget.category.categoryId];
    if (categoryRatings != null) {
      for (String choiceId in this.labelControllers.keys) {
        if (categoryRatings.containsKey(choiceId)) {
          this.ratesControllers[choiceId].text = categoryRatings[choiceId];
        }
      }
    }
    // sort by rows choice number
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
    for (String choiceId in this.labelControllers.keys) {
      this.originalLabels.putIfAbsent(
          choiceId, () => this.labelControllers[choiceId].text.toString());
    }
    for (String choiceId in this.ratesControllers.keys) {
      this.originalRatings.putIfAbsent(
          choiceId, () => this.ratesControllers[choiceId].text.toString());
    }
  }

  /*
    Copies the category by using the current original ratings and names for choices
   */
  void copyCategory(String categoryName) async {
    Map<String, String> labelsToSave = new LinkedHashMap<String, String>();
    Map<String, String> ratesToSave = new LinkedHashMap<String, String>();
    for (String i in this.originalLabels.keys) {
      labelsToSave.putIfAbsent(i, () => this.originalLabels[i]);
      ratesToSave.putIfAbsent(i, () => this.originalRatings[i]);
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
      Globals.activeUserCategories.add(resultStatus.data);
      // update local ratings in user object locally
      Globals.user.categoryRatings.update(
          widget.category.categoryId, (existing) => ratesToSave,
          ifAbsent: () => ratesToSave);
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
    final form = this.formKey.currentState;
    if (this.choiceRows.isEmpty) {
      showErrorMessage("Error", "Must have at least one choice!", this.context);
    } else if (form.validate()) {
      Map<String, String> labelsToSave = new LinkedHashMap<String, String>();
      Map<String, String> ratesToSave = new LinkedHashMap<String, String>();
      bool duplicates = false;
      Set names = new Set();
      for (String i in this.labelControllers.keys) {
        labelsToSave.putIfAbsent(i, () => this.labelControllers[i].text.trim());
        ratesToSave.putIfAbsent(i, () => this.ratesControllers[i].text.trim());
        if (!names.add(this.labelControllers[i].text.trim())) {
          duplicates = true;
        }
      }
      hideKeyboard(this.context);
      if (duplicates) {
        setState(() {
          showErrorMessage(
              "Input Error", "No duplicate choices allowed!", this.context);
          this.autoValidate = true;
        });
      } else if (this.isCategoryOwner) {
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
          Globals.activeUserCategories.remove(this.category);
          Globals.activeUserCategories.add(this.category);
          // update local ratings in user object locally
          Globals.user.categoryRatings.update(
              widget.category.categoryId, (existing) => ratesToSave,
              ifAbsent: () => ratesToSave);

          setState(() {
            setOriginalValues();
            this.categoryChanged = false;
          });
        } else {
          showErrorMessage("Error", resultStatus.errorMessage, this.context);
        }
      } else {
        // not the owner so only save these new ratings
        showLoadingDialog(this.context, "Saving changes...", true);
        ResultStatus resultStatus = await UsersManager.updateUserChoiceRatings(
            this.category.categoryId, ratesToSave);
        Navigator.of(this.context, rootNavigator: true).pop('dialog');

        if (resultStatus.success) {
          // update local ratings
          Globals.user.categoryRatings.update(
              widget.category.categoryId, (existing) => ratesToSave,
              ifAbsent: () => ratesToSave);
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
      this.labelControllers.remove(choiceRow.choiceNumber);
      this.ratesControllers.remove(choiceRow.choiceNumber);
      hideKeyboard(this.context);
      checkForChanges();
    });
  }
}
