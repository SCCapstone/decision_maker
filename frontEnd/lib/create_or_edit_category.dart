import 'package:flutter/material.dart';
import 'package:numberpicker/numberpicker.dart';

import 'imports/categories_manager.dart';
import 'imports/globals.dart';

class CreateOrEditCategory extends StatefulWidget {
  final bool isEdit;

  CreateOrEditCategory({this.isEdit});

  @override
  _StartScreenState createState() => _StartScreenState(isEdit: this.isEdit);
}

class _StartScreenState extends State<CreateOrEditCategory> {
  final TextEditingController categoryNameController = TextEditingController();
  final bool isEdit;
  final int defaultRate = 3;

  int categoryId;
  int numChoices = 1;
  List<TextEditingController> labels = new List<TextEditingController>();
  List<TextEditingController> rates = new List<TextEditingController>();

  _StartScreenState({@required this.isEdit, this.categoryId}) {
    if (this.isEdit && this.categoryId == null) {
      //error, the categoryId is required for an edit
    }
    TextEditingController t1 = new TextEditingController();
    this.labels.add(t1);

    TextEditingController t2 = new TextEditingController();
    t2.text = "3";
    this.rates.add(t2);
  }

  @override
  void dispose() {
    this.categoryNameController.dispose();
    for (TextEditingController tec in this.labels) {
      tec.dispose();
    }
    for (TextEditingController tec in this.rates) {
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
    for (int i = 0; i < this.numChoices; i++) {
      choices.add(this.getChoiceRatingWidget(
          labels.elementAt(i), rates.elementAt(i), i == this.numChoices - 1));
    }
    choices.add(RaisedButton.icon(
        onPressed: () {
          setState(() {
            TextEditingController t1 = new TextEditingController();
            this.labels.add(t1);

            TextEditingController t2 = new TextEditingController();
            t2.text = "3";
            this.rates.add(t2);

            this.numChoices++;
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
                  List<String> labelsToSave = new List<String>();
                  List<String> ratesToSave = new List<String>();
                  for (int i = 0; i < this.numChoices; i++) {
                    labelsToSave.add(this.labels.elementAt(i).text);
                    ratesToSave.add(this.rates.elementAt(i).text);
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
          )),
    );
  }

  Widget getChoiceRatingWidget(
      TextEditingController l, TextEditingController r, bool isLast) {

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

    if (isLast) {
      children.add(IconButton(
        icon: Icon(Icons.cancel),
        onPressed: () {
          setState(() {
            this.labels.removeLast();
            this.rates.removeLast();
            this.numChoices--;
          });
        },
      ));
    }

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
        }).then((int value) {
          if (value != null) {
            setState(() => r.text = value.toString());
          }
    });
  }
}
