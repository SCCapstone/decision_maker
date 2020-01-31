import 'dart:collection';
import 'package:flutter/material.dart';
import 'package:frontEnd/categories_widgets/choice_row.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/globals.dart';
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
  Future<Map<String, dynamic>> ratingsFromDb;

  bool initialPageLoad = true;
  bool doneLoading = false;
  bool autoValidate = false;
  bool isCategoryOwner;
  int nextChoiceNum;

  @override
  void dispose() {
    this.categoryNameController.dispose();
    for (TextEditingController tec in this.labelControllers.values) {
      tec.dispose();
    }
    for (TextEditingController tec in this.ratesControllers.values) {
      tec.dispose();
    }
    super.dispose();
  }

  @override
  void initState() {
    this.isCategoryOwner = (widget.category.owner == Globals.username);
    this.categoryNameController.text = widget.category.categoryName;
    this.nextChoiceNum = widget.category.nextChoiceNum;

    this.ratingsFromDb =
        UsersManager.getUserRatings(widget.category.categoryId, context);

    for (String choiceId in widget.category.choices.keys) {
      TextEditingController labelController = new TextEditingController();
      labelController.text = widget.category.choices[choiceId];
      labelControllers.putIfAbsent(choiceId, () => labelController);

      TextEditingController rateController = new TextEditingController();
      rateController.text = this.defaultRate.toString();
      ratesControllers.putIfAbsent(choiceId, () => rateController);

      ChoiceRow choice = new ChoiceRow(
          int.parse(choiceId),
          widget.category.choices[choiceId.toString()],
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
    return Scaffold(
        appBar: AppBar(title: Text("Edit ${widget.category.categoryName}")),
        body: Center(
          child: Form(
            key: this.formKey,
            autovalidate: this.autoValidate,
            child: Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .015),
              child: Column(
                children: <Widget>[
                  Visibility(
                    visible: this.isCategoryOwner,
                    child: TextFormField(
                      maxLength: 40,
                      controller: this.categoryNameController,
                      validator: validCategory,
                      decoration: InputDecoration(
                          labelText: "Category Name", counterText: ""),
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
                  Expanded(
                    child: Scrollbar(
                      child: FutureBuilder(
                          future: this.ratingsFromDb,
                          builder:
                              (BuildContext context, AsyncSnapshot snapshot) {
                            if (snapshot.hasData) {
                              if (this.initialPageLoad) {
                                Map<String, dynamic> userRatings =
                                    snapshot.data;
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
                              }
                              this.doneLoading = true;
                              return ListView.builder(
                                  shrinkWrap: true,
                                  itemCount: choiceRows.length,
                                  itemBuilder:
                                      (BuildContext context, int index) {
                                    return choiceRows[index];
                                  });
                            } else if (snapshot.hasError) {
                              return Text("Error: ${snapshot.error}");
                            }
                            return Center(child: CircularProgressIndicator());
                          }),
                    ),
                  ),
                  RaisedButton.icon(
                    icon: Icon(Icons.save),
                    label: Text("Save"),
                    onPressed: () {
                      final form = this.formKey.currentState;
                      if (this.choiceRows.length == 0) {
                        showErrorMessage("Error!",
                            "Must have at least one choice!", context);
                      } else if (form.validate()) {
                        Map<String, String> labelsToSave =
                            new LinkedHashMap<String, String>();
                        Map<String, String> ratesToSave =
                            new LinkedHashMap<String, String>();
                        bool duplicates = false;
                        Set names = new Set();
                        for (String i in this.labelControllers.keys) {
                          labelsToSave.putIfAbsent(i.toString(),
                              () => this.labelControllers[i].text);
                          ratesToSave.putIfAbsent(i.toString(),
                              () => this.ratesControllers[i].text);
                          if (!names.add(this.labelControllers[i].text)) {
                            duplicates = true;
                          }
                        }
                        if (duplicates) {
                          setState(() {
                            showErrorMessage("Input Error!",
                                "No duplicate choices allowed!", context);
                            this.autoValidate = true;
                          });
                        } else if (this.isCategoryOwner) {
                          CategoriesManager.addOrEditCategory(
                              this.categoryNameController.text,
                              labelsToSave,
                              ratesToSave,
                              widget.category,
                              context);
                        } else {
                          UsersManager.updateUserChoiceRatings(
                              widget.category.categoryId, ratesToSave, context);
                        }
                      } else {
                        setState(() {
                          this.autoValidate = true;
                        });
                      }
                    },
                  )
                ],
              ),
            ),
          ),
        ),
        floatingActionButton: FloatingActionButton(
          child: Icon(Icons.add),
          onPressed: () {
            if (this.doneLoading && this.isCategoryOwner) {
              // don't let the user add new choices while data is being pulled from DB
              setState(() {
                TextEditingController labelController =
                    new TextEditingController();
                labelControllers.putIfAbsent(
                    this.nextChoiceNum.toString(), () => labelController);
                TextEditingController rateController =
                    new TextEditingController();
                rateController.text = defaultRate.toString();
                ratesControllers.putIfAbsent(
                    this.nextChoiceNum.toString(), () => rateController);

                ChoiceRow choice = new ChoiceRow(this.nextChoiceNum, null,
                    this.isCategoryOwner, labelController, rateController,
                    deleteChoice: (choice) => deleteChoice(choice));
                this.choiceRows.add(choice);
                this.nextChoiceNum++;
              });
            }
          },
        ));
  }

  void deleteChoice(ChoiceRow choiceRow) {
    setState(() {
      this.choiceRows.remove(choiceRow);
      this.labelControllers.remove(choiceRow.choiceNumber);
      this.ratesControllers.remove(choiceRow.choiceNumber);
    });
  }
}
