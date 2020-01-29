import 'package:flutter/material.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:numberpicker/numberpicker.dart';

class ChoiceRow extends StatefulWidget {
  final int choiceNumber;
  final String choiceName;
  final bool isOwner;
  final TextEditingController labelController;
  final TextEditingController rateController;
  final Function deleteChoice;

  ChoiceRow(this.choiceNumber, this.choiceName, this.isOwner, this.labelController,
      this.rateController,
      {this.deleteChoice});

  @override
  _ChoiceRowState createState() => new _ChoiceRowState();
}

class _ChoiceRowState extends State<ChoiceRow> {
  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceAround,
      children: <Widget>[
        Flexible(
          child: Visibility(
            visible: widget.isOwner,
            child: TextFormField(
              validator: validChoice,
              maxLength: 40,
              controller: widget.labelController,
              decoration: InputDecoration(labelText: "Choice", counterText: ""),
            ),
          ),
        ),
        Visibility(
          // if the user is not the category owner, they cannot edit the choice names, only ratings
          visible: !widget.isOwner,
          child: Expanded(child: Text("Choice:\n${widget.choiceName}")),
        ),
        RaisedButton.icon(
          icon: Icon(Icons.edit),
          label: Text("Rating: " + widget.rateController.text),
          onPressed: () {
            displayRateSelector();
          },
        ),
        Visibility(
          // if user is not the category owner, they cannot delete choices
          visible: widget.isOwner,
          child: IconButton(
            icon: Icon(Icons.cancel),
            onPressed: () {
              widget.deleteChoice(widget);
            },
          ),
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
            initialIntegerValue: int.parse(widget.rateController.text),
          );
        }).then((int value) {
      if (value != null) {
        setState(() => widget.rateController.text = value.toString());
      }
    });
  }
}
