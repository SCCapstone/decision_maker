import 'dart:collection';
import 'package:flutter/material.dart';
import 'package:frontEnd/categories_widgets/choice_row.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/category.dart';

class EditCategory extends StatefulWidget {
  final Category category;

  EditCategory({Key key, this.category}) : super(key: key);

  @override
  _EditCategoryState createState() => _EditCategoryState();
}

class _EditCategoryState extends State<EditCategory> {
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
    this.isCategoryOwner = (widget.category.owner == Globals.username);
    this.categoryNameController.text = widget.category.categoryName;
    this.nextChoiceValue = widget.category.nextChoiceNum;

    for (String choiceId in widget.category.choices.keys) {
      TextEditingController t1 = new TextEditingController();
      t1.text = widget.category.choices[choiceId];
      this.labelControllers.putIfAbsent(int.parse(choiceId), () => t1);

      TextEditingController t2 = new TextEditingController();
      t2.text = this.defaultRate.toString();
      this.ratesControllers.putIfAbsent(int.parse(choiceId), () => t2);
      ChoiceRow choice = new ChoiceRow(int.parse(choiceId), t1, t2,
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
          child: Padding(
            padding: EdgeInsets.all(MediaQuery.of(context).size.height * .015),
            child: Column(
              children: <Widget>[
                TextFormField(
                  maxLength: 40,
                  controller: categoryNameController,
                  decoration: InputDecoration(
                      labelText: "Category Name", counterText: ""),
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
              ],
            ),
          ),
        ),
        floatingActionButton: FloatingActionButton(
          child: Icon(Icons.add),
          onPressed: () {
            setState(() {
              TextEditingController t1 = new TextEditingController();
              labelControllers.putIfAbsent(this.nextChoiceValue, () => t1);
              TextEditingController t2 = new TextEditingController();
              t2.text = "3";
              ratesControllers.putIfAbsent(this.nextChoiceValue, () => t2);
              ChoiceRow choice = new ChoiceRow(nextChoiceValue, t1, t2,
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
    });
  }
}
