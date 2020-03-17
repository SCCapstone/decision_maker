import 'package:flutter/material.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:numberpicker/numberpicker.dart';

class ChoiceRow extends StatefulWidget {
  final String choiceNumber;
  final bool isOwner;
  final TextEditingController labelController;
  final TextEditingController rateController;
  final Function deleteChoice;
  final VoidCallback checkForChange;
  final FocusNode focusNode;

  ChoiceRow(this.choiceNumber, this.isOwner, this.labelController,
      this.rateController,
      {this.deleteChoice, this.focusNode, this.checkForChange});

  void requestFocus(BuildContext context) {
    FocusScope.of(context).requestFocus(focusNode);
  }

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
          child: TextFormField(
            onChanged: (val) {
              widget.checkForChange();
            },
            validator: validChoiceName,
            maxLength: Globals.maxChoiceNameLength,
            enabled: widget.isOwner,
            textCapitalization: TextCapitalization.sentences,
            focusNode: widget.focusNode,
            controller: widget.labelController,
            decoration: InputDecoration(labelText: "Choice", counterText: ""),
          ),
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
            color: Colors.red,
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
            title: Text("Rate this choice:"),
            initialIntegerValue: int.parse(widget.rateController.text),
          );
        }).then((int value) {
      if (value != null) {
        setState(() => widget.rateController.text = value.toString());
        widget.checkForChange();
      }
    });
  }
}
