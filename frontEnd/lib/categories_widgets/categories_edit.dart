import 'dart:collection';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:frontEnd/categories_widgets/choice_row.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';

class EditCategory extends StatefulWidget {
  final Category category;
  final bool editName;

  EditCategory({Key key, this.category, this.editName}) : super(key: key);

  @override
  _EditCategoryState createState() => _EditCategoryState();
}

class _EditCategoryState extends State<EditCategory> {
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  final TextEditingController categoryNameController =
      new TextEditingController();
  final int defaultRate = 3;
  final Map<String, TextEditingController> labelControllers =
      new LinkedHashMap<String, TextEditingController>();
  final Map<String, TextEditingController> ratesControllers =
      new LinkedHashMap<String, TextEditingController>();
  Map<String, String> originalLabels = new LinkedHashMap<String, String>();
  Map<String, String> originalRatings = new LinkedHashMap<String, String>();

  final List<ChoiceRow> choiceRows = new List<ChoiceRow>();
  final ScrollController scrollController = new ScrollController();

  FocusNode focusNode;
  bool autoValidate = false;
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
                      color: Colors.blue,
                      onPressed: saveCategory,
                    ),
                  ),
                ],
              ),
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
                              MediaQuery.of(context).size.height * .008),
                        ),
                        Container(
                          width: MediaQuery.of(context).size.width * .7,
                          child: TextFormField(
                            enabled: widget.editName,
                            onChanged: (val) => checkForChanges(),
                            maxLength: 40,
                            controller: this.categoryNameController,
                            validator: validCategory,
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
                          child: Text(
                            "Category Owner: ${widget.category.owner}",
                            style: TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: DefaultTextStyle.of(context)
                                        .style
                                        .fontSize *
                                    0.4),
                          ),
                        ),
                        Padding(
                          padding: EdgeInsets.all(
                              MediaQuery.of(context).size.height * .008),
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
                                )
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
                  onPressed: () {
                    if (this.isCategoryOwner) {
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
        body: Container(
          height: MediaQuery.of(context).size.height * .80,
          child: RefreshIndicator(
            child: ListView(
              children: <Widget>[
                Padding(
                    padding: EdgeInsets.all(
                        MediaQuery.of(context).size.height * .15)),
                Center(child: Text(errorMsg, style: TextStyle(fontSize: 30))),
              ],
            ),
            onRefresh: getCategory,
          ),
        ));
  }

  Future<bool> handleBackPress() async {
    // if editing, ensure the user really wants to leave to lose their changes
    if (this.categoryChanged) {
      confirmLeavePage();
      return false;
    } else {
      return true;
    }
  }

  void confirmLeavePage() {
    hideKeyboard(context);
    showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: Text("Unsaved changes"),
            actions: <Widget>[
              FlatButton(
                child: Text("Yes"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
                  Navigator.of(context).pop();
                },
              ),
              FlatButton(
                child: Text("No"),
                onPressed: () {
                  Navigator.of(context, rootNavigator: true).pop('dialog');
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

  void getRatings() {
    this.isCategoryOwner = (this.category.owner == Globals.username);
    this.categoryNameController.text = this.category.categoryName;
    this.nextChoiceNum = this.category.nextChoiceNum;

    for (String choiceId in this.category.choices.keys) {
      TextEditingController labelController = new TextEditingController();
      labelController.text = this.category.choices[choiceId];
      this.labelControllers.putIfAbsent(choiceId, () => labelController);

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
    // populate the choices with the ratings
    Map<String, dynamic> userRatings =
        Globals.user.userRatings[widget.category.categoryId];
    if (userRatings != null) {
      for (String choiceId in this.labelControllers.keys) {
        if (userRatings.containsKey(choiceId.toString())) {
          this.ratesControllers[choiceId].text =
              userRatings[choiceId.toString()].toString();
        }
      }
    }
    setOriginalValues();
  }

  void setOriginalValues() {
    // preserve original values to determine if save button is to be shown or not
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

  void saveCategory() async {
    final form = this.formKey.currentState;
    if (this.choiceRows.isEmpty) {
      showErrorMessage("Error", "Must have at least one choice!", context);
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
      hideKeyboard(context);
      if (duplicates) {
        setState(() {
          showErrorMessage(
              "Input Error", "No duplicate choices allowed!", context);
          this.autoValidate = true;
        });
      } else if (this.isCategoryOwner) {
        showLoadingDialog(context, "Saving changes...", true);
        ResultStatus<Category> resultStatus =
            await CategoriesManager.addOrEditCategory(
                this.categoryNameController.text.trim(),
                labelsToSave,
                ratesToSave,
                this.category);
        Navigator.of(context, rootNavigator: true).pop('dialog');
        if (resultStatus.success) {
          // update category with new one returned from DB
          this.category = resultStatus.data;
          // update mapping in user object locally
          Globals.user.ownedCategories.remove(widget.category);
          Globals.user.ownedCategories.add(new Category.debug(
              widget.category.categoryId,
              this.category.categoryName,
              null,
              null,
              null,
              null));
          Globals.activeUserCategories.remove(this.category);
          Globals.activeUserCategories.add(this.category);
          // update local ratings in user object locally
          Globals.user.userRatings.update(
              widget.category.categoryId, (existing) => ratesToSave,
              ifAbsent: () => ratesToSave);

          setState(() {
            setOriginalValues();
            this.categoryChanged = false;
          });
        } else {
          showErrorMessage("Error", resultStatus.errorMessage, context);
        }
      } else {
        showLoadingDialog(context, "Saving changes...", true);
        ResultStatus resultStatus = await UsersManager.updateUserChoiceRatings(
            this.category.categoryId, ratesToSave);
        Navigator.of(context, rootNavigator: true).pop('dialog');

        if (resultStatus.success) {
          // update local ratings
          Globals.user.userRatings.update(
              widget.category.categoryId, (existing) => ratesToSave,
              ifAbsent: () => ratesToSave);
          setState(() {
            setOriginalValues();
            this.categoryChanged = false;
          });
        } else {
          showErrorMessage("Error", resultStatus.errorMessage, context);
        }
      }
    } else {
      setState(() {
        this.autoValidate = true;
      });
    }
  }

  void deleteChoice(ChoiceRow choiceRow) {
    setState(() {
      this.choiceRows.remove(choiceRow);
      this.labelControllers.remove(choiceRow.choiceNumber);
      this.ratesControllers.remove(choiceRow.choiceNumber);
      hideKeyboard(context);
      checkForChanges();
    });
  }
}
