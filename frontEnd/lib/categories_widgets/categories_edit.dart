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

  EditCategory({Key key, this.category}) : super(key: key);

  @override
  _EditCategoryState createState() => _EditCategoryState();
}

class _EditCategoryState extends State<EditCategory> {
  final formKey = GlobalKey<FormState>();
  final TextEditingController categoryNameController =
      new TextEditingController();
  final int defaultRate = 3;
  final Map<String, TextEditingController> labelControllers =
      new LinkedHashMap<String, TextEditingController>();
  final Map<String, TextEditingController> ratesControllers =
      new LinkedHashMap<String, TextEditingController>();
  final List<ChoiceRow> choiceRows = new List<ChoiceRow>();
  final ScrollController scrollController = new ScrollController();

  Future<ResultStatus<Map<String, dynamic>>> resultFuture;
  FocusNode focusNode;
  bool initialPageLoad = true;
  bool doneLoading = false;
  bool autoValidate = false;
  bool isCategoryOwner;
  int nextChoiceNum;
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
    scrollController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    this.isCategoryOwner = (widget.category.owner == Globals.username);
    this.categoryNameController.text = widget.category.categoryName;
    this.nextChoiceNum = widget.category.nextChoiceNum;

    this.resultFuture = UsersManager.getUserRatings(widget.category.categoryId);

    for (String choiceId in widget.category.choices.keys) {
      TextEditingController labelController = new TextEditingController();
      labelController.text = widget.category.choices[choiceId];
      labelControllers.putIfAbsent(choiceId, () => labelController);

      TextEditingController rateController = new TextEditingController();
      rateController.text = this.defaultRate.toString();
      ratesControllers.putIfAbsent(choiceId, () => rateController);

      ChoiceRow choice = new ChoiceRow(
          choiceId,
          widget.category.choices[choiceId],
          this.isCategoryOwner,
          labelController,
          rateController,
          deleteChoice: (choice) => deleteChoice(choice));
      this.choiceRows.add(choice);
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
                        child: FutureBuilder(
                            future: this.resultFuture,
                            builder:
                                (BuildContext context, AsyncSnapshot snapshot) {
                              bool error = false;
                              String errorMsg;
                              if (snapshot.hasData) {
                                if (this.initialPageLoad) {
                                  ResultStatus<Map<String, dynamic>> result =
                                      snapshot.data;
                                  if (result.success) {
                                    Map<String, dynamic> userRatings =
                                        result.data;
                                    //if the mapping exists in the user's table, override the default
                                    for (String choiceId
                                        in this.labelControllers.keys) {
                                      if (userRatings
                                          .containsKey(choiceId.toString())) {
                                        this.ratesControllers[choiceId].text =
                                            userRatings[choiceId.toString()]
                                                .toString();
                                      }
                                    }
                                    this.initialPageLoad = false;
                                  } else {
                                    error = true;
                                    errorMsg = result.errorMessage;
                                  }
                                }
                                this.doneLoading = true;
                                if (error) {
                                  return Center(
                                      child: Text(errorMsg,
                                          style: TextStyle(fontSize: 30)));
                                }
                                return CustomScrollView(
                                  controller: scrollController,
                                  slivers: <Widget>[
                                    SliverList(
                                      delegate: SliverChildBuilderDelegate(
                                          (context, index) =>
                                              this.choiceRows[index],
                                          childCount: this.choiceRows.length),
                                    )
                                  ],
                                );
                              } else if (snapshot.hasError) {
                                // this should never happen, but keep just in case
                                return Text("Error: ${snapshot.error}");
                              }
                              return Center(child: CircularProgressIndicator());
                            }),
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
                if (this.doneLoading && this.isCategoryOwner) {
                  // don't let the user add new choices while data is being pulled from DB
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
      if (duplicates) {
        setState(() {
          showErrorMessage(
              "Input Error!", "No duplicate choices allowed!", context);
          this.autoValidate = true;
        });
      } else if (this.isCategoryOwner) {
        showLoadingDialog(context, "Saving changes...", true);
        ResultStatus status = await CategoriesManager.addOrEditCategory(
            this.categoryNameController.text,
            labelsToSave,
            ratesToSave,
            widget.category);
        Navigator.of(context, rootNavigator: true).pop('dialog');

        if (!status.success) {
          showErrorMessage("Error", status.errorMessage, context);
        }
      } else {
        showLoadingDialog(context, "Saving changes...", true);
        ResultStatus status = await UsersManager.updateUserChoiceRatings(
            widget.category.categoryId, ratesToSave);
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
