import 'dart:collection';

import 'package:flutter/material.dart';
import 'package:frontEnd/imports/users_manager.dart';
import 'package:numberpicker/numberpicker.dart';

import 'imports/categories_manager.dart';
import 'imports/globals.dart';
import 'models/category.dart';

class CreateOrEditCategory extends StatefulWidget {
  final bool isEdit;

  final Category category;

  CreateOrEditCategory({@required this.isEdit, this.category});

  @override
  _StartScreenState createState() =>
      _StartScreenState(isEdit: this.isEdit, category: this.category);
}

class _StartScreenState extends State<CreateOrEditCategory> {
  final TextEditingController categoryNameController = TextEditingController();
  final bool isEdit;
  final Category category;

  final int defaultRate = 3;
  final Map<int, TextEditingController> labels =
      new LinkedHashMap<int, TextEditingController>();
  final Map<int, TextEditingController> rates =
      new LinkedHashMap<int, TextEditingController>();

  Future<Map<String, dynamic>> ratings;
  bool isCategoryOwner;
  int nextChoiceValue;

  _StartScreenState({@required this.isEdit, this.category}) {
    if (this.isEdit && this.category == null) {
      //error, the category is required for an edit
    }

    if (this.isEdit) {
      this.isCategoryOwner = (this.category.owner == Globals.username);
      this.categoryNameController.text = this.category.categoryName;
      this.nextChoiceValue = this.category.nextChoiceNum;

      for (String choiceId in this.category.choices.keys) {
        TextEditingController t1 = new TextEditingController();
        t1.text = this.category.choices[choiceId];
        this.labels.putIfAbsent(int.parse(choiceId), () => t1);

        TextEditingController t2 = new TextEditingController();
        //todo: get user's choice rating for these instead of default (https://github.com/SCCapstone/decision_maker/issues/105)
        t2.text = this.defaultRate.toString();
        this.rates.putIfAbsent(int.parse(choiceId), () => t2);
      }
    } else {
      this.isCategoryOwner = true; // you're creating the category so its urs
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
    this.ratings = UsersManager.getUserRatings(
        (this.isEdit ? this.category.categoryId : null), context);

    return FutureBuilder(
      future: this.ratings,
      builder: (BuildContext context, AsyncSnapshot snapshot) {
        if (snapshot.hasData) {
          Map<String, dynamic> userRatings = snapshot.data;

          //if the mapping exists in the user's table, override the default
          for (int choiceId in this.labels.keys) {
            if (userRatings.containsKey(choiceId.toString())) {
              this.rates[choiceId].text =
                  userRatings[choiceId.toString()].toString();
            }
          }

          return this.getPageBody();
        } else if (snapshot.hasError) {
          return Text("Error: ${snapshot.error}");
        }
        return Center(child: CircularProgressIndicator());
      },
    );
  }

  Widget getPageBody() {
    List<Widget> choices = new List<Widget>();

    if (this.isCategoryOwner) {
      choices.add(TextFormField(
        controller: this.categoryNameController,
        decoration: InputDecoration(
          labelText: (this.isEdit ? "Edit" : "New") + " Category Name:",
        ),
      ));
    } else {
      choices.add(Row(children: [
        Text(
          "Category: " + this.category.categoryName,
          style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: DefaultTextStyle.of(context).style.fontSize * 0.5),
        ),
        Spacer()
      ]));
      choices
          .add(Row(children: [Text("By: " + this.category.owner), Spacer()]));
    }

    for (int i in this.labels.keys) {
      choices.add(this.getChoiceRatingWidget(labels[i], rates[i], i));
    }

    if (this.isCategoryOwner) {
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
    }

    return Scaffold(
      appBar: AppBar(
        title: Text((this.isEdit ? "Edit" : "New") + " Category"),
      ),
      body: Center(
        child: Padding(
          padding: EdgeInsets.all(20.0),
          child: ListView(children: [
            Column(
              children: choices,
            )
          ]),
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
                  Map<String, String> labelsToSave =
                      new LinkedHashMap<String, String>();
                  Map<String, String> ratesToSave =
                      new LinkedHashMap<String, String>();
                  for (int i in this.labels.keys) {
                    labelsToSave.putIfAbsent(
                        i.toString(), () => this.labels[i].text);
                    ratesToSave.putIfAbsent(
                        i.toString(), () => this.rates[i].text);
                  }

                  if (this.isCategoryOwner) {
                    CategoriesManager.addOrEditCategory(
                        this.categoryNameController.text,
                        labelsToSave,
                        ratesToSave,
                        (this.isEdit ? this.category : null),
                        Globals.username,
                        context);
                  } else {
                    UsersManager.updateUserChoiceRatings(
                        this.category.categoryId, ratesToSave, context);
                  }
                },
              )
            ],
          )),
    );
  }

  Widget getChoiceRatingWidget(
      TextEditingController l, TextEditingController r, int choiceNo) {
    //this builds the children for an input row of a choice
    List<Widget> children = new List<Widget>();
    if (this.isCategoryOwner) {
      children.add(Expanded(
        child: TextFormField(
          controller: l,
          decoration: InputDecoration(
            labelText: "Choice",
          ),
        ),
      ));
    } else {
      children.add(
        Expanded(
          child: Text("Choice: " + this.category.choices[choiceNo.toString()]),
        ),
      );
    }

    children.add(Expanded(
      child: RaisedButton.icon(
        icon: Icon(Icons.edit),
        label: Text("Rating: " + r.text),
        onPressed: () {
          this.displayRateSelector(r);
        },
      ),
    ));

    children.add(IconButton(
      icon: Icon(Icons.cancel),
      onPressed: () {
        setState(() {
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
        }).then((int value) {
      if (value != null) {
        setState(() => r.text = value.toString());
      }
    });
  }
}
