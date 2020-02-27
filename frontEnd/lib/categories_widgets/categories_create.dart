import 'dart:collection';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:frontEnd/categories_widgets/choice_row.dart';
import 'package:frontEnd/imports/categories_manager.dart';
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
  final Map<String, TextEditingController> labelControllers =
      new LinkedHashMap<String, TextEditingController>();
  final Map<String, TextEditingController> ratesControllers =
      new LinkedHashMap<String, TextEditingController>();
  final List<ChoiceRow> choiceRows = new List<ChoiceRow>();

  ScrollController _scrollController = new ScrollController();
  int nextChoiceValue;
  bool autoValidate = false;
  FocusNode focusNode;

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
    focusNode = new FocusNode();

    this.nextChoiceValue = 2;

    TextEditingController initLabelController = new TextEditingController();
    this.labelControllers.putIfAbsent("1", () => initLabelController);

    TextEditingController initRatingController = new TextEditingController();
    initRatingController.text = this.defaultRate.toString();
    this.ratesControllers.putIfAbsent("1", () => initRatingController);

    ChoiceRow choice = new ChoiceRow(
        "1", null, true, initLabelController, initRatingController,
        deleteChoice: (choice) => deleteChoice(choice));
    this.choiceRows.add(choice); // provide an initial choice to edit
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    // allow the list to automatically scroll down as it grows
    SchedulerBinding.instance.addPostFrameCallback((_) {
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeOut,
      );
    });
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
                      child: CustomScrollView(
                        controller: _scrollController,
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
                          labelsToSave.putIfAbsent(
                              i, () => this.labelControllers[i].text);
                          ratesToSave.putIfAbsent(
                              i, () => this.ratesControllers[i].text);
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
                        } else {
                          CategoriesManager.addOrEditCategory(
                              this.categoryNameController.text,
                              labelsToSave,
                              ratesToSave,
                              null,
                              context);
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
              focusNode = new FocusNode();
              TextEditingController labelController =
                  new TextEditingController();
              labelControllers.putIfAbsent(
                  this.nextChoiceValue.toString(), () => labelController);
              TextEditingController rateController =
                  new TextEditingController();
              rateController.text = defaultRate.toString();
              ratesControllers.putIfAbsent(
                  this.nextChoiceValue.toString(), () => rateController);

              ChoiceRow choice = new ChoiceRow(
                this.nextChoiceValue.toString(),
                null,
                true,
                labelController,
                rateController,
                deleteChoice: (choice) => deleteChoice(choice),
                focusNode: focusNode,
              );
              this.choiceRows.add(choice);
              this.nextChoiceValue++;
            });
            FocusScope.of(context).requestFocus(focusNode);
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
