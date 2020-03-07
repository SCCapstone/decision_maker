import 'dart:collection';
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
  final List<ChoiceRow> choiceRows = new List<ChoiceRow>();
  final ScrollController scrollController = new ScrollController();

  FocusNode focusNode;
  bool initialPageLoad = true;
  bool autoValidate = false;
  bool isCategoryOwner;
  bool loading;
  bool errorLoading;
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
    scrollController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    loading = true;
    errorLoading = false;
    for (Category cat in Globals.activeUserCategories) {
//      print(cat);
//      print(widget.category.categoryId);
      // if category is cached, get it
      if (cat.categoryId == widget.category.categoryId) {
        print("fuck");
        category = cat;
        loading = false;
      }
    }
    if (loading) {
      getCategory();
    } else {
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
    if (loading) {
      return categoriesLoading();
    } else if (errorLoading) {
      return errorWidget;
    } else {
      return GestureDetector(
        onTap: () {
          // allows for anywhere on the screen to be clicked to lose focus of a textfield
          hideKeyboard(context);
        },
        child: Scaffold(
            appBar: AppBar(
              title: Text("Edit ${widget.category.categoryName}"),
              actions: <Widget>[
                RaisedButton.icon(
                  icon: Icon(Icons.save),
                  label: Text("Save"),
                  color: Colors.blue,
                  onPressed: saveCategory,
                ),
              ],
            ),
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
                        child: Visibility(
                          visible: this.isCategoryOwner,
                          child: TextFormField(
                            enabled: widget.editName,
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
                      ),
                      Visibility(
                        visible: !this.isCategoryOwner,
                        child: Text(
                          "Category: ${widget.category.categoryName}\nBy: ${widget.category.owner}",
                          style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize:
                                  DefaultTextStyle.of(context).style.fontSize *
                                      0.5),
                        ),
                      ),
                      Padding(
                        padding: EdgeInsets.all(
                            MediaQuery.of(context).size.height * .008),
                      ),
                      Expanded(
                        child: Scrollbar(
                          child: CustomScrollView(
                            controller: scrollController,
                            slivers: <Widget>[
                              SliverList(
                                delegate: SliverChildBuilderDelegate(
                                    (context, index) => this.choiceRows[index],
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
              visible: isCategoryOwner,
              child: FloatingActionButton(
                child: Icon(Icons.add),
                onPressed: () {
                  if (this.isCategoryOwner) {
                    setState(() {
                      focusNode = new FocusNode();
                      TextEditingController labelController =
                          new TextEditingController();
                      labelControllers.putIfAbsent(
                          this.nextChoiceNum.toString(), () => labelController);
                      TextEditingController rateController =
                          new TextEditingController();
                      rateController.text = defaultRate.toString();
                      ratesControllers.putIfAbsent(
                          this.nextChoiceNum.toString(), () => rateController);

                      ChoiceRow choice = new ChoiceRow(
                        this.nextChoiceNum.toString(),
                        null,
                        this.isCategoryOwner,
                        labelController,
                        rateController,
                        deleteChoice: (choice) => deleteChoice(choice),
                        focusNode: focusNode,
                      );
                      this.currentChoice = choice;
                      this.choiceRows.add(choice);
                      this.nextChoiceNum++;
                      // allow the list to automatically scroll down as it grows
                      SchedulerBinding.instance.addPostFrameCallback((_) {
                        scrollController.animateTo(
                          scrollController.position.maxScrollExtent,
                          duration: const Duration(microseconds: 100),
                          curve: Curves.easeOut,
                        );
                      });
                    });
                  }
                },
              ),
            )),
      );
    }
  }

  Widget categoriesLoading() {
    return Scaffold(
        appBar: AppBar(
            title: Text(
          "Edit ${widget.category.categoryName}",
        )),
        body: Center(child: CircularProgressIndicator()));
  }

  Widget categoriesError(String errorMsg) {
    return Scaffold(
        appBar: AppBar(
            title: Text(
          "Edit ${widget.category.categoryName}",
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

  Future<Null> getCategory() async {
    ResultStatus<List<Category>> resultStatus =
        await CategoriesManager.getAllCategoriesList(
            categoryId: widget.category.categoryId);
    if (resultStatus.success) {
      errorLoading = false;
      category = resultStatus.data.first;
      if (category.owner == Globals.username) {
        // cache groups that the user owns
        Globals.activeUserCategories.insert(0, category);
      }
      getRatings();
    } else {
      errorWidget = categoriesError(resultStatus.errorMessage);
    }
    setState(() {
      loading = false;
    });
  }

  void getRatings() {
    this.isCategoryOwner = (category.owner == Globals.username);
    this.categoryNameController.text = category.categoryName;
    this.nextChoiceNum = category.nextChoiceNum;

    for (String choiceId in category.choices.keys) {
      TextEditingController labelController = new TextEditingController();
      labelController.text = category.choices[choiceId];
      labelControllers.putIfAbsent(choiceId, () => labelController);

      TextEditingController rateController = new TextEditingController();
      rateController.text = this.defaultRate.toString();
      ratesControllers.putIfAbsent(choiceId, () => rateController);

      ChoiceRow choice = new ChoiceRow(choiceId, category.choices[choiceId],
          this.isCategoryOwner, labelController, rateController,
          deleteChoice: (choice) => deleteChoice(choice));
      this.choiceRows.add(choice);
    }
    // populate the choices with the ratings
    Map<String, dynamic> userRatings =
        Globals.user.categories[widget.category.categoryId];
    if (userRatings != null) {
      for (String choiceId in this.labelControllers.keys) {
        if (userRatings.containsKey(choiceId.toString())) {
          this.ratesControllers[choiceId].text =
              userRatings[choiceId.toString()].toString();
        }
      }
    }
  }

  void saveCategory() async {
    final form = this.formKey.currentState;
    if (this.choiceRows.length == 0) {
      showErrorMessage("Error!", "Must have at least one choice!", context);
    } else if (form.validate()) {
      Map<String, String> labelsToSave = new LinkedHashMap<String, String>();
      Map<String, String> ratesToSave = new LinkedHashMap<String, String>();
      bool duplicates = false;
      Set names = new Set();
      for (String i in this.labelControllers.keys) {
        labelsToSave.putIfAbsent(i, () => this.labelControllers[i].text);
        ratesToSave.putIfAbsent(i, () => this.ratesControllers[i].text);
        if (!names.add(this.labelControllers[i].text)) {
          duplicates = true;
        }
      }
      hideKeyboard(context);
      if (duplicates) {
        setState(() {
          showErrorMessage(
              "Input Error!", "No duplicate choices allowed!", context);
          this.autoValidate = true;
        });
      } else if (this.isCategoryOwner) {
        showLoadingDialog(context, "Saving changes...", true);
        ResultStatus status = await CategoriesManager.addOrEditCategory(
            this.categoryNameController.text.trim(),
            labelsToSave,
            ratesToSave,
            category);
        Navigator.of(context, rootNavigator: true).pop('dialog');
        if(status.success){
          // TODO grab category and replace global list with it
          if (this.categoryNameController.text.trim() !=
              widget.category.categoryName) {
            print("here");
            // new name, so update in the user object locally
            Globals.user.ownedCategories.remove(widget.category);
            Globals.user.ownedCategories.add(new Category.debug(
                widget.category.categoryId,
                widget.category.categoryName,
                null,
                null,
                nextChoiceNum,
                null));
          }
        }
        else {
          showErrorMessage("Error", status.errorMessage, context);
        }
      } else {
        showLoadingDialog(context, "Saving changes...", true);
        ResultStatus status = await UsersManager.updateUserChoiceRatings(
            category.categoryId, ratesToSave);
        Navigator.of(context, rootNavigator: true).pop('dialog');

        if (!status.success) {
          showErrorMessage("Error", status.errorMessage, context);
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
    });
  }
}
