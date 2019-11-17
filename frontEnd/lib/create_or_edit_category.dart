import 'dart:collection';

import 'package:flutter/material.dart';
import 'package:numberpicker/numberpicker.dart';

import 'imports/categories_manager.dart';
import 'imports/globals.dart';

class CreateOrEditCategory extends StatefulWidget {
  final bool isEdit;
  final String categoryId;

  CreateOrEditCategory({@required this.isEdit, this.categoryId});

  @override
  _StartScreenState createState() => _StartScreenState(isEdit: this.isEdit, categoryId: this.categoryId);
}

class _StartScreenState extends State<CreateOrEditCategory> {
  final TextEditingController categoryNameController = TextEditingController();
  final bool isEdit;
  final int defaultRate = 3;
  final String categoryId;

  int nextChoiceValue;
  Map<int, TextEditingController> labels = new LinkedHashMap<int, TextEditingController>();
  Map<int, TextEditingController> rates = new LinkedHashMap<int, TextEditingController>();

  _StartScreenState({@required this.isEdit, this.categoryId}) {
    if (this.isEdit && this.categoryId == null) {
      //error, the categoryId is required for an edit
    }

    if (this.isEdit) {

    } else {
      this.nextChoiceValue = 2;

      TextEditingController t1 = new TextEditingController();
      this.labels.putIfAbsent(1, () => t1);

      TextEditingController t2 = new TextEditingController();
      t2.text = this.defaultRate.toString();
      this.rates.putIfAbsent(1, () => t2);
    }
  }

  @override
  void dispose() {
    this.categoryNameController.dispose();
    for (TextEditingController tec in this.labels.values) {
      tec.dispose();
    }
    for (TextEditingController tec in this.rates.values) {
      tec.dispose();
    }

    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    List<Widget> choices = new List<Widget>();
    choices.add(TextFormField(
      controller: this.categoryNameController,
      decoration: InputDecoration(
        labelText: (this.isEdit ? "Edit" : "New") + " Category Name:",
      ),
    ));

    for (int i in this.labels.keys) {
      choices.add(this.getChoiceRatingWidget(labels[i], rates[i], i));
    }

    choices.add(RaisedButton.icon(
        onPressed: () {
          setState(() {
            TextEditingController t1 = new TextEditingController();
            this.labels.putIfAbsent(this.nextChoiceValue, () => t1);

            TextEditingController t2 = new TextEditingController();
            t2.text = "3";
            this.rates.putIfAbsent(this.nextChoiceValue, () => t2);

            this.nextChoiceValue++;
          });
        },
        icon: Icon(Icons.add),
        label: Text("Add Choice")));

    return Scaffold(
      appBar: AppBar(
        title: Text((this.isEdit ? "Edit" : "New") + " Category"),
      ),
      body: Center(
        child: Padding(
          padding: EdgeInsets.all(20.0),
          child: Column(
            children: choices,
          ),
        ),
      ),
      bottomNavigationBar: BottomAppBar(
        color: Colors.green,
        child: Row(
          mainAxisSize: MainAxisSize.max,
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: [
            RaisedButton.icon(
              icon: Icon(Icons.save),
              label: Text((this.isEdit ? "Save" : "Create")),
              onPressed: () {
                Map<String, String> labelsToSave = new LinkedHashMap<String, String>();
                Map<String, String> ratesToSave = new LinkedHashMap<String, String>();
                for (int i in this.labels.keys) {
                  labelsToSave.putIfAbsent(i.toString(), () => this.labels[i].text);
                  ratesToSave.putIfAbsent(i.toString(), () => this.rates[i].text);
                }

                CategoriesManager.addNewCategory(
                  this.categoryNameController.text,
                  labelsToSave,
                  ratesToSave,
                  Globals.username,
                  context
                );
              },
            )
          ],
        )
      ),
    );
  }

  Widget getChoiceRatingWidget(
      TextEditingController l, TextEditingController r, int choiceNo) {

    //this builds the children for an input row of a choice
    List<Widget> children = new List<Widget>();
    children.add(Expanded(
      child: TextFormField(
        controller: l,
        decoration: InputDecoration(
          labelText: "Choice",
        ),
      ),
    ));
    children.add(Expanded(
      child: TextFormField(
        controller: r,
        decoration: InputDecoration(
          labelText: "Rate",
        ),
        onTap: () {
          this.displayRateSelector(r);
        },
      ),
    ));

    children.add(IconButton(
      icon: Icon(Icons.cancel),
      onPressed: () {
        setState(() {
          print(choiceNo);
          this.labels.remove(choiceNo); // somehow get the last key in the set
          this.rates.remove(choiceNo);
        });
      },
    ));

    return Row(
      mainAxisSize: MainAxisSize.max,
      mainAxisAlignment: MainAxisAlignment.spaceAround,
      children: children,
    );
  }

  void displayRateSelector(TextEditingController r) {
    showDialog<int>(
        context: context,
        builder: (BuildContext context) {
          return new NumberPickerDialog.integer(
            minValue: 0,
            maxValue: 5,
            title: new Text("Rate this choice:"),
            initialIntegerValue: defaultRate,
          );
        }
      ).then((int value) {
        if (value != null) {
          setState(() => r.text = value.toString());
        }
      }
    );
  }
}
