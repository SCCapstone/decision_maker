import 'dart:collection';
import 'package:flutter/material.dart';
import 'package:frontEnd/categories_widgets/choice_row.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';

class CreateCategory extends StatefulWidget {
  final Category category;

  CreateCategory({Key key, this.category}) : super(key: key);

  @override
  _CreateCategoryState createState() => _CreateCategoryState();
}

class _CreateCategoryState extends State<CreateCategory> {
  final formKey = GlobalKey<FormState>();
  final TextEditingController categoryNameController =
      new TextEditingController();
  final int defaultRate = 3;
  final Map<int, TextEditingController> labelControllers =
      new LinkedHashMap<int, TextEditingController>();
  final Map<int, TextEditingController> ratesControllers =
      new LinkedHashMap<int, TextEditingController>();
  final List<ChoiceRow> choiceRows = new List<ChoiceRow>();

  bool isCategoryOwner;
  int nextChoiceValue;
  bool autoValidate = false;

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
    this.isCategoryOwner =
        true; // you're creating the category so you are granted the rank of master
    this.nextChoiceValue = 2;

    TextEditingController initLabelController = new TextEditingController();
    this.labelControllers.putIfAbsent(1, () => initLabelController);

    TextEditingController initRatingController = new TextEditingController();
    initRatingController.text = this.defaultRate.toString();
    this.ratesControllers.putIfAbsent(1, () => initRatingController);

    ChoiceRow choice = new ChoiceRow(1, null, this.isCategoryOwner,
        initLabelController, initRatingController,
        deleteChoice: (choice) => deleteChoice(choice));
    this.choiceRows.add(choice); // provide an initial choice to edit
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(title: Text("New Category")),
        body: Center(
          child: Form(
            key: formKey,
            autovalidate: autoValidate,
            child: Padding(
              padding:
                  EdgeInsets.all(MediaQuery.of(context).size.height * .015),
              child: Column(
                children: <Widget>[
                  TextFormField(
                    maxLength: 40,
                    validator: validCategory,
                    controller: this.categoryNameController,
                    decoration: InputDecoration(
                        labelText: "Category Name", counterText: ""),
                  ),
                  Expanded(
                    child: Scrollbar(
                      child: ListView.builder(
                          shrinkWrap: true,
                          itemCount: this.choiceRows.length,
                          itemBuilder: (BuildContext context, int index) {
                            return this.choiceRows[index];
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
                        for (int i in this.labelControllers.keys) {
                          labelsToSave.putIfAbsent(i.toString(),
                              () => this.labelControllers[i].text);
                          ratesToSave.putIfAbsent(i.toString(),
                              () => this.ratesControllers[i].text);
                        }
                        List<String> allNames = new List<String>();
                        for (String id in labelsToSave.keys) {
                          allNames.add(labelsToSave[id]);
                        }
                        List setNames = allNames.toSet().toList();
                        if (setNames.length != allNames.length) {
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
                              null,
                              context);
                        } else {
                          UsersManager.updateUserChoiceRatings(
                              widget.category.categoryId, ratesToSave, context);
                        }
                      } else {
                        // error, don't allow user to save with empty choices/category name
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
            setState(() {
              TextEditingController labelController =
                  new TextEditingController();
              labelControllers.putIfAbsent(
                  this.nextChoiceValue, () => labelController);
              TextEditingController rateController =
                  new TextEditingController();
              rateController.text = defaultRate.toString();
              ratesControllers.putIfAbsent(
                  this.nextChoiceValue, () => rateController);

              ChoiceRow choice = new ChoiceRow(this.nextChoiceValue, null,
                  this.isCategoryOwner, labelController, rateController,
                  deleteChoice: (choice) => deleteChoice(choice));
              this.choiceRows.add(choice);
              this.nextChoiceValue++;
            });
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
