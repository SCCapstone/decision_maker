import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/utilities/validator.dart';
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

  // opens up the keyboard to this specific widget
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
            key: Key("choice_row:choice_name_input:${widget.choiceNumber}"),
          ),
        ),
        RaisedButton.icon(
          icon: Icon(Icons.edit),
          label: Text("Rating: " + widget.rateController.text),
          key: Key("choice_row:ratings_button:${widget.choiceNumber}"),
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
            key: Key("choice_row:delete_button:${widget.choiceNumber}"),
            onPressed: () {
              widget.deleteChoice(widget);
            },
          ),
        )
      ],
    );
  }

  // displays a popup for allowing the users to pick a rating for the given choice
  void displayRateSelector() {
    showDialog<int>(
        context: this.context,
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
