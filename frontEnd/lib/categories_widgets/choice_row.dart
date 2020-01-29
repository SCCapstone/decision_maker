import 'package:flutter/material.dart';
import 'package:numberpicker/numberpicker.dart';

class ChoiceRow extends StatefulWidget {
  final int choiceNumber;
  final TextEditingController labelController;
  final TextEditingController rateController;
  final Function deleteChoice;

  ChoiceRow(this.choiceNumber, this.labelController, this.rateController,
      {this.deleteChoice});

  @override
  _ChoiceRowState createState() => new _ChoiceRowState();
}

class _ChoiceRowState extends State<ChoiceRow> {
  final int defaultRate = 3;

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      children: <Widget>[
        Flexible(
          child: TextFormField(
            controller: widget.labelController,
            decoration: InputDecoration(
              labelText: "Choice",
            ),
          ),
        ),
        RaisedButton.icon(
          icon: Icon(Icons.edit),
          label: Text("Rating: " + widget.rateController.text),
          onPressed: () {
            displayRateSelector();
          },
        ),
        IconButton(
          icon: Icon(Icons.cancel),
          onPressed: () {
            widget.deleteChoice(widget);
          },
        )
      ],
    );
  }

  void displayRateSelector() {
    showDialog<int>(
        context: context,
        builder: (BuildContext context) {
          return NumberPickerDialog.integer(
            minValue: 0,
            maxValue: 5,
            title: new Text("Rate this choice:"),
            initialIntegerValue: defaultRate,
          );
        }).then((int value) {
      if (value != null) {
        setState(() => widget.rateController.text = value.toString());
      }
    });
  }
}
