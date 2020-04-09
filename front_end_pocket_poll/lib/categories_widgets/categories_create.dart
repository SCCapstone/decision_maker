import 'dart:collection';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:front_end_pocket_poll/categories_widgets/choice_row.dart';
import 'package:front_end_pocket_poll/imports/categories_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'package:front_end_pocket_poll/utilities/validator.dart';

class CreateCategory extends StatefulWidget {
  CreateCategory({Key key}) : super(key: key);

  @override
  _CreateCategoryState createState() => _CreateCategoryState();
}

class _CreateCategoryState extends State<CreateCategory> {
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  final TextEditingController categoryNameController =
      new TextEditingController();
  final int defaultRate = 3;

  // map of choice number to controller that contains the name of the proposed choice
  final Map<String, TextEditingController> labelControllers =
      new LinkedHashMap<String, TextEditingController>();

  // map of choice number to controller that contains the rating of the proposed choice
  final Map<String, TextEditingController> ratesControllers =
      new LinkedHashMap<String, TextEditingController>();
  final List<ChoiceRow> choiceRows = new List<ChoiceRow>();
  final ScrollController scrollController = new ScrollController();

  int nextChoiceValue;
  bool autoValidate;
  FocusNode focusNode;
  ChoiceRow currentChoice;

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
    // we are creating a category, so thus the first choice value is already set to 1
    TextEditingController initLabelController = new TextEditingController();
    this.labelControllers.putIfAbsent("1", () => initLabelController);

    TextEditingController initRatingController = new TextEditingController();
    initRatingController.text = this.defaultRate.toString();
    this.ratesControllers.putIfAbsent("1", () => initRatingController);

    this.nextChoiceValue = 2;

    ChoiceRow choice = new ChoiceRow(
        "1", true, initLabelController, initRatingController,
        deleteChoice: (choice) => deleteChoice(choice));
    this.choiceRows.add(choice); // provide an initial choice to edit
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    if (this.currentChoice != null) {
      // this allows for the keyboard to be displayed when clicking new choice button
      this.currentChoice.requestFocus(context);
      this.currentChoice = null;
    }
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
                "New Category",
                maxLines: 1,
                style: TextStyle(fontSize: 25),
                minFontSize: 12,
                overflow: TextOverflow.ellipsis,
              ),
              actions: <Widget>[
                RaisedButton.icon(
                  icon: Icon(Icons.save),
                  label: Text("Save"),
                  key: Key("categories_create:save_button"),
                  color: Colors.blue,
                  onPressed: saveCategory,
                ),
              ],
            ),
            key: Key("categories_create:scaffold"),
            body: Center(
              child: Form(
                key: this.formKey,
                autovalidate: this.autoValidate,
                child: Padding(
                  padding:
                      EdgeInsets.all(MediaQuery.of(context).size.height * .015),
                  child: Column(
                    children: <Widget>[
                      Padding(
                        padding: EdgeInsets.all(
                            MediaQuery.of(context).size.height * .008),
                      ),
                      Container(
                        width: MediaQuery.of(context).size.width * .7,
                        child: TextFormField(
                          maxLength: Globals.maxCategoryNameLength,
                          validator: (value) {
                            return validCategoryName(value.trim());
                          },
                          key: Key("categories_create:category_name_input"),
                          controller: this.categoryNameController,
                          textCapitalization: TextCapitalization.sentences,
                          style: TextStyle(fontSize: 20),
                          decoration: InputDecoration(
                              border: OutlineInputBorder(),
                              labelText: "Category Name",
                              counterText: ""),
                        ),
                      ),
                      Padding(
                        padding: EdgeInsets.all(
                            MediaQuery.of(context).size.height * .008),
                      ),
                      Expanded(
                        child: Scrollbar(
                          child: CustomScrollView(
                            shrinkWrap: false,
                            controller: this.scrollController,
                            slivers: <Widget>[
                              SliverList(
                                  delegate: SliverChildBuilderDelegate(
                                      (context, index) =>
                                          this.choiceRows[index],
                                      childCount: this.choiceRows.length),
                                  key: Key("categories_create:choice_list"))
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
            floatingActionButton: FloatingActionButton(
              child: Icon(Icons.add),
              key: Key("categories_create:add_choice_button"),
              onPressed: () {
                setState(() {
                  this.focusNode = new FocusNode();
                  TextEditingController labelController =
                      new TextEditingController();
                  this.labelControllers.putIfAbsent(
                      this.nextChoiceValue.toString(), () => labelController);
                  TextEditingController rateController =
                      new TextEditingController();
                  rateController.text = this.defaultRate.toString();
                  this.ratesControllers.putIfAbsent(
                      this.nextChoiceValue.toString(), () => rateController);

                  ChoiceRow choice = new ChoiceRow(
                    this.nextChoiceValue.toString(),
                    true,
                    labelController,
                    rateController,
                    deleteChoice: (choice) => deleteChoice(choice),
                    focusNode: this.focusNode,
                  );
                  this.currentChoice = choice;
                  this.choiceRows.add(choice);
                  this.nextChoiceValue++;
                  // allow the list to automatically scroll down as it grows
                  SchedulerBinding.instance.addPostFrameCallback((_) {
                    this.scrollController.animateTo(
                          this.scrollController.position.maxScrollExtent,
                          duration: const Duration(microseconds: 100),
                          curve: Curves.easeOut,
                        );
                  });
                });
              },
            )),
      ),
    );
  }

  /*
    If creating a category then display a popup to confirm if the user really wishes to
    lose their changes.
    Return true to pop the current page and false to stay.

    Assumption is that if the category name is set or there are choices, then the user is editing.
   */
  Future<bool> handleBackPress() async {
    if (this.choiceRows.isNotEmpty &&
        this.categoryNameController.text.trim().isNotEmpty) {
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
                child: Text("Yes"),
                key: Key("categories_create:confirm_leave_page_button"),
                onPressed: () {
                  Navigator.of(this.context, rootNavigator: true).pop('dialog');
                  Navigator.of(this.context).pop();
                },
              ),
              FlatButton(
                child: Text("No"),
                key: Key("categories_create:denial_leave_page_button"),
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

  /*
    Attempts to save the category to the DB.

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
      // using a set is more efficient than looping over the maps
      Set names = new Set();
      for (String i in this.labelControllers.keys) {
        labelsToSave.putIfAbsent(i, () => this.labelControllers[i].text.trim());
        ratesToSave.putIfAbsent(i, () => this.ratesControllers[i].text.trim());
        if (!names.add(this.labelControllers[i].text.trim())) {
          duplicates = true;
        }
      }
      if (duplicates) {
        setState(() {
          showErrorMessage(
              "Input Error", "No duplicate choices allowed!", this.context);
          this.autoValidate = true;
        });
      } else {
        showLoadingDialog(this.context, "Creating category...", true);
        ResultStatus<Category> resultStatus =
            await CategoriesManager.addOrEditCategory(
                this.categoryNameController.text.trim(),
                labelsToSave,
                ratesToSave,
                null);
        Navigator.of(this.context, rootNavigator: true).pop('dialog');
        if (resultStatus.success) {
          Category newCategory = resultStatus.data;
          // update local user object to reflect the new category
          Globals.user.ownedCategories.add(new Category(
              owner: Globals.username,
              categoryId: newCategory.categoryId,
              categoryName: newCategory.categoryName));
          Globals.activeUserCategories.add(newCategory);
          if (Globals.activeUserCategories.length >
              Globals.maxCategoryCacheSize) {
            // we only let the user cache so many categories
            Globals.activeUserCategories
                .removeAt(Globals.maxCategoryCacheSize - 1);
          }
          // update local ratings
          Globals.user.categoryRatings.update(
              newCategory.categoryId, (existing) => ratesToSave,
              ifAbsent: () => ratesToSave);
          Navigator.of(this.context).pop();
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
    });
  }
}
