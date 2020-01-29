import 'dart:collection';
import 'package:flutter/material.dart';
import 'package:frontEnd/categories_widgets/choice_row.dart';
import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:frontEnd/models/category.dart';

class CreateCategory extends StatefulWidget {
  final Category category;

  CreateCategory({Key key, this.category}) : super(key: key);

  @override
  _CreateCategoryState createState() => _CreateCategoryState();
}

class _CreateCategoryState extends State<CreateCategory> {
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
    this.isCategoryOwner = true; // you're creating the category so its urs
    this.nextChoiceValue = 2;

    TextEditingController initLabelController = new TextEditingController();
    this.labelControllers.putIfAbsent(1, () => initLabelController);

    TextEditingController initRatingController = new TextEditingController();
    initRatingController.text = this.defaultRate.toString();
    this.ratesControllers.putIfAbsent(1, () => initRatingController);

    ChoiceRow choice = new ChoiceRow(
        0, initLabelController, initRatingController,
        deleteChoice: (choice) => deleteChoice(choice));
    this.choiceRows.add(choice); // provide an initial choice to edit
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(title: Text("New Category")),
        body: Center(
          child: Padding(
            padding: EdgeInsets.all(MediaQuery.of(context).size.height * .015),
            child: Column(
              children: <Widget>[
                TextFormField(
                  maxLength: 40,
                  controller: categoryNameController,
                  decoration: InputDecoration(
                      labelText: "Edit Category Name", counterText: ""),
                ),
                Expanded(
                  child: Scrollbar(
                    child: ListView.builder(
                        shrinkWrap: true,
                        itemCount: choiceRows.length,
                        itemBuilder: (BuildContext context, int index) {
                          return choiceRows[index];
                        }),
                  ),
                ),
                RaisedButton.icon(
                  icon: Icon(Icons.save),
                  label: Text("Save"),
                  onPressed: () {
                    Map<String, String> labelsToSave =
                        new LinkedHashMap<String, String>();
                    Map<String, String> ratesToSave =
                        new LinkedHashMap<String, String>();
                    for (int i in labelControllers.keys) {
                      labelsToSave.putIfAbsent(
                          i.toString(), () => this.labelControllers[i].text);
                      ratesToSave.putIfAbsent(
                          i.toString(), () => this.ratesControllers[i].text);
                    }

                    if (this.isCategoryOwner) {
                      print(ratesToSave);
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
                  },
                )
              ],
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
              rateController.text = "3";
              ratesControllers.putIfAbsent(
                  this.nextChoiceValue, () => rateController);
              ChoiceRow choice = new ChoiceRow(
                  nextChoiceValue, labelController, rateController,
                  deleteChoice: (choice) => deleteChoice(choice));
              this.choiceRows.add(choice);
              nextChoiceValue++;
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
