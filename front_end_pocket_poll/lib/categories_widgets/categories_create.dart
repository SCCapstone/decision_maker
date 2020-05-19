import 'dart:collection';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:front_end_pocket_poll/categories_widgets/choice_row.dart';
import 'package:front_end_pocket_poll/imports/categories_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/models/category_rating_tuple.dart';
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
  final List<ChoiceRow> choiceRows = new List<ChoiceRow>();
  final ScrollController scrollController = new ScrollController();

  int nextChoiceValue;
  bool autoValidate;

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
    this.autoValidate = false;
    // we are creating a category, so thus the first choice value is already set to 1
    TextEditingController initLabelController = new TextEditingController();
    TextEditingController initRatingController = new TextEditingController();
    initRatingController.text = this.defaultRate.toString();

    this.nextChoiceValue = 2;

    ChoiceRow choice = new ChoiceRow(
        "1", true, initLabelController, initRatingController,
        focusNode: new FocusNode(),
        deleteChoice: (choice) => deleteChoice(choice));
    this.choiceRows.add(choice); // provide an initial choice to edit
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
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
                FlatButton(
                  child: Text(
                    "SAVE",
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                  key: Key("categories_create:save_button"),
                  textColor: Colors.black,
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
                          validator: validCategoryName,
                          key: Key("categories_create:category_name_input"),
                          controller: this.categoryNameController,
                          textCapitalization: TextCapitalization.sentences,
                          style: TextStyle(fontSize: 20),
                          onFieldSubmitted: (val) {
                            // on enter, move focus to the first choice row
                            if (this.choiceRows.isNotEmpty) {
                              this.choiceRows[0].requestFocus(context);
                            }
                          },
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
                FocusNode focusNode = new FocusNode();
                TextEditingController labelController =
                    new TextEditingController();
                TextEditingController rateController =
                    new TextEditingController();
                rateController.text = this.defaultRate.toString();

                ChoiceRow choice = new ChoiceRow(
                  this.nextChoiceValue.toString(),
                  true,
                  labelController,
                  rateController,
                  deleteChoice: (choice) => deleteChoice(choice),
                  focusNode: focusNode,
                );
                setState(() {
                  this.choiceRows.add(choice);
                  this.nextChoiceValue++;
                });
                SchedulerBinding.instance
                    .addPostFrameCallback((_) => scrollToBottom(choice));
              },
            )),
      ),
    );
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
                child: Text("YES"),
                key: Key("categories_create:confirm_leave_page_button"),
                onPressed: () {
                  Navigator.of(this.context, rootNavigator: true).pop('dialog');
                  Navigator.of(this.context).pop();
                },
              ),
              FlatButton(
                child: Text("NO"),
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
    hideKeyboard(this.context);
    final form = this.formKey.currentState;
    if (this.choiceRows.isEmpty) {
      showErrorMessage("Error", "Must have at least one choice!", this.context);
    } else if (form.validate()) {
      Map<String, String> labelsToSave = new LinkedHashMap<String, String>();
      Map<String, String> ratesToSave = new LinkedHashMap<String, String>();
      bool duplicates = false;
      // using a set is more efficient than looping over the maps
      Set names = new Set();
      for (ChoiceRow choiceRow in this.choiceRows) {
        labelsToSave.putIfAbsent(choiceRow.choiceNumber,
            () => choiceRow.labelController.text.trim());
        ratesToSave.putIfAbsent(
            choiceRow.choiceNumber, () => choiceRow.rateController.text.trim());
        if (!names.add(choiceRow.labelController.text.trim())) {
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
          // attempt to cache the category
          Globals.cachedCategories.add(new CategoryRatingTuple(
              category: newCategory, ratings: ratesToSave));
          if (Globals.cachedCategories.length > Globals.maxCategoryCacheSize) {
            // we only let the user cache so many categories
            Globals.cachedCategories.removeAt(Globals.maxCategoryCacheSize - 1);
          }
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
      hideKeyboard(this.context);
    });
  }
}
